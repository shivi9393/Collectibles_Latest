import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import apiClient from '../utils/apiClient';

const BrowsePage = () => {
    const [items, setItems] = useState([]);
    const [loading, setLoading] = useState(true);
    const [filters, setFilters] = useState({
        category: '',
        saleType: '',
        maxPrice: '',
        verifiedOnly: false,
    });

    useEffect(() => {
        const fetchItems = async () => {
            try {
                const res = await apiClient.get('/items');
                setItems(res.data || []);
            } catch (err) {
                console.error('Failed to fetch items', err);
            } finally {
                setLoading(false);
            }
        };
        fetchItems();
    }, []);

    const filteredItems = items.filter(item => {
        if (filters.category && item.category !== filters.category) return false;
        if (filters.saleType && item.saleType !== filters.saleType) return false;
        if (filters.maxPrice && Number(item.currentPrice || item.fixedPrice || 0) > Number(filters.maxPrice)) return false;
        if (filters.verifiedOnly && !item.isVerified) return false;
        return true;
    });

    const getTimeRemaining = (endTime) => {
        if (!endTime) return null;
        const now = new Date();
        const end = new Date(endTime);
        const diff = end - now;
        if (diff <= 0) return 'Ended';
        const hours = Math.floor(diff / (1000 * 60 * 60));
        const minutes = Math.floor((diff % (1000 * 60 * 60)) / (1000 * 60));
        if (hours > 24) {
            const days = Math.floor(hours / 24);
            return `${days}d ${hours % 24}h`;
        }
        return `${hours}h ${minutes}m`;
    };

    const getPrice = (item) => {
        return item.currentPrice || item.fixedPrice || item.startingBid || 0;
    };

    const getImageUrl = (item) => {
        if (item.images && item.images.length > 0) {
            return `http://localhost:8080${item.images[0].imageUrl}`;
        }
        return null;
    };

    return (
        <div className="min-h-screen bg-gray-50 py-8">
            <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
                {/* Header */}
                <div className="mb-8">
                    <h1 className="text-4xl font-bold text-gray-900 mb-4">Browse Collectibles</h1>
                    <p className="text-gray-600">Discover rare and unique items from collectors worldwide</p>
                </div>

                {/* Filters */}
                <div className="bg-white rounded-xl shadow-sm border border-gray-100 p-6 mb-8">
                    <div className="grid md:grid-cols-4 gap-4">
                        <div>
                            <label className="block text-sm font-medium text-gray-700 mb-2">Category</label>
                            <select className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm"
                                value={filters.category} onChange={(e) => setFilters(f => ({ ...f, category: e.target.value }))}>
                                <option value="">All Categories</option>
                                <option value="Coins">Coins</option>
                                <option value="Figurines">Figurines</option>
                                <option value="Trading Cards">Trading Cards</option>
                                <option value="Antiques">Antiques</option>
                                <option value="Art">Art</option>
                                <option value="Sports">Sports</option>
                                <option value="Other">Other</option>
                            </select>
                        </div>
                        <div>
                            <label className="block text-sm font-medium text-gray-700 mb-2">Sale Type</label>
                            <select className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm"
                                value={filters.saleType} onChange={(e) => setFilters(f => ({ ...f, saleType: e.target.value }))}>
                                <option value="">All Types</option>
                                <option value="AUCTION">Auction</option>
                                <option value="FIXED_PRICE">Fixed Price</option>
                                <option value="AUCTION_WITH_BUY_NOW">Hybrid</option>
                            </select>
                        </div>
                        <div>
                            <label className="block text-sm font-medium text-gray-700 mb-2">Max Price</label>
                            <input type="number" className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm" placeholder="$0"
                                value={filters.maxPrice} onChange={(e) => setFilters(f => ({ ...f, maxPrice: e.target.value }))} />
                        </div>
                        <div className="flex items-end">
                            <label className="flex items-center space-x-2 cursor-pointer">
                                <input type="checkbox" className="w-5 h-5 text-primary-600 rounded focus:ring-primary-500"
                                    checked={filters.verifiedOnly} onChange={(e) => setFilters(f => ({ ...f, verifiedOnly: e.target.checked }))} />
                                <span className="text-sm font-medium text-gray-700">Verified Only</span>
                            </label>
                        </div>
                    </div>
                </div>

                {/* Loading */}
                {loading ? (
                    <div className="flex justify-center py-16">
                        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-600"></div>
                    </div>
                ) : filteredItems.length === 0 ? (
                    <div className="text-center py-16">
                        <span className="text-6xl mb-4 block">📦</span>
                        <h3 className="text-xl font-semibold text-gray-700 mb-2">No items found</h3>
                        <p className="text-gray-500">Try adjusting your filters or check back later</p>
                    </div>
                ) : (
                    <div className="grid md:grid-cols-2 lg:grid-cols-4 gap-6">
                        {filteredItems.map((item) => (
                            <Link key={item.id} to={`/items/${item.id}`} className="bg-white rounded-xl shadow-sm border border-gray-100 group cursor-pointer overflow-hidden hover:shadow-md transition-shadow">
                                <div className="relative overflow-hidden">
                                    {getImageUrl(item) ? (
                                        <img src={getImageUrl(item)} alt={item.title}
                                            className="w-full h-64 object-cover group-hover:scale-110 transition-transform duration-300" />
                                    ) : (
                                        <div className="w-full h-64 bg-gradient-to-br from-gray-200 to-gray-300 flex items-center justify-center group-hover:scale-110 transition-transform duration-300">
                                            <span className="text-5xl">📦</span>
                                        </div>
                                    )}
                                    {item.isVerified && (
                                        <div className="absolute top-3 right-3 bg-green-500 text-white px-3 py-1 rounded-full text-xs font-semibold">
                                            ✓ Verified
                                        </div>
                                    )}
                                    <div className="absolute top-3 left-3">
                                        {item.saleType === 'AUCTION' && <span className="bg-blue-500 text-white px-2 py-1 rounded text-xs font-medium">Auction</span>}
                                        {item.saleType === 'FIXED_PRICE' && <span className="bg-green-500 text-white px-2 py-1 rounded text-xs font-medium">Buy Now</span>}
                                        {item.saleType === 'AUCTION_WITH_BUY_NOW' && <span className="bg-yellow-500 text-white px-2 py-1 rounded text-xs font-medium">Hybrid</span>}
                                    </div>
                                </div>

                                <div className="p-4">
                                    <div className="text-sm text-gray-500 mb-1">{item.category}</div>
                                    <h3 className="font-bold text-lg text-gray-900 mb-2 line-clamp-2">{item.title}</h3>

                                    <div className="flex items-center justify-between mb-3">
                                        <div>
                                            <div className="text-sm text-gray-500">
                                                {item.saleType === 'FIXED_PRICE' ? 'Price' : 'Current Bid'}
                                            </div>
                                            <div className="text-2xl font-bold text-primary-600">
                                                ${Number(getPrice(item)).toLocaleString()}
                                            </div>
                                        </div>
                                    </div>

                                    {item.auction?.endTime && (
                                        <div className="flex items-center text-sm text-gray-600 mb-3">
                                            <span>⏱ {getTimeRemaining(item.auction.endTime)} remaining</span>
                                        </div>
                                    )}

                                    <div className="w-full bg-primary-600 text-white text-center py-2.5 rounded-lg font-semibold hover:bg-primary-700 transition-colors">
                                        {item.saleType === 'FIXED_PRICE' ? 'Buy Now' : 'View & Bid'}
                                    </div>
                                </div>
                            </Link>
                        ))}
                    </div>
                )}
            </div>
        </div>
    );
};

export default BrowsePage;
