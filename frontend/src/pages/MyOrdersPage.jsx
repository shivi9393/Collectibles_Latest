import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import apiClient from '../utils/apiClient';
import { formatDistanceToNow } from 'date-fns';

const statusColors = {
    PENDING_PAYMENT: 'bg-yellow-100 text-yellow-800',
    PAID: 'bg-blue-100 text-blue-800',
    SHIPPED: 'bg-purple-100 text-purple-800',
    DELIVERED: 'bg-green-100 text-green-800',
    COMPLETED: 'bg-green-100 text-green-800',
    DISPUTED: 'bg-red-100 text-red-800',
    CANCELLED: 'bg-gray-100 text-gray-800',
    LOST: 'bg-red-100 text-red-800',
    REFUNDED: 'bg-orange-100 text-orange-800',
};

const escrowSteps = ['PENDING_PAYMENT', 'PAID', 'SHIPPED', 'DELIVERED', 'COMPLETED'];

const MyOrdersPage = () => {
    const navigate = useNavigate();
    const [tab, setTab] = useState('purchases');
    const [purchases, setPurchases] = useState([]);
    const [sales, setSales] = useState([]);
    const [loading, setLoading] = useState(true);
    const [actionLoading, setActionLoading] = useState(null);

    const fetchOrders = async () => {
        try {
            const res = await apiClient.get('/orders/my');
            setPurchases(res.data.purchases || []);
            setSales(res.data.sales || []);
        } catch (err) {
            console.error('Failed to fetch orders', err);
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => { fetchOrders(); }, []);

    const handlePay = async (orderId, amount) => {
        setActionLoading(orderId);
        try {
            await apiClient.post(`/orders/${orderId}/pay`, {
                amount, paymentMethod: 'CARD', txId: 'txn_' + Date.now()
            });
            fetchOrders();
        } catch (err) {
            alert('Payment failed: ' + (err.response?.data?.error || err.message));
        } finally {
            setActionLoading(null);
        }
    };

    const handleShip = async (orderId) => {
        const trackingNumber = prompt('Enter tracking number:');
        if (!trackingNumber) return;
        const carrier = prompt('Enter carrier (e.g., FedEx, UPS):') || 'Standard';
        setActionLoading(orderId);
        try {
            await apiClient.post(`/orders/${orderId}/ship`, { trackingNumber, carrier });
            fetchOrders();
        } catch (err) {
            alert('Ship failed: ' + (err.response?.data?.error || err.message));
        } finally {
            setActionLoading(null);
        }
    };

    const handleConfirmDelivery = async (orderId) => {
        setActionLoading(orderId);
        try {
            await apiClient.post(`/orders/${orderId}/deliver`);
            fetchOrders();
        } catch (err) {
            alert('Confirm failed: ' + (err.response?.data?.error || err.message));
        } finally {
            setActionLoading(null);
        }
    };

    const handleDispute = async (orderId) => {
        const reason = prompt('Enter dispute reason:');
        if (!reason) return;
        setActionLoading(orderId);
        try {
            await apiClient.post(`/orders/${orderId}/dispute`, { reason });
            fetchOrders();
        } catch (err) {
            alert('Dispute failed: ' + (err.response?.data?.error || err.message));
        } finally {
            setActionLoading(null);
        }
    };

    const getEscrowProgress = (status) => {
        const idx = escrowSteps.indexOf(status);
        return idx >= 0 ? ((idx + 1) / escrowSteps.length) * 100 : 0;
    };

    const OrderCard = ({ order, isBuyer }) => (
        <div className="bg-white rounded-xl shadow-sm border border-gray-100 p-6 mb-4">
            <div className="flex justify-between items-start mb-4">
                <div>
                    <h3 className="text-lg font-bold text-gray-900">{order.item?.title || `Order #${order.id}`}</h3>
                    <p className="text-sm text-gray-500 mt-1">
                        {isBuyer ? `Seller: ${order.seller?.username || '-'}` : `Buyer: ${order.buyer?.username || '-'}`}
                        {' • '}{order.createdAt ? formatDistanceToNow(new Date(order.createdAt), { addSuffix: true }) : ''}
                    </p>
                </div>
                <div className="text-right">
                    <span className={`inline-flex px-2 py-1 text-xs font-semibold rounded-full ${statusColors[order.status] || 'bg-gray-100'}`}>
                        {order.status?.replace(/_/g, ' ')}
                    </span>
                    <p className="text-lg font-bold text-gray-900 mt-1">${order.amount}</p>
                </div>
            </div>

            {/* Escrow Progress Bar */}
            <div className="mb-4">
                <div className="flex justify-between text-xs text-gray-500 mb-1">
                    <span>Payment</span><span>Paid</span><span>Shipped</span><span>Delivered</span><span>Complete</span>
                </div>
                <div className="w-full bg-gray-200 rounded-full h-2">
                    <div className="bg-green-500 h-2 rounded-full transition-all duration-500"
                        style={{ width: `${getEscrowProgress(order.status)}%` }}></div>
                </div>
            </div>

            {/* Shipping Info */}
            {order.shippingInfo && (
                <div className="bg-gray-50 rounded-lg p-3 mb-4 text-sm">
                    <span className="font-medium text-gray-700">📦 Tracking: </span>
                    <span className="text-gray-600">{order.shippingInfo.trackingNumber || '-'}</span>
                    <span className="text-gray-400 ml-2">({order.shippingInfo.carrier || 'Standard'})</span>
                </div>
            )}

            {/* Actions */}
            <div className="flex gap-3 flex-wrap">
                {isBuyer && order.status === 'PENDING_PAYMENT' && (
                    <button onClick={() => handlePay(order.id, order.amount)} disabled={actionLoading === order.id}
                        className="px-4 py-2 bg-blue-600 text-white rounded-lg text-sm font-medium hover:bg-blue-700 disabled:opacity-50">
                        {actionLoading === order.id ? 'Processing...' : '💳 Pay Now'}
                    </button>
                )}
                {!isBuyer && order.status === 'PAID' && (
                    <button onClick={() => handleShip(order.id)} disabled={actionLoading === order.id}
                        className="px-4 py-2 bg-purple-600 text-white rounded-lg text-sm font-medium hover:bg-purple-700 disabled:opacity-50">
                        {actionLoading === order.id ? 'Processing...' : '📦 Ship Order'}
                    </button>
                )}
                {isBuyer && order.status === 'SHIPPED' && (
                    <button onClick={() => handleConfirmDelivery(order.id)} disabled={actionLoading === order.id}
                        className="px-4 py-2 bg-green-600 text-white rounded-lg text-sm font-medium hover:bg-green-700 disabled:opacity-50">
                        {actionLoading === order.id ? 'Processing...' : '✅ Confirm Delivery'}
                    </button>
                )}
                {isBuyer && (order.status === 'SHIPPED' || order.status === 'DELIVERED') && (
                    <button onClick={() => handleDispute(order.id)} disabled={actionLoading === order.id}
                        className="px-4 py-2 bg-red-600 text-white rounded-lg text-sm font-medium hover:bg-red-700 disabled:opacity-50">
                        ⚠️ Dispute
                    </button>
                )}
            </div>
        </div>
    );

    const orders = tab === 'purchases' ? purchases : sales;

    return (
        <div className="min-h-screen bg-gray-50 py-8">
            <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8">
                <h1 className="text-3xl font-bold text-gray-900 mb-6">My Orders</h1>

                {/* Tabs */}
                <div className="flex gap-1 mb-6 bg-gray-200 rounded-lg p-1 w-fit">
                    <button onClick={() => setTab('purchases')}
                        className={`px-6 py-2 rounded-md text-sm font-medium transition-colors ${tab === 'purchases' ? 'bg-white shadow-sm text-gray-900' : 'text-gray-600 hover:text-gray-900'
                            }`}>
                        Purchases ({purchases.length})
                    </button>
                    <button onClick={() => setTab('sales')}
                        className={`px-6 py-2 rounded-md text-sm font-medium transition-colors ${tab === 'sales' ? 'bg-white shadow-sm text-gray-900' : 'text-gray-600 hover:text-gray-900'
                            }`}>
                        Sales ({sales.length})
                    </button>
                </div>

                {loading ? (
                    <div className="flex justify-center py-12">
                        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-600"></div>
                    </div>
                ) : orders.length === 0 ? (
                    <div className="bg-white rounded-xl shadow-sm border border-gray-100 p-12 text-center">
                        <span className="text-4xl mb-4 block">{tab === 'purchases' ? '🛒' : '📦'}</span>
                        <p className="text-gray-600 text-lg">
                            {tab === 'purchases' ? 'No purchases yet.' : 'No sales yet.'}
                        </p>
                        {tab === 'purchases' && (
                            <button onClick={() => navigate('/browse')}
                                className="mt-4 px-6 py-2 bg-primary-600 text-white rounded-lg font-medium hover:bg-primary-700">
                                Browse Items
                            </button>
                        )}
                    </div>
                ) : (
                    orders.map(order => (
                        <OrderCard key={order.id} order={order} isBuyer={tab === 'purchases'} />
                    ))
                )}
            </div>
        </div>
    );
};

export default MyOrdersPage;
