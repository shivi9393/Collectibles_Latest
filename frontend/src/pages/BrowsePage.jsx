import React from 'react';

const BrowsePage = () => {
    // Mock data for demonstration
    const mockItems = [
        {
            id: 1,
            title: '1909-S VDB Lincoln Penny',
            category: 'Coins',
            currentPrice: 1250.00,
            saleType: 'AUCTION',
            imageUrl: 'https://via.placeholder.com/300x300?text=Rare+Coin',
            endTime: '2024-02-15T18:00:00',
            isVerified: true
        },
        {
            id: 2,
            title: 'Vintage Star Wars Action Figure',
            category: 'Figurines',
            currentPrice: 850.00,
            saleType: 'FIXED_PRICE',
            imageUrl: 'https://via.placeholder.com/300x300?text=Action+Figure',
            isVerified: true
        },
        {
            id: 3,
            title: 'Charizard First Edition Pokemon Card',
            category: 'Trading Cards',
            currentPrice: 3500.00,
            saleType: 'AUCTION',
            imageUrl: 'https://via.placeholder.com/300x300?text=Pokemon+Card',
            endTime: '2024-02-14T20:00:00',
            isVerified: false
        },
        {
            id: 4,
            title: 'Antique Victorian Pocket Watch',
            category: 'Antiques',
            currentPrice: 2100.00,
            saleType: 'HYBRID',
            imageUrl: 'https://via.placeholder.com/300x300?text=Pocket+Watch',
            endTime: '2024-02-16T15:00:00',
            isVerified: true
        }
    ];

    const getTimeRemaining = (endTime) => {
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

    return (
        <div className="min-h-screen bg-gray-50 py-8">
            <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
                {/* Header */}
                <div className="mb-8">
                    <h1 className="text-4xl font-bold text-gray-900 mb-4">Browse Collectibles</h1>
                    <p className="text-gray-600">Discover rare and unique items from collectors worldwide</p>
                </div>

                {/* Filters */}
                <div className="card p-6 mb-8">
                    <div className="grid md:grid-cols-4 gap-4">
                        <div>
                            <label className="block text-sm font-medium text-gray-700 mb-2">Category</label>
                            <select className="input-field">
                                <option value="">All Categories</option>
                                <option value="coins">Coins</option>
                                <option value="figurines">Figurines</option>
                                <option value="cards">Trading Cards</option>
                                <option value="antiques">Antiques</option>
                            </select>
                        </div>

                        <div>
                            <label className="block text-sm font-medium text-gray-700 mb-2">Sale Type</label>
                            <select className="input-field">
                                <option value="">All Types</option>
                                <option value="auction">Auction</option>
                                <option value="fixed">Fixed Price</option>
                                <option value="hybrid">Hybrid</option>
                            </select>
                        </div>

                        <div>
                            <label className="block text-sm font-medium text-gray-700 mb-2">Max Price</label>
                            <input type="number" className="input-field" placeholder="$0" />
                        </div>

                        <div className="flex items-end">
                            <label className="flex items-center space-x-2 cursor-pointer">
                                <input type="checkbox" className="w-5 h-5 text-primary-600 rounded focus:ring-primary-500" />
                                <span className="text-sm font-medium text-gray-700">Verified Only</span>
                            </label>
                        </div>
                    </div>
                </div>

                {/* Items Grid */}
                <div className="grid md:grid-cols-2 lg:grid-cols-4 gap-6">
                    {mockItems.map((item) => (
                        <div key={item.id} className="card group cursor-pointer">
                            <div className="relative overflow-hidden">
                                <img
                                    src={item.imageUrl}
                                    alt={item.title}
                                    className="w-full h-64 object-cover group-hover:scale-110 transition-transform duration-300"
                                />
                                {item.isVerified && (
                                    <div className="absolute top-3 right-3 bg-green-500 text-white px-3 py-1 rounded-full text-xs font-semibold flex items-center space-x-1">
                                        <svg className="w-4 h-4" fill="currentColor" viewBox="0 0 20 20">
                                            <path fillRule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clipRule="evenodd" />
                                        </svg>
                                        <span>Verified</span>
                                    </div>
                                )}
                                <div className="absolute top-3 left-3">
                                    {item.saleType === 'AUCTION' && (
                                        <span className="badge badge-info">Auction</span>
                                    )}
                                    {item.saleType === 'FIXED_PRICE' && (
                                        <span className="badge badge-success">Buy Now</span>
                                    )}
                                    {item.saleType === 'HYBRID' && (
                                        <span className="badge badge-warning">Hybrid</span>
                                    )}
                                </div>
                            </div>

                            <div className="p-4">
                                <div className="text-sm text-gray-500 mb-1">{item.category}</div>
                                <h3 className="font-bold text-lg text-gray-900 mb-2 line-clamp-2">{item.title}</h3>

                                <div className="flex items-center justify-between mb-3">
                                    <div>
                                        <div className="text-sm text-gray-500">Current Price</div>
                                        <div className="text-2xl font-bold text-primary-600">
                                            ${item.currentPrice.toLocaleString()}
                                        </div>
                                    </div>
                                </div>

                                {item.endTime && (
                                    <div className="flex items-center text-sm text-gray-600 mb-3">
                                        <svg className="w-4 h-4 mr-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
                                        </svg>
                                        <span>{getTimeRemaining(item.endTime)} remaining</span>
                                    </div>
                                )}

                                <button className="w-full btn-primary">
                                    {item.saleType === 'AUCTION' ? 'Place Bid' : 'Buy Now'}
                                </button>
                            </div>
                        </div>
                    ))}
                </div>

                {/* Empty State */}
                {mockItems.length === 0 && (
                    <div className="text-center py-16">
                        <div className="text-gray-400 mb-4">
                            <svg className="w-24 h-24 mx-auto" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M20 13V6a2 2 0 00-2-2H6a2 2 0 00-2 2v7m16 0v5a2 2 0 01-2 2H6a2 2 0 01-2-2v-5m16 0h-2.586a1 1 0 00-.707.293l-2.414 2.414a1 1 0 01-.707.293h-3.172a1 1 0 01-.707-.293l-2.414-2.414A1 1 0 006.586 13H4" />
                            </svg>
                        </div>
                        <h3 className="text-xl font-semibold text-gray-700 mb-2">No items found</h3>
                        <p className="text-gray-500">Try adjusting your filters</p>
                    </div>
                )}
            </div>
        </div>
    );
};

export default BrowsePage;
