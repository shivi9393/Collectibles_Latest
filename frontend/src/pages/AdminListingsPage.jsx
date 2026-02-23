import React, { useState, useEffect } from 'react';
import apiClient from '../utils/apiClient';

const AdminListingsPage = () => {
    const [items, setItems] = useState([]);
    const [page, setPage] = useState(0);
    const [totalPages, setTotalPages] = useState(0);
    const [loading, setLoading] = useState(true);

    const fetchPending = async (p = 0) => {
        setLoading(true);
        try {
            const res = await apiClient.get(`/admin/items/pending?page=${p}&size=15`);
            setItems(res.data.content || []);
            setTotalPages(res.data.totalPages || 0);
            setPage(p);
        } catch (err) {
            console.error('Failed to fetch pending items', err);
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => { fetchPending(); }, []);

    const handleApprove = async (itemId) => {
        try {
            await apiClient.put(`/admin/items/${itemId}/approve`);
            fetchPending(page);
        } catch (err) {
            alert('Failed to approve: ' + (err.response?.data?.message || err.message));
        }
    };

    const handleReject = async (itemId) => {
        const reason = prompt('Enter reason for rejection:');
        if (!reason) return;
        try {
            await apiClient.put(`/admin/items/${itemId}/reject`, { reason });
            fetchPending(page);
        } catch (err) {
            alert('Failed to reject: ' + (err.response?.data?.message || err.message));
        }
    };

    return (
        <div className="min-h-screen bg-gray-50 py-8">
            <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
                <h1 className="text-3xl font-bold text-gray-900 mb-8">Listing Approval</h1>

                {loading ? (
                    <div className="flex justify-center py-12">
                        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-600"></div>
                    </div>
                ) : items.length === 0 ? (
                    <div className="bg-white rounded-xl shadow-sm border border-gray-100 p-12 text-center">
                        <span className="text-4xl mb-4 block">✅</span>
                        <p className="text-gray-600 text-lg">No items pending approval!</p>
                    </div>
                ) : (
                    <>
                        <div className="grid gap-6">
                            {items.map(item => (
                                <div key={item.id} className="bg-white rounded-xl shadow-sm border border-gray-100 p-6">
                                    <div className="flex justify-between items-start">
                                        <div>
                                            <h3 className="text-lg font-bold text-gray-900">{item.title}</h3>
                                            <p className="text-sm text-gray-500 mt-1">{item.category} • {item.saleType}</p>
                                            <p className="text-sm text-gray-600 mt-2 line-clamp-2">{item.description}</p>
                                            <div className="flex gap-4 mt-3 text-sm text-gray-500">
                                                {item.startingBid && <span>Starting Bid: ${item.startingBid}</span>}
                                                {item.fixedPrice && <span>Fixed Price: ${item.fixedPrice}</span>}
                                                {item.buyNowPrice && <span>Buy Now: ${item.buyNowPrice}</span>}
                                            </div>
                                        </div>
                                        <div className="flex gap-3 flex-shrink-0">
                                            <button onClick={() => handleApprove(item.id)}
                                                className="px-4 py-2 bg-green-600 text-white rounded-lg text-sm font-medium hover:bg-green-700 transition-colors">
                                                Approve
                                            </button>
                                            <button onClick={() => handleReject(item.id)}
                                                className="px-4 py-2 bg-red-600 text-white rounded-lg text-sm font-medium hover:bg-red-700 transition-colors">
                                                Reject
                                            </button>
                                        </div>
                                    </div>
                                </div>
                            ))}
                        </div>

                        <div className="flex justify-between items-center mt-6">
                            <button disabled={page === 0} onClick={() => fetchPending(page - 1)}
                                className="px-4 py-2 bg-white border border-gray-300 rounded-lg text-sm font-medium text-gray-700 hover:bg-gray-50 disabled:opacity-50">
                                Previous
                            </button>
                            <span className="text-sm text-gray-600">Page {page + 1} of {totalPages}</span>
                            <button disabled={page >= totalPages - 1} onClick={() => fetchPending(page + 1)}
                                className="px-4 py-2 bg-white border border-gray-300 rounded-lg text-sm font-medium text-gray-700 hover:bg-gray-50 disabled:opacity-50">
                                Next
                            </button>
                        </div>
                    </>
                )}
            </div>
        </div>
    );
};

export default AdminListingsPage;
