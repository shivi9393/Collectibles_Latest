import React from 'react';
import { useAuth } from '../context/AuthContext';

const DashboardPage = () => {
    const { user } = useAuth();
    const [stats, setStats] = React.useState({
        activeBids: 0,
        watchlist: 0,
        activeListings: 0,
        totalSales: 0
    });
    const [recentBids, setRecentBids] = React.useState([]);
    const [recentOrders, setRecentOrders] = React.useState([]);

    React.useEffect(() => {
        // TODO: specific API calls for dashboard stats
        // For now, we set them to empty/zero to avoid "random data"
    }, []);

    return (
        <div className="min-h-screen bg-gray-50 py-8">
            <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
                <div className="mb-8">
                    <h1 className="text-4xl font-bold text-gray-900 mb-2">Dashboard</h1>
                    <p className="text-gray-600">Welcome back, {user?.username}!</p>
                </div>

                {/* Stats Grid */}
                <div className="grid md:grid-cols-4 gap-6 mb-8">
                    <div className="card p-6">
                        <div className="flex items-center justify-between mb-4">
                            <div className="text-sm font-medium text-gray-600">Active Bids</div>
                            <div className="bg-primary-100 p-2 rounded-lg">
                                <svg className="w-6 h-6 text-primary-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M7 7h.01M7 3h5c.512 0 1.024.195 1.414.586l7 7a2 2 0 010 2.828l-7 7a2 2 0 01-2.828 0l-7-7A1.994 1.994 0 013 12V7a4 4 0 014-4z" />
                                </svg>
                            </div>
                        </div>
                        <div className="text-3xl font-bold text-gray-900">{stats.activeBids}</div>
                    </div>

                    <div className="card p-6">
                        <div className="flex items-center justify-between mb-4">
                            <div className="text-sm font-medium text-gray-600">Watchlist</div>
                            <div className="bg-yellow-100 p-2 rounded-lg">
                                <svg className="w-6 h-6 text-yellow-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" />
                                </svg>
                            </div>
                        </div>
                        <div className="text-3xl font-bold text-gray-900">{stats.watchlist}</div>
                    </div>

                    <div className="card p-6">
                        <div className="flex items-center justify-between mb-4">
                            <div className="text-sm font-medium text-gray-600">Active Listings</div>
                            <div className="bg-green-100 p-2 rounded-lg">
                                <svg className="w-6 h-6 text-green-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M20 7l-8-4-8 4m16 0l-8 4m8-4v10l-8 4m0-10L4 7m8 4v10M4 7v10l8 4" />
                                </svg>
                            </div>
                        </div>
                        <div className="text-3xl font-bold text-gray-900">{stats.activeListings}</div>
                    </div>

                    <div className="card p-6">
                        <div className="flex items-center justify-between mb-4">
                            <div className="text-sm font-medium text-gray-600">Total Sales</div>
                            <div className="bg-purple-100 p-2 rounded-lg">
                                <svg className="w-6 h-6 text-purple-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8c-1.657 0-3 .895-3 2s1.343 2 3 2 3 .895 3 2-1.343 2-3 2m0-8c1.11 0 2.08.402 2.599 1M12 8V7m0 1v8m0 0v1m0-1c-1.11 0-2.08-.402-2.599-1M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                                </svg>
                            </div>
                        </div>
                        <div className="text-3xl font-bold text-gray-900">${stats.totalSales}</div>
                    </div>
                </div>

                {/* Recent Activity */}
                <div className="grid md:grid-cols-2 gap-8">
                    <div className="card p-6">
                        <h2 className="text-xl font-bold text-gray-900 mb-4">Recent Bids</h2>
                        {recentBids.length === 0 ? (
                            <p className="text-gray-500">No recent bids.</p>
                        ) : (
                            <div className="space-y-4">
                                {/* Map recentBids here */}
                            </div>
                        )}
                    </div>

                    <div className="card p-6">
                        <h2 className="text-xl font-bold text-gray-900 mb-4">Recent Orders</h2>
                        {recentOrders.length === 0 ? (
                            <p className="text-gray-500">No recent orders.</p>
                        ) : (
                            <div className="space-y-4">
                                {/* Map recentOrders here */}
                            </div>
                        )}
                    </div>
                </div>
            </div>
        </div>
    );
};

export default DashboardPage;
