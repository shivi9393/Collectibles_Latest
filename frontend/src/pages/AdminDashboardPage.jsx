import React, { useState, useEffect } from 'react';
import apiClient from '../utils/apiClient';

const AdminDashboardPage = () => {
    const [stats, setStats] = useState(null);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        const fetchStats = async () => {
            try {
                const res = await apiClient.get('/admin/dashboard');
                setStats(res.data);
            } catch (err) {
                console.error('Failed to fetch admin stats', err);
            } finally {
                setLoading(false);
            }
        };
        fetchStats();
    }, []);

    if (loading) {
        return (
            <div className="min-h-screen flex items-center justify-center">
                <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-600"></div>
            </div>
        );
    }

    const cards = [
        { label: 'Total Users', value: stats?.totalUsers || 0, color: 'bg-blue-500', icon: '👥' },
        { label: 'Active Auctions', value: stats?.activeAuctions || 0, color: 'bg-green-500', icon: '🔨' },
        { label: 'Pending Items', value: stats?.pendingItems || 0, color: 'bg-yellow-500', icon: '⏳' },
        { label: 'Open Fraud Reports', value: stats?.openFraudReports || 0, color: 'bg-red-500', icon: '🚨' },
        { label: 'Frozen Users', value: stats?.frozenUsers || 0, color: 'bg-purple-500', icon: '🧊' },
    ];

    return (
        <div className="min-h-screen bg-gray-50 py-8">
            <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
                <h1 className="text-3xl font-bold text-gray-900 mb-8">Admin Dashboard</h1>

                <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-5 gap-6 mb-8">
                    {cards.map((card) => (
                        <div key={card.label} className="bg-white rounded-xl shadow-sm border border-gray-100 p-6">
                            <div className="flex items-center justify-between mb-3">
                                <span className="text-sm font-medium text-gray-500">{card.label}</span>
                                <span className="text-2xl">{card.icon}</span>
                            </div>
                            <div className="text-3xl font-bold text-gray-900">{card.value}</div>
                        </div>
                    ))}
                </div>

                <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
                    <div className="bg-white rounded-xl shadow-sm border border-gray-100 p-6">
                        <h2 className="text-lg font-bold text-gray-900 mb-4">Quick Actions</h2>
                        <div className="space-y-3">
                            <a href="/admin/users" className="block p-3 rounded-lg bg-gray-50 hover:bg-gray-100 transition-colors">
                                <span className="font-medium text-gray-700">👥 Manage Users</span>
                                <p className="text-sm text-gray-500 mt-1">Ban/unban users, view user details</p>
                            </a>
                            <a href="/admin/listings" className="block p-3 rounded-lg bg-gray-50 hover:bg-gray-100 transition-colors">
                                <span className="font-medium text-gray-700">📦 Review Listings</span>
                                <p className="text-sm text-gray-500 mt-1">Approve or reject pending item listings</p>
                            </a>
                            <a href="/admin/fraud" className="block p-3 rounded-lg bg-gray-50 hover:bg-gray-100 transition-colors">
                                <span className="font-medium text-gray-700">🚨 Fraud Reports</span>
                                <p className="text-sm text-gray-500 mt-1">Investigate and resolve fraud reports</p>
                            </a>
                            <a href="/admin/audit-logs" className="block p-3 rounded-lg bg-gray-50 hover:bg-gray-100 transition-colors">
                                <span className="font-medium text-gray-700">📋 Audit Logs</span>
                                <p className="text-sm text-gray-500 mt-1">View immutable admin action history</p>
                            </a>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    );
};

export default AdminDashboardPage;
