import React from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

const Navbar = () => {
    const { user, logout, isAuthenticated } = useAuth();

    return (
        <nav className="bg-white shadow-md sticky top-0 z-50">
            <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
                <div className="flex justify-between items-center h-16">
                    <Link to="/" className="flex items-center space-x-2">
                        <div className="text-2xl font-bold bg-gradient-to-r from-primary-600 to-primary-800 bg-clip-text text-transparent">
                            CollectMarket
                        </div>
                    </Link>

                    <div className="flex items-center space-x-6">
                        <Link to="/browse" className="text-gray-700 hover:text-primary-600 font-medium transition-colors">
                            Browse
                        </Link>

                        {isAuthenticated ? (
                            <>
                                <Link to="/dashboard" className="text-gray-700 hover:text-primary-600 font-medium transition-colors">
                                    Dashboard
                                </Link>
                                <div className="flex items-center space-x-4">
                                    <span className="text-sm text-gray-600">
                                        {user?.username}
                                    </span>
                                    <button
                                        onClick={logout}
                                        className="bg-gray-200 hover:bg-gray-300 text-gray-800 px-4 py-2 rounded-lg font-medium transition-colors"
                                    >
                                        Logout
                                    </button>
                                </div>
                            </>
                        ) : (
                            <>
                                <Link to="/login" className="text-gray-700 hover:text-primary-600 font-medium transition-colors">
                                    Login
                                </Link>
                                <Link to="/register" className="btn-primary">
                                    Sign Up
                                </Link>
                            </>
                        )}
                    </div>
                </div>
            </div>
        </nav>
    );
};

export default Navbar;
