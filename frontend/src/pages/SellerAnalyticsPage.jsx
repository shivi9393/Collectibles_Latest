import React, { useState, useEffect } from 'react';
import apiClient from '../utils/apiClient';
import { formatDistanceToNow } from 'date-fns';

const SellerAnalyticsPage = () => {
    const [items, setItems] = useState([]);
    const [orders, setOrders] = useState([]);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        const fetchData = async () => {
            try {
                const [itemsRes, ordersRes] = await Promise.all([
                    apiClient.get('/items/user/me'),
                    apiClient.get('/orders/my'),
                ]);
                setItems(itemsRes.data || []);
                setOrders(ordersRes.data.sales || []);
            } catch (err) {
                console.error('Failed to fetch analytics data', err);
            } finally {
                setLoading(false);
            }
        };
        fetchData();
    }, []);

    if (loading) {
        return (
            <div className="min-h-screen flex items-center justify-center">
                <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-600"></div>
            </div>
        );
    }

    const totalRevenue = orders
        .filter(o => ['PAID', 'SHIPPED', 'DELIVERED', 'COMPLETED'].includes(o.status))
        .reduce((sum, o) => sum + Number(o.amount || 0), 0);

    const completedOrders = orders.filter(o => o.status === 'COMPLETED' || o.status === 'DELIVERED');
    const activeItems = items.filter(i => i.status === 'ACTIVE');
    const auctionItems = items.filter(i => i.saleType === 'AUCTION' || i.saleType === 'AUCTION_WITH_BUY_NOW');

    const stats = [
        { label: 'Total Revenue', value: `$${totalRevenue.toFixed(2)}`, icon: '💰', color: 'bg-green-50 border-green-200' },
        { label: 'Items Listed', value: items.length, icon: '📦', color: 'bg-blue-50 border-blue-200' },
        { label: 'Active Listings', value: activeItems.length, icon: '🟢', color: 'bg-emerald-50 border-emerald-200' },
        { label: 'Completed Sales', value: completedOrders.length, icon: '✅', color: 'bg-purple-50 border-purple-200' },
        { label: 'Active Auctions', value: auctionItems.filter(i => i.auction?.status === 'ACTIVE').length, icon: '🔨', color: 'bg-yellow-50 border-yellow-200' },
        { label: 'Total Orders', value: orders.length, icon: '📋', color: 'bg-gray-50 border-gray-200' },
    ];

    return (
        <div className="min-h-screen bg-gray-50 py-8">
            <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
                <h1 className="text-3xl font-bold text-gray-900 mb-8">Seller Analytics</h1>

                {/* Stats Grid */}
                <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-6 gap-4 mb-8">
                    {stats.map(stat => (
                        <div key={stat.label} className={`rounded-xl border p-4 ${stat.color}`}>
                            <span className="text-2xl">{stat.icon}</span>
                            <p className="text-2xl font-bold text-gray-900 mt-2">{stat.value}</p>
                            <p className="text-xs text-gray-500 mt-1">{stat.label}</p>
                        </div>
                    ))}
                </div>

                {/* Revenue Bar Chart (simple CSS-based) */}
                {orders.length > 0 && (
                    <div className="bg-white rounded-xl shadow-sm border border-gray-100 p-6 mb-8">
                        <h2 className="text-lg font-bold text-gray-900 mb-4">Revenue by Order</h2>
                        <div className="space-y-2">
                            {orders.slice(0, 10).map(order => {
                                const pct = totalRevenue > 0 ? (Number(order.amount) / totalRevenue * 100) : 0;
                                return (
                                    <div key={order.id} className="flex items-center gap-3">
                                        <span className="text-xs text-gray-500 w-20 truncate">#{order.id}</span>
                                        <div className="flex-1 bg-gray-100 rounded-full h-6 overflow-hidden">
                                            <div className="bg-gradient-to-r from-primary-500 to-primary-700 h-6 rounded-full flex items-center pl-2 transition-all"
                                                style={{ width: `${Math.max(pct, 5)}%` }}>
                                                <span className="text-xs text-white font-medium">${order.amount}</span>
                                            </div>
                                        </div>
                                    </div>
                                );
                            })}
                        </div>
                    </div>
                )}

                {/* Recent Sales Table */}
                <div className="bg-white rounded-xl shadow-sm border border-gray-100 overflow-hidden">
                    <div className="p-6 border-b border-gray-100">
                        <h2 className="text-lg font-bold text-gray-900">Recent Sales</h2>
                    </div>
                    {orders.length === 0 ? (
                        <div className="p-12 text-center">
                            <span className="text-4xl mb-4 block">📊</span>
                            <p className="text-gray-600">No sales yet. Start listing items to see analytics!</p>
                        </div>
                    ) : (
                        <table className="min-w-full divide-y divide-gray-200">
                            <thead className="bg-gray-50">
                                <tr>
                                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Item</th>
                                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Buyer</th>
                                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Amount</th>
                                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Status</th>
                                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Date</th>
                                </tr>
                            </thead>
                            <tbody className="bg-white divide-y divide-gray-200">
                                {orders.map(order => (
                                    <tr key={order.id}>
                                        <td className="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">
                                            {order.item?.title || `Item #${order.item?.id}`}
                                        </td>
                                        <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                                            {order.buyer?.username || '-'}
                                        </td>
                                        <td className="px-6 py-4 whitespace-nowrap text-sm font-bold text-gray-900">
                                            ${order.amount}
                                        </td>
                                        <td className="px-6 py-4 whitespace-nowrap">
                                            <span className={`inline-flex px-2 py-1 text-xs font-semibold rounded-full ${order.status === 'COMPLETED' || order.status === 'DELIVERED' ? 'bg-green-100 text-green-800' :
                                                    order.status === 'PAID' || order.status === 'SHIPPED' ? 'bg-blue-100 text-blue-800' :
                                                        'bg-gray-100 text-gray-800'
                                                }`}>
                                                {order.status?.replace(/_/g, ' ')}
                                            </span>
                                        </td>
                                        <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                                            {order.createdAt ? formatDistanceToNow(new Date(order.createdAt), { addSuffix: true }) : '-'}
                                        </td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    )}
                </div>
            </div>
        </div>
    );
};

export default SellerAnalyticsPage;
