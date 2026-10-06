import React, { useState, useEffect, useRef } from 'react';
import { X, Send, MessageCircle } from 'lucide-react';
import api from '../api/axios';
import { useAuth } from '../context/AuthContext';
import { toast } from 'react-toastify';

// Booking chat modal
const BookingChatModal = ({ booking, onClose }) => {
  const { user } = useAuth();

  // Chat messages and input
  const [messages, setMessages] = useState([]);
  const [newMessage, setNewMessage] = useState('');

  // Loading states
  const [loading, setLoading] = useState(true);
  const [sending, setSending] = useState(false);

  // Reference for auto scroll
  const bottomRef = useRef(null);

  // Fetch booking messages
  const fetchMessages = async () => {
    try {
      const res = await api.get(`/api/chat/booking/${booking.bookingId}`);
      setMessages(res.data);

      // Mark messages as read
      await api.put(`/api/chat/booking/${booking.bookingId}/read`).catch(() => {});
    } catch (err) {
      console.error('Error fetching chat messages', err);
    } finally {
      setLoading(false);
    }
  };

  // Load messages and refresh every 8 seconds
  useEffect(() => {
    fetchMessages();
    const interval = setInterval(fetchMessages, 8000);

    return () => clearInterval(interval);
  }, [booking.bookingId]);

  // Scroll to latest message
  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages]);

  // Send new message
  const handleSend = async (e) => {
    e.preventDefault();

    // Don't send empty messages
    if (!newMessage.trim()) return;

    setSending(true);

    try {
      // Send message to backend
      await api.post(`/api/chat/booking/${booking.bookingId}`, {
        message: newMessage.trim()
      });

      setNewMessage('');

      // Refresh messages
      fetchMessages();
    } catch (err) {
      toast.error('Failed to send message');
    } finally {
      setSending(false);
    }
  };

  // Format message time
  const formatTime = (dt) => {
    if (!dt) return '';

    return new Date(dt).toLocaleTimeString('en-LK', {
      hour: '2-digit',
      minute: '2-digit'
    });
  };

  // Format message date
  const formatDate = (dt) => {
    if (!dt) return '';

    return new Date(dt).toLocaleDateString('en-LK', {
      day: 'numeric',
      month: 'short'
    });
  };

  return (
    // Chat modal
    <div className="fixed inset-0 z-50 bg-black/90 backdrop-blur-md flex items-center justify-center p-4">

      <div
        className="glass-card rounded-3xl max-w-lg w-full border border-gold/40 flex flex-col"
        style={{ height: '560px' }}
      >

        {/* Chat header */}
        <div className="flex items-center justify-between px-6 py-4 border-b border-gray-800">
          <div>
            <div className="flex items-center space-x-2">
              <MessageCircle className="w-4 h-4 text-gold" />
              <span className="text-xs font-mono text-gold uppercase tracking-wider">
                Event Chat
              </span>
            </div>

            <h3 className="font-serif-title text-base font-bold text-white mt-0.5">
              Booking #{booking.bookingId} — {booking.packageName}
            </h3>

            <p className="text-[11px] text-gray-500">
              {booking.photographerName
                ? `with ${booking.photographerName}`
                : 'Awaiting photographer assignment'}
            </p>
          </div>

          {/* Close chat */}
          <button onClick={onClose} className="p-1.5 text-gray-400 hover:text-white">
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Messages area */}
        <div className="flex-1 overflow-y-auto p-4 space-y-3">

          {/* Loading indicator */}
          {loading ? (
            <div className="flex justify-center py-10">
              <div className="w-7 h-7 border-2 border-gold/20 border-t-gold rounded-full animate-spin" />
            </div>

          ) : messages.length === 0 ? (

            // No messages
            <div className="text-center py-10 space-y-2">
              <MessageCircle className="w-8 h-8 text-gray-700 mx-auto" />
              <p className="text-xs text-gray-500">
                No messages yet. Start the conversation!
              </p>
            </div>

          ) : (

            // Display messages
            messages.map((msg, i) => {
              // Check if message belongs to current user
              const isOwn = msg.senderId === user?.userId;

              // Show date when date changes
              const showDate =
                i === 0 ||
                formatDate(msg.createdAt) !==
                formatDate(messages[i - 1]?.createdAt);

              return (
                <div key={msg.messageId}>

                  {/* Date separator */}
                  {showDate && (
                    <div className="text-center my-2">
                      <span className="text-[10px] text-gray-600 bg-[#0f0f0f] px-3 py-0.5 rounded-full border border-gray-800">
                        {formatDate(msg.createdAt)}
                      </span>
                    </div>
                  )}

                  <div className={`flex ${isOwn ? 'justify-end' : 'justify-start'}`}>
                    <div
                      className={`max-w-[75%] ${
                        isOwn ? 'items-end' : 'items-start'
                      } flex flex-col`}
                    >

                      {/* Sender name */}
                      {!isOwn && (
                        <span className="text-[10px] text-gray-500 mb-0.5 ml-1">
                          {msg.senderName}
                        </span>
                      )}

                      {/* Message bubble */}
                      <div
                        className={`px-3.5 py-2 rounded-2xl text-xs leading-relaxed ${
                          isOwn
                            ? 'bg-gradient-to-br from-gold/20 to-gold/10 border border-gold/30 text-white rounded-tr-sm'
                            : 'bg-[#1a1a1f] border border-gray-800 text-gray-200 rounded-tl-sm'
                        }`}
                      >
                        {msg.message}
                      </div>

                      {/* Message time */}
                      <span className="text-[9px] text-gray-600 mt-0.5 mx-1">
                        {formatTime(msg.createdAt)}
                      </span>
                    </div>
                  </div>
                </div>
              );
            })
          )}

          {/* Bottom reference */}
          <div ref={bottomRef} />
        </div>

        {/* Message input */}
        <div className="px-4 py-4 border-t border-gray-800">
          <form onSubmit={handleSend} className="flex items-center space-x-2">

            {/* Message field */}
            <input
              type="text"
              placeholder="Type a message..."
              value={newMessage}
              onChange={(e) => setNewMessage(e.target.value)}
              className="flex-1 bg-[#151515] border border-gray-700 rounded-xl px-4 py-2.5 text-xs text-white placeholder-gray-500 focus:outline-none focus:border-gold"
              maxLength={500}
            />

            {/* Send button */}
            <button
              type="submit"
              disabled={sending || !newMessage.trim()}
              className="p-2.5 rounded-xl btn-gold disabled:opacity-50 disabled:pointer-events-none"
            >
              <Send className="w-4 h-4" />
            </button>

          </form>
        </div>
      </div>
    </div>
  );
};

export default BookingChatModal;