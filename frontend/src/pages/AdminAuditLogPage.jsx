import React, { useState, useEffect } from 'react';
import apiClient from '../utils/apiClient';
import { formatDistanceToNow } from 'date-fns';

const AdminAuditLogPage = () => {
    const [logs, setLogs] = useState([]);
    const [entityTypeFilter, setEntityTypeFilter] = useState('');
    const [page, setPage] = useState(0);
    const [totalPages, setTotalPages] = useState(0);
    const [loading, setLoading] = useState(true);

    const fetchLogs = async (p = 0) => {
        setLoading(true);
        try {
            const params = new URLSearchParams({ page: p, size: 20 });
            if (entityTypeFilter) params.append('entityType', entityTypeFilter);
            const res = await apiClient.get(`/admin/audit-logs?${params}`);
            setLogs(res.data.content || []);
            setTotalPages(res.data.totalPages || 0);
            setPage(p);
        } catch (err) {
            console.error('Failed to fetch audit logs', err);
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => { fetchLogs(); }, [entityTypeFilter]);

    const actionColors = {
        BAN_USER: 'text-red-600',
        UNBAN_USER: 'text-green-600',
        APPROVE_ITEM: 'text-green-600',
        REJECT_ITEM: 'text-red-600',
        RESOLVE_FRAUD_REPORT: 'text-blue-600',
    };

    return (
        <div className="min-h-screen bg-gray-50 py-8">
            <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
                <div className="flex justify-between items-center mb-8">
                    <h1 className="text-3xl font-bold text-gray-900">Audit Logs</h1>
                    <select value={entityTypeFilter} onChange={(e) => setEntityTypeFilter(e.target.value)}
                        className="px-4 py-2 border border-gray-300 rounded-lg text-sm bg-white">
                        <option value="">All Entity Types</option>
                        <option value="User">User</option>
                        <option value="Item">Item</option>
                        <option value="FraudReport">Fraud Report</option>
                    </select>
                </div>

                {loading ? (
                    <div className="flex justify-center py-12">
                        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-600"></div>
                    </div>
                ) : logs.length === 0 ? (
                    <div className="bg-white rounded-xl shadow-sm border border-gray-100 p-12 text-center">
                        <span className="text-4xl mb-4 block">📋</span>
                        <p className="text-gray-600 text-lg">No audit logs found.</p>
                    </div>
                ) : (
                    <>
                        <div className="bg-white rounded-xl shadow-sm border border-gray-100 overflow-hidden">
                            <table className="min-w-full divide-y divide-gray-200">
                                <thead className="bg-gray-50">
                                    <tr>
                                        <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Time</th>
                                        <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Action</th>
                                        <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Entity</th>
                                        <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Details</th>
                                        <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Admin</th>
                                    </tr>
                                </thead>
                                <tbody className="bg-white divide-y divide-gray-200">
                                    {logs.map(log => (
                                        <tr key={log.id}>
                                            <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                                                {log.createdAt ? formatDistanceToNow(new Date(log.createdAt), { addSuffix: true }) : '-'}
                                            </td>
                                            <td className="px-6 py-4 whitespace-nowrap">
                                                <span className={`text-sm font-medium ${actionColors[log.action] || 'text-gray-900'}`}>
                                                    {log.action}
                                                </span>
                                            </td>
                                            <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-600">
                                                {log.entityType} #{log.entityId}
                                            </td>
                                            <td className="px-6 py-4 text-sm text-gray-600 max-w-md truncate">
                                                {log.details}
                                            </td>
                                            <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                                                {log.admin?.email || '-'}
                                            </td>
                                        </tr>
                                    ))}
                                </tbody>
                            </table>
                        </div>

                        <div className="flex justify-between items-center mt-6">
                            <button disabled={page === 0} onClick={() => fetchLogs(page - 1)}
                                className="px-4 py-2 bg-white border border-gray-300 rounded-lg text-sm font-medium text-gray-700 hover:bg-gray-50 disabled:opacity-50">
                                Previous
                            </button>
                            <span className="text-sm text-gray-600">Page {page + 1} of {totalPages}</span>
                            <button disabled={page >= totalPages - 1} onClick={() => fetchLogs(page + 1)}
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

export default AdminAuditLogPage;
