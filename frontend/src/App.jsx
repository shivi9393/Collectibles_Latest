import React from 'react';
import { Routes, Route, Navigate, useLocation } from 'react-router-dom';
import { useAuth } from './context/AuthContext';
import { Toaster } from 'react-hot-toast';
import Navbar from './components/Navbar';
import LandingPage from './pages/LandingPage';
import LoginPage from './pages/LoginPage';
import RegisterPage from './pages/RegisterPage';
import BrowsePage from './pages/BrowsePage';
import DashboardPage from './pages/DashboardPage';
import AdminDashboardPage from './pages/AdminDashboardPage';
import AdminUsersPage from './pages/AdminUsersPage';
import AdminListingsPage from './pages/AdminListingsPage';
import AdminFraudPage from './pages/AdminFraudPage';
import AdminAuditLogPage from './pages/AdminAuditLogPage';
import ItemDetailPage from './pages/ItemDetailPage';
import MyOrdersPage from './pages/MyOrdersPage';
import SellerAnalyticsPage from './pages/SellerAnalyticsPage';

const ProtectedRoute = ({ children }) => {
    const { isAuthenticated, loading } = useAuth();

    if (loading) {
        return (
            <div className="min-h-screen flex items-center justify-center">
                <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-600"></div>
            </div>
        );
    }

    return isAuthenticated ? children : <Navigate to="/login" />;
};

const AdminRoute = ({ children }) => {
    const { user, isAuthenticated, loading } = useAuth();

    if (loading) {
        return (
            <div className="min-h-screen flex items-center justify-center">
                <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-600"></div>
            </div>
        );
    }

    if (!isAuthenticated) return <Navigate to="/login" />;
    if (user?.role !== 'ADMIN') return <Navigate to="/dashboard" />;
    return children;
};

function App() {
    const { isAuthenticated } = useAuth();
    const location = useLocation();
    const hideNavbarPaths = ['/login', '/register'];

    return (
        <div className="min-h-screen bg-gray-50">
            <Toaster position="top-right" />
            {!hideNavbarPaths.includes(location.pathname) && <Navbar />}
            <Routes>
                <Route path="/" element={isAuthenticated ? <Navigate to="/browse" /> : <LandingPage />} />
                <Route path="/login" element={<LoginPage />} />
                <Route path="/register" element={<RegisterPage />} />
                <Route path="/browse" element={<BrowsePage />} />
                <Route path="/items/:id" element={<ItemDetailPage />} />
                <Route
                    path="/dashboard"
                    element={
                        <ProtectedRoute>
                            <DashboardPage />
                        </ProtectedRoute>
                    }
                />
                <Route path="/orders" element={<ProtectedRoute><MyOrdersPage /></ProtectedRoute>} />
                <Route path="/analytics" element={<ProtectedRoute><SellerAnalyticsPage /></ProtectedRoute>} />
                {/* Admin Routes */}
                <Route path="/admin" element={<AdminRoute><AdminDashboardPage /></AdminRoute>} />
                <Route path="/admin/users" element={<AdminRoute><AdminUsersPage /></AdminRoute>} />
                <Route path="/admin/listings" element={<AdminRoute><AdminListingsPage /></AdminRoute>} />
                <Route path="/admin/fraud" element={<AdminRoute><AdminFraudPage /></AdminRoute>} />
                <Route path="/admin/audit-logs" element={<AdminRoute><AdminAuditLogPage /></AdminRoute>} />
            </Routes>
        </div>
    );
}

export default App;
