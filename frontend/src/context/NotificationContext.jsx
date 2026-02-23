import React, { createContext, useContext, useState, useEffect, useRef } from 'react';
import { useAuth } from './AuthContext';
import apiClient from '../utils/apiClient';
import SockJS from 'sockjs-client';
import { Client } from '@stomp/stompjs';
import toast from 'react-hot-toast';

const NotificationContext = createContext(null);

export const NotificationProvider = ({ children }) => {
    const { user, isAuthenticated } = useAuth();
    const [notifications, setNotifications] = useState([]);
    const [unreadCount, setUnreadCount] = useState(0);
    const [loading, setLoading] = useState(false);
    
    const clientRef = useRef(null);
    const processedMessageIds = useRef(new Set()); // For exact deduplication
    
    const fetchNotifications = async () => {
        if (!isAuthenticated) return;
        setLoading(true);
        try {
            const [notifsRes, countRes] = await Promise.all([
                apiClient.get('/notifications?page=0&size=50'),
                apiClient.get('/notifications/unread-count')
            ]);
            setNotifications(notifsRes.data.content || []);
            setUnreadCount(countRes.data.count || 0);
        } catch (error) {
            console.error('Failed to fetch notifications', error);
            // Non-blocking failure
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        if (isAuthenticated && user) {
            fetchNotifications();

            // Setup WebSocket
            const token = localStorage.getItem('token');
            const client = new Client({
                webSocketFactory: () => new SockJS('http://localhost:8080/ws'),
                connectHeaders: {
                    Authorization: `Bearer ${token}`
                },
                debug: function (str) {
                    // console.log(str);
                },
                reconnectDelay: 5000,
                heartbeatIncoming: 4000,
                heartbeatOutgoing: 4000,
            });

            client.onConnect = function (frame) {
                console.log('Connected to WebSocket for Notifications');
                client.subscribe(`/user/queue/notifications`, (message) => {
                    const notification = JSON.parse(message.body);
                    
                    // Deduplication logic
                    if (processedMessageIds.current.has(notification.id)) {
                        return;
                    }
                    processedMessageIds.current.add(notification.id);
                    // Prevent memory leak by keeping Set size manageable
                    if (processedMessageIds.current.size > 1000) {
                        processedMessageIds.current.clear();
                    }

                    // Prepend new notification
                    setNotifications(prev => [notification, ...prev]);
                    setUnreadCount(prev => prev + 1);

                    // Show Toast
                    toast(notification.message, {
                        icon: '🔔',
                        style: {
                            borderRadius: '10px',
                            background: '#333',
                            color: '#fff',
                        },
                    });
                });
            };

            client.onStompError = function (frame) {
                console.error('Broker reported error: ' + frame.headers['message']);
                console.error('Additional details: ' + frame.body);
            };

            client.activate();
            clientRef.current = client;

            return () => {
                client.deactivate();
            };
        } else {
            // Unauthenticated - clean up
            if (clientRef.current) {
                clientRef.current.deactivate();
                clientRef.current = null;
            }
            setNotifications([]);
            setUnreadCount(0);
        }
    }, [isAuthenticated, user]);

    const markAsRead = async (id) => {
        // Optimistic UI update
        const targetNotif = notifications.find(n => n.id === id);
        if (!targetNotif || targetNotif.isRead) return;

        setNotifications(prev => 
            prev.map(n => n.id === id ? { ...n, isRead: true } : n)
        );
        setUnreadCount(prev => Math.max(0, prev - 1));

        try {
            await apiClient.put(`/notifications/${id}/read`);
        } catch (error) {
            console.error('Failed to mark notification as read', error);
            // Optionally revert optimistic update here if critical
        }
    };

    const markAllAsRead = async () => {
        setNotifications(prev => prev.map(n => ({ ...n, isRead: true })));
        setUnreadCount(0);
        try {
            await apiClient.put('/notifications/read-all');
        } catch (error) {
            console.error('Failed to mark all as read', error);
        }
    };

    return (
        <NotificationContext.Provider value={{ 
            notifications, 
            unreadCount, 
            loading, 
            markAsRead, 
            markAllAsRead,
            fetchNotifications 
        }}>
            {children}
        </NotificationContext.Provider>
    );
};

export const useNotifications = () => {
    const context = useContext(NotificationContext);
    if (!context) {
        throw new Error('useNotifications must be used within NotificationProvider');
    }
    return context;
};
