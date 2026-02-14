import React from 'react';
import { Link } from 'react-router-dom';

const LandingPage = () => {
    return (
        <div className="min-h-screen">
            {/* Hero Section */}
            <section className="relative bg-gradient-to-br from-primary-600 via-primary-700 to-primary-900 text-white py-24">
                <div className="absolute inset-0 bg-black opacity-10"></div>
                <div className="relative max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
                    <div className="text-center">
                        <h1 className="text-5xl md:text-6xl font-extrabold mb-6 leading-tight">
                            Discover Rare Collectibles
                        </h1>
                        <p className="text-xl md:text-2xl mb-8 text-primary-100 max-w-3xl mx-auto">
                            Buy and sell unique collectibles through secure auctions and instant purchases
                        </p>
                        <div className="flex justify-center space-x-4">
                            <Link to="/browse" className="bg-white text-primary-700 hover:bg-gray-100 px-8 py-4 rounded-lg font-bold text-lg shadow-xl hover:shadow-2xl transition-all duration-200">
                                Start Browsing
                            </Link>
                            <Link to="/register" className="bg-primary-500 hover:bg-primary-400 text-white px-8 py-4 rounded-lg font-bold text-lg shadow-xl hover:shadow-2xl transition-all duration-200">
                                Join Now
                            </Link>
                        </div>
                    </div>
                </div>
            </section>

            {/* Features Section */}
            <section className="py-20 bg-white">
                <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
                    <h2 className="text-4xl font-bold text-center mb-16 text-gray-900">
                        Why Choose CollectMarket?
                    </h2>
                    <div className="grid md:grid-cols-3 gap-12">
                        <div className="text-center">
                            <div className="bg-primary-100 w-20 h-20 rounded-full flex items-center justify-center mx-auto mb-6">
                                <svg className="w-10 h-10 text-primary-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
                                </svg>
                            </div>
                            <h3 className="text-2xl font-bold mb-4">Hard-Close Auctions</h3>
                            <p className="text-gray-600">
                                Fair auctions with fixed end times. No last-minute extensions.
                            </p>
                        </div>

                        <div className="text-center">
                            <div className="bg-primary-100 w-20 h-20 rounded-full flex items-center justify-center mx-auto mb-6">
                                <svg className="w-10 h-10 text-primary-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z" />
                                </svg>
                            </div>
                            <h3 className="text-2xl font-bold mb-4">Secure Escrow</h3>
                            <p className="text-gray-600">
                                Payments held safely until delivery is confirmed by buyer.
                            </p>
                        </div>

                        <div className="text-center">
                            <div className="bg-primary-100 w-20 h-20 rounded-full flex items-center justify-center mx-auto mb-6">
                                <svg className="w-10 h-10 text-primary-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2m-6 9l2 2 4-4" />
                                </svg>
                            </div>
                            <h3 className="text-2xl font-bold mb-4">Verified Items</h3>
                            <p className="text-gray-600">
                                Authenticity certificates reviewed by our expert team.
                            </p>
                        </div>
                    </div>
                </div>
            </section>

            {/* Categories Section */}
            <section className="py-20 bg-gray-50">
                <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
                    <h2 className="text-4xl font-bold text-center mb-16 text-gray-900">
                        Popular Categories
                    </h2>
                    <div className="grid md:grid-cols-4 gap-6">
                        {['Coins', 'Figurines', 'Trading Cards', 'Antiques'].map((category) => (
                            <Link
                                key={category}
                                to="/browse"
                                className="card p-8 text-center hover:scale-105 transition-transform duration-200"
                            >
                                <h3 className="text-xl font-bold text-gray-900">{category}</h3>
                            </Link>
                        ))}
                    </div>
                </div>
            </section>

            {/* CTA Section */}
            <section className="py-20 bg-primary-600 text-white">
                <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 text-center">
                    <h2 className="text-4xl font-bold mb-6">Ready to Start Collecting?</h2>
                    <p className="text-xl mb-8 text-primary-100">
                        Join thousands of collectors buying and selling rare items every day
                    </p>
                    <Link to="/register" className="bg-white text-primary-700 hover:bg-gray-100 px-8 py-4 rounded-lg font-bold text-lg shadow-xl hover:shadow-2xl transition-all duration-200 inline-block">
                        Create Free Account
                    </Link>
                </div>
            </section>
        </div>
    );
};

export default LandingPage;
