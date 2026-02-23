import React, { useState, useEffect } from 'react';
import apiClient from '../utils/apiClient';

const AdminFraudPage = () => {
    const [reports, setReports] = useState([]);
    const [statusFilter, setStatusFilter] = useState('');
    const [page, setPage] = useState(0);
    const [totalPages, setTotalPages] = useState(0);
    const [loading, setLoading] = useState(true);

    const fetchReports = async (p = 0) => {
        setLoading(true);
        try {
            const params = new URLSearchParams({ page: p, size: 15 });
            if (statusFilter) params.append('status', statusFilter);
            const res = await apiClient.get(`/admin/fraud-reports?${params}`);
            setReports(res.data.content || []);
            setTotalPages(res.data.totalPages || 0);
            setPage(p);
        } catch (err) {
            console.error('Failed to fetch fraud reports', err);
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => { fetchReports(); }, [statusFilter]);

    const handleResolve = async (reportId, newStatus) => {
        const notes = prompt(`Enter resolution notes for marking as ${newStatus}:`);
        if (notes === null) return;
        try {
            await apiClient.put(`/admin/fraud-reports/${reportId}/resolve`, { status: newStatus, notes });
            fetchReports(page);
        } catch (err) {
            alert('Failed to resolve: ' + (err.response?.data?.message || err.message));
        }
    };

    const statusColors = {
        PENDING: 'bg-yellow-100 text-yellow-800',
        INVESTIGATING: 'bg-blue-100 text-blue-800',
        RESOLVED: 'bg-green-100 text-green-800',
        DISMISSED: 'bg-gray-100 text-gray-800',
    };

    return (
        <div className="min-h-screen bg-gray-50 py-8">
            <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
                <div className="flex justify-between items-center mb-8">
                    <h1 className="text-3xl font-bold text-gray-900">Fraud Reports</h1>
                    <select value={statusFilter} onChange={(e) => setStatusFilter(e.target.value)}
                        className="px-4 py-2 border border-gray-300 rounded-lg text-sm bg-white">
                        <option value="">All Statuses</option>
                        <option value="PENDING">Pending</option>
                        <option value="INVESTIGATING">Investigating</option>
                        <option value="RESOLVED">Resolved</option>
                        <option value="DISMISSED">Dismissed</option>
                    </select>
                </div>

                {loading ? (
                    <div className="flex justify-center py-12">
                        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-600"></div>
                    </div>
                ) : reports.length === 0 ? (
                    <div className="bg-white rounded-xl shadow-sm border border-gray-100 p-12 text-center">
                        <span className="text-4xl mb-4 block">🎉</span>
                        <p className="text-gray-600 text-lg">No fraud reports found!</p>
                    </div>
                ) : (
                    <>
                        <div className="space-y-4">
                            {reports.map(report => (
                                <div key={report.id} className="bg-white rounded-xl shadow-sm border border-gray-100 p-6">
                                    <div className="flex justify-between items-start">
                                        <div className="flex-1">
                                            <div className="flex items-center gap-3 mb-2">
                                                <span className="text-sm font-bold text-gray-900">Report #{report.id}</span>
                                                <span className={`inline-flex px-2 py-1 text-xs font-semibold rounded-full ${statusColors[report.status] || 'bg-gray-100'}`}>
                                                    {report.status}
                                                </span>
                                                <span className="text-xs text-gray-400">{report.reportType}</span>
                                            </div>
                                            <p className="text-sm text-gray-700">{report.description}</p>
                                            {report.resolutionNotes && (
                                                <p className="text-sm text-gray-500 mt-2 italic">Resolution: {report.resolutionNotes}</p>
                                            )}
                                        </div>
                                        {(report.status === 'PENDING' || report.status === 'INVESTIGATING') && (
                                            <div className="flex gap-2 flex-shrink-0 ml-4">
                                                {report.status === 'PENDING' && (
                                                    <button onClick={() => handleResolve(report.id, 'INVESTIGATING')}
                                                        className="px-3 py-1.5 bg-blue-600 text-white rounded-lg text-xs font-medium hover:bg-blue-700">
                                                        Investigate
                                                    </button>
                                                )}
                                                <button onClick={() => handleResolve(report.id, 'RESOLVED')}
                                                    className="px-3 py-1.5 bg-green-600 text-white rounded-lg text-xs font-medium hover:bg-green-700">
                                                    Resolve
                                                </button>
                                                <button onClick={() => handleResolve(report.id, 'DISMISSED')}
                                                    className="px-3 py-1.5 bg-gray-500 text-white rounded-lg text-xs font-medium hover:bg-gray-600">
                                                    Dismiss
                                                </button>
                                            </div>
                                        )}
                                    </div>
                                </div>
                            ))}
                        </div>

                        <div className="flex justify-between items-center mt-6">
                            <button disabled={page === 0} onClick={() => fetchReports(page - 1)}
                                className="px-4 py-2 bg-white border border-gray-300 rounded-lg text-sm font-medium text-gray-700 hover:bg-gray-50 disabled:opacity-50">
                                Previous
                            </button>
                            <span className="text-sm text-gray-600">Page {page + 1} of {totalPages}</span>
                            <button disabled={page >= totalPages - 1} onClick={() => fetchReports(page + 1)}
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

export default AdminFraudPage;
