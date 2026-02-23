import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import apiClient from '../utils/apiClient';
import { formatDistanceToNow } from 'date-fns';

const ItemDetailPage = () => {
    const { id } = useParams();
    const navigate = useNavigate();
    const { user, isAuthenticated } = useAuth();
    const [item, setItem] = useState(null);
    const [bids, setBids] = useState([]);
    const [bidAmount, setBidAmount] = useState('');
    const [proxyMax, setProxyMax] = useState('');
    const [loading, setLoading] = useState(true);
    const [bidding, setBidding] = useState(false);
    const [error, setError] = useState('');
    const [timeLeft, setTimeLeft] = useState('');

    useEffect(() => {
        const fetchItem = async () => {
            try {
                const res = await apiClient.get(`/items/${id}`);
                setItem(res.data);

                // Fetch bids if auction exists
                if (res.data.auction) {
                    const bidsRes = await apiClient.get(`/bids/auction/${res.data.auction.id}`);
                    setBids(bidsRes.data);
                }
            } catch (err) {
                setError('Failed to load item');
            } finally {
                setLoading(false);
            }
        };
        fetchItem();
    }, [id]);

    // Countdown timer
    useEffect(() => {
        if (!item?.auction?.endTime) return;
        const interval = setInterval(() => {
            const end = new Date(item.auction.endTime).getTime();
            const now = Date.now();
            const diff = end - now;

            if (diff <= 0) {
                setTimeLeft('Auction Ended');
                clearInterval(interval);
                return;
            }

            const days = Math.floor(diff / (1000 * 60 * 60 * 24));
            const hours = Math.floor((diff % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60));
            const mins = Math.floor((diff % (1000 * 60 * 60)) / (1000 * 60));
            const secs = Math.floor((diff % (1000 * 60)) / 1000);
            setTimeLeft(`${days}d ${hours}h ${mins}m ${secs}s`);
        }, 1000);
        return () => clearInterval(interval);
    }, [item?.auction?.endTime]);

    const handlePlaceBid = async (e) => {
        e.preventDefault();
        if (!isAuthenticated) { navigate('/login'); return; }
        setBidding(true);
        setError('');
        try {
            const payload = { itemId: Number(id), amount: Number(bidAmount) };
            if (proxyMax) payload.maxProxyAmount = Number(proxyMax);
            await apiClient.post('/bids', payload);

            // Refresh bids
            if (item.auction) {
                const bidsRes = await apiClient.get(`/bids/auction/${item.auction.id}`);
                setBids(bidsRes.data);
            }
            // Refresh item for updated currentPrice
            const res = await apiClient.get(`/items/${id}`);
            setItem(res.data);
            setBidAmount('');
            setProxyMax('');
        } catch (err) {
            setError(err.response?.data?.error || 'Failed to place bid');
        } finally {
            setBidding(false);
        }
    };

    const handleBuyNow = async () => {
        if (!isAuthenticated) { navigate('/login'); return; }
        try {
            await apiClient.post('/orders', { itemId: Number(id) });
            navigate('/orders');
        } catch (err) {
            setError(err.response?.data?.error || 'Failed to complete purchase');
        }
    };

    if (loading) {
        return (
            <div className="min-h-screen flex items-center justify-center">
                <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-600"></div>
            </div>
        );
    }

    if (!item) {
        return (
            <div className="min-h-screen flex items-center justify-center">
                <p className="text-gray-600 text-lg">Item not found</p>
            </div>
        );
    }

    const isAuction = item.saleType === 'AUCTION' || item.saleType === 'AUCTION_WITH_BUY_NOW';
    const isFixedPrice = item.saleType === 'FIXED_PRICE' || item.saleType === 'AUCTION_WITH_BUY_NOW';
    const isSeller = user && item.seller?.id === user.id;
    const auctionActive = item.auction?.status === 'ACTIVE';

    return (
        <div className="min-h-screen bg-gray-50 py-8">
            <div className="max-w-6xl mx-auto px-4 sm:px-6 lg:px-8">
                <button onClick={() => navigate(-1)} className="mb-6 text-gray-600 hover:text-gray-900 font-medium">
                    ← Back
                </button>

                <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
                    {/* Left: Image & Details */}
                    <div>
                        <div className="bg-white rounded-xl shadow-sm border border-gray-100 overflow-hidden">
                            {item.images && item.images.length > 0 ? (
                                <img src={`http://localhost:8080${item.images[0].imageUrl}`}
                                    alt={item.title} className="w-full h-96 object-cover" />
                            ) : (
                                <div className="w-full h-96 bg-gradient-to-br from-gray-200 to-gray-300 flex items-center justify-center">
                                    <span className="text-6xl">📦</span>
                                </div>
                            )}
                        </div>

                        <div className="bg-white rounded-xl shadow-sm border border-gray-100 p-6 mt-6">
                            <h2 className="text-lg font-bold text-gray-900 mb-3">Description</h2>
                            <p className="text-gray-600 whitespace-pre-wrap">{item.description || 'No description provided.'}</p>

                            <div className="grid grid-cols-2 gap-4 mt-6 text-sm">
                                {item.category && (
                                    <div><span className="text-gray-500">Category:</span> <span className="font-medium text-gray-900">{item.category}</span></div>
                                )}
                                {item.yearEra && (
                                    <div><span className="text-gray-500">Era:</span> <span className="font-medium text-gray-900">{item.yearEra}</span></div>
                                )}
                                {item.conditionDescription && (
                                    <div className="col-span-2"><span className="text-gray-500">Condition:</span> <span className="font-medium text-gray-900">{item.conditionDescription}</span></div>
                                )}
                            </div>
                        </div>
                    </div>

                    {/* Right: Bidding / Buy Now */}
                    <div className="space-y-6">
                        <div className="bg-white rounded-xl shadow-sm border border-gray-100 p-6">
                            <h1 className="text-2xl font-bold text-gray-900 mb-2">{item.title}</h1>
                            <div className="flex items-center gap-3 mb-4">
                                <span className={`inline-flex px-2 py-1 text-xs font-semibold rounded-full ${item.status === 'ACTIVE' ? 'bg-green-100 text-green-800' :
                                        item.status === 'SOLD' ? 'bg-red-100 text-red-800' : 'bg-gray-100 text-gray-800'
                                    }`}>{item.status}</span>
                                <span className="text-xs text-gray-500">{item.saleType?.replace(/_/g, ' ')}</span>
                                {item.isVerified && <span className="text-xs text-blue-600 font-medium">✓ Verified</span>}
                            </div>

                            {/* Price / Current Bid */}
                            <div className="bg-gray-50 rounded-lg p-4 mb-4">
                                {isAuction && (
                                    <div className="flex justify-between items-center mb-2">
                                        <span className="text-sm text-gray-500">Current Bid</span>
                                        <span className="text-2xl font-bold text-gray-900">
                                            ${item.currentPrice || item.startingBid || '0.00'}
                                        </span>
                                    </div>
                                )}
                                {isFixedPrice && item.buyNowPrice && (
                                    <div className="flex justify-between items-center">
                                        <span className="text-sm text-gray-500">Buy Now Price</span>
                                        <span className="text-2xl font-bold text-green-700">${item.buyNowPrice}</span>
                                    </div>
                                )}
                                {item.fixedPrice && item.saleType === 'FIXED_PRICE' && (
                                    <div className="flex justify-between items-center">
                                        <span className="text-sm text-gray-500">Price</span>
                                        <span className="text-2xl font-bold text-gray-900">${item.fixedPrice}</span>
                                    </div>
                                )}
                            </div>

                            {/* Countdown */}
                            {isAuction && auctionActive && timeLeft && (
                                <div className="bg-yellow-50 border border-yellow-200 rounded-lg p-3 mb-4 text-center">
                                    <span className="text-sm text-yellow-800 font-medium">⏱ Time Left: </span>
                                    <span className="text-lg font-bold text-yellow-900">{timeLeft}</span>
                                </div>
                            )}

                            {/* Bid Form */}
                            {isAuction && auctionActive && !isSeller && item.status === 'ACTIVE' && (
                                <form onSubmit={handlePlaceBid} className="space-y-3 mb-4">
                                    <div>
                                        <label className="block text-sm font-medium text-gray-700 mb-1">Your Bid ($)</label>
                                        <input type="number" step="0.01" required value={bidAmount} onChange={(e) => setBidAmount(e.target.value)}
                                            className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-transparent"
                                            placeholder={`Min: $${Number(item.currentPrice || item.startingBid || 0) + 1}`} />
                                    </div>
                                    <div>
                                        <label className="block text-sm font-medium text-gray-700 mb-1">Proxy Bid Max (optional)</label>
                                        <input type="number" step="0.01" value={proxyMax} onChange={(e) => setProxyMax(e.target.value)}
                                            className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-transparent"
                                            placeholder="Auto-bid up to this amount" />
                                    </div>
                                    <button type="submit" disabled={bidding}
                                        className="w-full py-3 bg-primary-600 text-white rounded-lg font-semibold hover:bg-primary-700 transition-colors disabled:opacity-50">
                                        {bidding ? 'Placing Bid...' : 'Place Bid'}
                                    </button>
                                </form>
                            )}

                            {/* Buy Now */}
                            {isFixedPrice && !isSeller && item.status === 'ACTIVE' && (
                                <button onClick={handleBuyNow}
                                    className="w-full py-3 bg-green-600 text-white rounded-lg font-semibold hover:bg-green-700 transition-colors mb-4">
                                    🛒 Buy Now — ${item.buyNowPrice || item.fixedPrice}
                                </button>
                            )}

                            {error && <p className="text-red-600 text-sm">{error}</p>}
                        </div>

                        {/* Bid History */}
                        {isAuction && bids.length > 0 && (
                            <div className="bg-white rounded-xl shadow-sm border border-gray-100 p-6">
                                <h2 className="text-lg font-bold text-gray-900 mb-4">Bid History ({bids.length})</h2>
                                <div className="space-y-2 max-h-64 overflow-y-auto">
                                    {bids.map((bid, i) => (
                                        <div key={bid.id} className={`flex justify-between items-center p-3 rounded-lg ${i === 0 ? 'bg-green-50 border border-green-200' : 'bg-gray-50'}`}>
                                            <div>
                                                <span className="text-sm font-medium text-gray-900">{bid.bidder?.username || 'Bidder'}</span>
                                                {i === 0 && <span className="ml-2 text-xs text-green-600 font-medium">Highest</span>}
                                            </div>
                                            <div className="text-right">
                                                <span className="text-sm font-bold text-gray-900">${bid.amount}</span>
                                                <p className="text-xs text-gray-400">
                                                    {bid.createdAt ? formatDistanceToNow(new Date(bid.createdAt), { addSuffix: true }) : ''}
                                                </p>
                                            </div>
                                        </div>
                                    ))}
                                </div>
                            </div>
                        )}

                        {/* Seller Info */}
                        <div className="bg-white rounded-xl shadow-sm border border-gray-100 p-6">
                            <h2 className="text-lg font-bold text-gray-900 mb-3">Seller</h2>
                            <div className="flex items-center gap-3">
                                <div className="w-10 h-10 rounded-full bg-primary-100 flex items-center justify-center text-primary-700 font-bold">
                                    {(item.seller?.username || 'S')[0].toUpperCase()}
                                </div>
                                <div>
                                    <p className="font-medium text-gray-900">{item.seller?.username || 'Unknown'}</p>
                                    <p className="text-xs text-gray-500">{item.viewCount || 0} views • {item.watchCount || 0} watchers</p>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    );
};

export default ItemDetailPage;
