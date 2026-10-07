import React, { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { useCart } from '../../context/CartContext';
import api from '../../api/axios';
import { 
  Calendar, 
  Clock, 
  MapPin, 
  User, 
  Download, 
  FileText, 
  Camera, 
  Star, 
  AlertCircle, 
  CheckCircle2, 
  XCircle, 
  CreditCard,
  Building,
  Sparkles,
  X,
  Eye,
  Lock,
  Heart,
  Edit3,
  MessageCircle
} from 'lucide-react';
import { toast } from 'react-toastify';
import { getImageUrl, downloadImage } from '../../utils/imageUrl';
import BookingChatModal from '../../components/BookingChatModal';

const ClientDashboard = () => {
  const { user } = useAuth();
  const navigate = useNavigate();
  const { selectPackage, updateBookingDetails } = useCart();
  const [bookings, setBookings] = useState([]);
  const [reviews, setReviews] = useState({}); // bookingId → { reviewId, starRating, reviewComment }
  const [loading, setLoading] = useState(true);

  // Modals state
  const [galleryBooking, setGalleryBooking] = useState(null);
  const [galleryPhotos, setGalleryPhotos] = useState([]);
  const [loadingPhotos, setLoadingPhotos] = useState(false);
  const [showFavoritesOnly, setShowFavoritesOnly] = useState(false);

  const [paymentBooking, setPaymentBooking] = useState(null);
  const [modalPaymentMethod, setModalPaymentMethod] = useState('CARD'); // 'CARD' | 'BANK_TRANSFER'
  const [modalCardName, setModalCardName] = useState(user?.fullName || '');
  const [modalCardNumber, setModalCardNumber] = useState('');
  const [modalCardExpiry, setModalCardExpiry] = useState('');
  const [modalCardCvv, setModalCardCvv] = useState('');
  const [paymentRef, setPaymentRef] = useState('');
  const [submittingPayment, setSubmittingPayment] = useState(false);

  const [reviewBooking, setReviewBooking] = useState(null);
  const [starRating, setStarRating] = useState(5);
  const [reviewComment, setReviewComment] = useState('');
  const [submittingReview, setSubmittingReview] = useState(false);
  const [editReview, setEditReview] = useState(null); // { reviewId, bookingId, starRating, reviewComment }
  const [deletingReviewId, setDeletingReviewId] = useState(null);

  const [downloadingReceiptId, setDownloadingReceiptId] = useState(null);

  // Edit Booking state
  const [editBookingModal, setEditBookingModal] = useState(null); // booking object
  const [editEventDate, setEditEventDate] = useState('');
  const [editEventTime, setEditEventTime] = useState('');
  const [editVenueAddress, setEditVenueAddress] = useState('');
  const [editClientNotes, setEditClientNotes] = useState('');
  const [savingEdit, setSavingEdit] = useState(false);

  // Chat state
  const [chatBooking, setChatBooking] = useState(null);


  const fetchBookings = async () => {
    try {
      const res = await api.get('/api/client/bookings');
      const bList = res.data;
      setBookings(bList);
      // Fetch existing review for each completed booking
      const reviewMap = {};
      await Promise.all(
        bList.filter(b => b.status === 'COMPLETED').map(async (b) => {
          try {
            const r = await api.get(`/api/client/bookings/${b.bookingId}/review`);
            if (r.data) reviewMap[b.bookingId] = r.data;
          } catch {
            // no review yet — that's fine
          }
        })
      );
      setReviews(reviewMap);
    } catch (err) {
      console.error('Error fetching client bookings', err);
      toast.error('Could not fetch your bookings');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchBookings();
  }, []);

  const formatLKR = (amount) => {
    return new Intl.NumberFormat('en-LK', {
      style: 'currency',
      currency: 'LKR',
      maximumFractionDigits: 0
    }).format(amount).replace('LKR', 'Rs.');
  };

  // Download PDF Receipt
  const handleDownloadReceipt = async (bookingId) => {
    setDownloadingReceiptId(bookingId);
    try {
      const res = await api.get(`/api/client/bookings/${bookingId}/receipt`, {
        responseType: 'blob'
      });
      const blob = new Blob([res.data], { type: 'application/pdf' });
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', `Pixora-Receipt-${bookingId}.pdf`);
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.URL.revokeObjectURL(url);
      toast.success(`PDF Receipt downloaded for Booking #${bookingId}`);
    } catch (err) {
      console.error('Receipt download error', err);
      toast.error('PDF receipt is available once your payment is approved by admin.');
    } finally {
      setDownloadingReceiptId(null);
    }
  };

  // Open Gallery Modal
  const handleOpenGallery = async (booking) => {
    setGalleryBooking(booking);
    setLoadingPhotos(true);
    try {
      const res = await api.get(`/api/client/bookings/${booking.bookingId}/photos`);
      setGalleryPhotos(res.data);
    } catch (err) {
      console.error('Error fetching gallery photos', err);
      toast.error('Could not load gallery photos');
    } finally {
      setLoadingPhotos(false);
    }
  };

  // Card input formatters & validator for modal
  const handleModalCardNumberChange = (e) => {
    const raw = e.target.value.replace(/\D/g, '').slice(0, 16);
    setModalCardNumber(raw.replace(/(\d{4})(?=\d)/g, '$1 '));
  };
  const handleModalCardExpiryChange = (e) => {
    const raw = e.target.value.replace(/\D/g, '').slice(0, 4);
    if (raw.length >= 3) {
      setModalCardExpiry(`${raw.slice(0, 2)}/${raw.slice(2)}`);
    } else {
      setModalCardExpiry(raw);
    }
  };
  const handleModalCardCvvChange = (e) => {
    setModalCardCvv(e.target.value.replace(/\D/g, '').slice(0, 4));
  };
  const validateExpiry = (val) => {
    if (!/^(0[1-9]|1[0-2])\/?([0-9]{2})$/.test(val)) return false;
    const parts = val.split('/');
    const month = parseInt(parts[0], 10);
    const year = parseInt(`20${parts[1]}`, 10);
    const now = new Date();
    const currentYear = now.getFullYear();
    const currentMonth = now.getMonth() + 1;
    if (year < currentYear) return false;
    if (year === currentYear && month < currentMonth) return false;
    return true;
  };

  // Submit Payment Modal
  const handleSubmitPayment = async (e) => {
    e.preventDefault();

    if (modalPaymentMethod === 'CARD') {
      if (!modalCardName.trim()) {
        toast.warning('Please enter the Cardholder Name');
        return;
      }
      const cleanCard = modalCardNumber.replace(/\s/g, '');
      if (cleanCard.length !== 16) {
        toast.warning('Please enter a valid 16-digit Card Number');
        return;
      }
      if (!validateExpiry(modalCardExpiry)) {
        toast.warning('Please enter a valid future Expiry Date (MM/YY)');
        return;
      }
      if (!/^\d{3,4}$/.test(modalCardCvv)) {
        toast.warning('Please enter a valid 3 or 4-digit CVV security code');
        return;
      }

      setSubmittingPayment(true);
      try {
        await api.post(`/api/client/bookings/${paymentBooking.bookingId}/payment`, {
          paymentMethod: 'CARD',
          cardholderName: modalCardName.trim(),
          cardNumber: cleanCard,
          expiryDate: modalCardExpiry.trim(),
          cvv: modalCardCvv.trim(),
          amountPaidLkr: paymentBooking.totalAmountLkr
        });
        toast.success('💳 Payment successful! Booking status updated to PAID.');
        setPaymentBooking(null);
        setModalCardNumber('');
        setModalCardExpiry('');
        setModalCardCvv('');
        fetchBookings();
      } catch (err) {
        console.error('Payment submit error', err);
        const errMsg = err.response?.data?.error || err.message || 'Failed to submit card payment';
        toast.error(errMsg);
      } finally {
        setSubmittingPayment(false);
      }
    } else {
      if (!paymentRef.trim()) {
        toast.warning('Please enter your Bank Transfer Reference / Deposit Slip number');
        return;
      }

      setSubmittingPayment(true);
      try {
        await api.post(`/api/client/bookings/${paymentBooking.bookingId}/payment`, {
          paymentMethod: 'BANK_TRANSFER',
          transactionRef: paymentRef.trim(),
          amountPaidLkr: paymentBooking.totalAmountLkr
        });
        toast.success('Payment submitted for admin approval!');
        setPaymentBooking(null);
        setPaymentRef('');
        fetchBookings();
      } catch (err) {
        console.error('Payment submit error', err);
        toast.error('Failed to submit payment reference');
      } finally {
        setSubmittingPayment(false);
      }
    }
  };

  // Submit Review Modal
  const handleSubmitReview = async (e) => {
    e.preventDefault();
    const comment = reviewComment.trim();
    if (comment.length < 3 || comment.length > 1000) {
      toast.warning('Feedback must be between 3 and 1000 characters long.');
      return;
    }
    setSubmittingReview(true);
    try {
      const res = await api.post(`/api/client/bookings/${reviewBooking.bookingId}/review`, {
        starRating,
        reviewComment: comment
      });
      toast.success('Thank you for your feedback!');
      const newReview = res.data;
      setReviews((prev) => ({
        ...prev,
        [reviewBooking.bookingId]: newReview
      }));
      setReviewBooking(null);
      setReviewComment('');
    } catch (err) {
      console.error('Review submit error', err);
      const msg = err.response?.data?.error || 'Failed to submit review';
      toast.error(msg);
    } finally {
      setSubmittingReview(false);
    }
  };

  // Save Edited Review
  const handleSaveEditReview = async (e) => {
    e.preventDefault();
    const comment = editReview.reviewComment.trim();
    if (comment.length < 3 || comment.length > 1000) {
      toast.warning('Feedback must be between 3 and 1000 characters long.');
      return;
    }
    try {
      const res = await api.put(`/api/client/reviews/${editReview.reviewId}`, {
        starRating: editReview.starRating,
        reviewComment: comment
      });
      toast.success('Review updated successfully!');
      const updatedReview = res.data;
      setReviews((prev) => ({
        ...prev,
        [editReview.bookingId]: updatedReview
      }));
      setEditReview(null);
    } catch (err) {
      console.error('Edit review error', err);
      const msg = err.response?.data?.error || 'Failed to update review';
      toast.error(msg);
    }
  };

  // Delete Review
  const handleDeleteReview = async (reviewId) => {
    if (!window.confirm('Delete your review? This cannot be undone.')) return;
    setDeletingReviewId(reviewId);
    try {
      await api.delete(`/api/client/reviews/${reviewId}`);
      toast.success('Review deleted.');
      // Immediate local state update:
      setReviews((prev) => {
        const next = { ...prev };
        for (const bId in next) {
          if (next[bId]?.reviewId === reviewId) {
            delete next[bId];
          }
        }
        return next;
      });
    } catch (err) {
      console.error('Delete review error', err);
      toast.error('Failed to delete review');
    } finally {
      setDeletingReviewId(null);
    }
  };



  // Cancel Booking
  const handleCancelBooking = async (bookingId) => {
    if (!window.confirm('Are you sure you want to cancel this booking?')) return;
    try {
      const res = await api.put(`/api/client/bookings/${bookingId}/cancel`);
      if (res.status === 204) {
        setBookings(prev => prev.filter(b => b.bookingId !== bookingId));
        toast.info('Pending booking cancelled and purged');
      } else {
        toast.info('Booking cancelled');
      }
      fetchBookings();
    } catch (err) {
      console.error('Cancel booking error', err);
      toast.error('Could not cancel booking');
    }
  };


  // Toggle Photo Favorite
  const handleToggleFavorite = async (photoId) => {
    try {
      const res = await api.put(`/api/client/photos/${photoId}/toggle-favorite`);
      setGalleryPhotos(prev =>
        prev.map(p => p.photoId === photoId ? { ...p, isFavorite: res.data.isFavorite } : p)
      );
    } catch (err) {
      toast.error('Failed to update favorite');
    }
  };

  // Open Edit Booking Modal
  const handleOpenEditBooking = (b) => {
    setEditBookingModal(b);
    setEditEventDate(b.eventDate || '');
    setEditEventTime(b.eventTime || '');
    setEditVenueAddress(b.venueAddress || '');
    setEditClientNotes(b.clientNotes || '');
  };

  // Save Edited Booking
  const handleSaveEditBooking = async (e) => {
    e.preventDefault();
    if (!editEventDate || !editEventTime || !editVenueAddress.trim()) {
      toast.warning('Please fill in all required fields');
      return;
    }
    setSavingEdit(true);
    try {
      await api.put(`/api/client/bookings/${editBookingModal.bookingId}`, {
        eventDate: editEventDate,
        eventTime: editEventTime,
        venueAddress: editVenueAddress.trim(),
        clientNotes: editClientNotes.trim()
      });
      toast.success('Booking updated! Photographer re-approval has been requested.');
      setEditBookingModal(null);
      fetchBookings();
    } catch (err) {
      const msg = err.response?.data?.error || 'Failed to update booking';
      toast.error(msg);
    } finally {
      setSavingEdit(false);
    }
  };


  const getStatusBadge = (status) => {
    switch (status) {
      case 'PAID':
        return (
          <span className="inline-flex items-center space-x-1 px-2.5 py-1 rounded-full text-xs font-semibold bg-emerald-950/60 text-emerald-400 border border-emerald-500/30">
            <CheckCircle2 className="w-3 h-3" />
            <span>PAID</span>
          </span>
        );
      case 'CONFIRMED':
        return (
          <span className="inline-flex items-center space-x-1 px-2.5 py-1 rounded-full text-xs font-semibold bg-green-950/60 text-green-400 border border-green-500/30">
            <CheckCircle2 className="w-3 h-3" />
            <span>Confirmed</span>
          </span>
        );
      case 'COMPLETED':
        return (
          <span className="inline-flex items-center space-x-1 px-2.5 py-1 rounded-full text-xs font-semibold bg-blue-950/60 text-blue-400 border border-blue-500/30">
            <CheckCircle2 className="w-3 h-3" />
            <span>Completed</span>
          </span>
        );
      case 'CANCELLED':
        return (
          <span className="inline-flex items-center space-x-1 px-2.5 py-1 rounded-full text-xs font-semibold bg-red-950/60 text-red-400 border border-red-500/30">
            <XCircle className="w-3 h-3" />
            <span>Cancelled</span>
          </span>
        );
      default:
        return (
          <span className="inline-flex items-center space-x-1 px-2.5 py-1 rounded-full text-xs font-semibold bg-yellow-950/60 text-yellow-400 border border-yellow-500/30">
            <AlertCircle className="w-3 h-3" />
            <span>Pending Admin Approval</span>
          </span>
        );
    }
  };

  const getPaymentBadge = (status) => {
    switch (status) {
      case 'PAID':
        return (
          <span className="text-[11px] font-mono px-2 py-0.5 rounded bg-emerald-900/40 text-emerald-400 border border-emerald-600/30">
            PAID
          </span>
        );
      case 'APPROVED':
        return (
          <span className="text-[11px] font-mono px-2 py-0.5 rounded bg-green-900/40 text-green-400 border border-green-600/30">
            Payment Approved
          </span>
        );
      case 'REJECTED':
        return (
          <span className="text-[11px] font-mono px-2 py-0.5 rounded bg-red-900/40 text-red-400 border border-red-600/30">
            Payment Rejected
          </span>
        );
      case 'PENDING_APPROVAL':
        return (
          <span className="text-[11px] font-mono px-2 py-0.5 rounded bg-yellow-900/40 text-yellow-400 border border-yellow-600/30">
            Verification Pending
          </span>
        );
      default:
        return (
          <span className="text-[11px] font-mono px-2 py-0.5 rounded bg-gray-800 text-gray-400">
            Payment Unsubmitted
          </span>
        );
    }
  };

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-14 space-y-10">
      
      {/* Client Welcome Header */}
      <div className="border-b border-gray-800 pb-6 flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <span className="text-xs uppercase font-mono tracking-widest text-gold">
            Client Portal
          </span>
          <h1 className="font-serif-title text-3xl sm:text-4xl font-bold text-white mt-1">
            My Birthday Celebrations
          </h1>
          <p className="text-xs sm:text-sm text-gray-400 mt-1">
            Track your reservations, download official PDF receipts, and access your event photo galleries.
          </p>
        </div>
        <div className="flex flex-wrap items-center gap-3 self-start sm:self-auto">
          <Link
            to="/packages"
            className="px-6 py-2.5 rounded-xl btn-gold text-xs font-semibold shadow-lg shadow-gold/20 flex items-center space-x-2"
          >
            <Sparkles className="w-3.5 h-3.5" />
            <span>Book Another Celebration</span>
          </Link>
        </div>
      </div>

      {/* Bookings List */}
      {loading ? (
        <div className="flex justify-center py-20">
          <div className="w-10 h-10 border-4 border-gold/20 border-t-gold rounded-full animate-spin"></div>
        </div>
      ) : bookings.length === 0 ? (
        <div className="glass-card rounded-3xl p-14 text-center max-w-xl mx-auto space-y-4">
          <Calendar className="w-12 h-12 text-gold mx-auto opacity-70" />
          <h3 className="font-serif-title text-xl font-bold text-white">No Bookings Found</h3>
          <p className="text-xs text-gray-400 leading-relaxed">
            You don't have any birthday celebrations booked yet. Browse our packages and secure your dream celebration photographer.
          </p>
          <div className="pt-2">
            <Link to="/packages" className="px-6 py-2.5 rounded-xl btn-gold text-xs font-semibold inline-block">
              Browse Birthday Packages
            </Link>
          </div>
        </div>
      ) : (
        <div className="space-y-6">
          {bookings.map((b) => (
            <div
              key={b.bookingId}
              className="glass-card rounded-2xl p-6 sm:p-8 border border-gray-800 hover:border-gold/40 transition-all space-y-6"
            >
              {/* Top Row: ID, Package, Statuses */}
              <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-gray-800/80 pb-5">
                <div>
                  <div className="flex items-center space-x-3">
                    <span className="font-mono text-xs uppercase tracking-wider text-gold">
                      Booking #{b.bookingId}
                    </span>
                    {getStatusBadge(b.status)}
                    {getPaymentBadge(b.paymentStatus)}
                  </div>
                  <h3 className="font-serif-title text-2xl font-bold text-white mt-1.5">
                    {b.packageName}
                  </h3>
                </div>

                <div className="text-left sm:text-right">
                  <span className="text-xs text-gray-400 block">Total Investment</span>
                  <span className="font-serif-title text-2xl font-bold text-gold">
                    {formatLKR(b.totalAmountLkr)}
                  </span>
                </div>
              </div>

              {/* Details Grid */}
              <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 text-xs text-gray-300">
                <div className="flex items-start space-x-2.5 p-3 rounded-xl bg-black/40 border border-gray-800/60">
                  <Calendar className="w-4 h-4 text-gold shrink-0 mt-0.5" />
                  <div>
                    <span className="text-gray-500 block">Event Date & Time</span>
                    <span className="font-semibold text-white">{b.eventDate}</span>
                    <span className="text-gray-400 block">{b.eventTime}</span>
                  </div>
                </div>

                <div className="flex items-start space-x-2.5 p-3 rounded-xl bg-black/40 border border-gray-800/60">
                  <MapPin className="w-4 h-4 text-gold shrink-0 mt-0.5" />
                  <div>
                    <span className="text-gray-500 block">Venue Address</span>
                    <span className="font-semibold text-white line-clamp-2">{b.venueAddress}</span>
                  </div>
                </div>

                <div className="flex items-start space-x-2.5 p-3 rounded-xl bg-black/40 border border-gray-800/60">
                  <User className="w-4 h-4 text-gold shrink-0 mt-0.5" />
                  <div>
                    <span className="text-gray-500 block">Assigned Photographer</span>
                    <span className="font-semibold text-white">
                      {b.photographerName || 'Pending Assignment'}
                    </span>
                  </div>
                </div>
              </div>

              {/* Action Buttons Bar */}
              <div className="pt-2 flex flex-wrap items-center gap-3">
                {/* 1. PDF Receipt Button */}
                {b.paymentStatus === 'APPROVED' || b.paymentStatus === 'PAID' || b.status === 'PAID' ? (
                  <button
                    onClick={() => handleDownloadReceipt(b.bookingId)}
                    disabled={downloadingReceiptId === b.bookingId}
                    className="px-4 py-2 rounded-xl btn-gold text-xs font-semibold flex items-center space-x-2 shadow"
                  >
                    {downloadingReceiptId === b.bookingId ? (
                      <div className="w-3.5 h-3.5 border-2 border-black border-t-transparent rounded-full animate-spin"></div>
                    ) : (
                      <Download className="w-3.5 h-3.5" />
                    )}
                    <span>Download Official PDF Receipt</span>
                  </button>
                ) : (
                  <button
                    onClick={() => {
                      setPaymentBooking(b);
                      setModalCardName(user?.fullName || '');
                      setModalCardNumber('');
                      setModalCardExpiry('');
                      setModalCardCvv('');
                      setPaymentRef('');
                    }}
                    className="px-4 py-2 rounded-xl border border-gold/40 text-gold hover:bg-gold/10 text-xs font-medium flex items-center space-x-1.5"
                  >
                    <CreditCard className="w-3.5 h-3.5" />
                    <span>{b.paymentStatus ? 'Update / Pay Online' : 'Pay Online / Submit Slip'}</span>
                  </button>
                )}

                {/* 2. Photo Gallery Button */}
                <button
                  onClick={() => handleOpenGallery(b)}
                  className="px-4 py-2 rounded-xl border border-gray-700 hover:border-gold/50 text-gray-200 text-xs font-medium flex items-center space-x-1.5 transition-colors"
                >
                  <Camera className="w-3.5 h-3.5 text-gold" />
                  <span>View Event Gallery</span>
                </button>

                {/* 3. Chat Button */}
                {(b.status === 'CONFIRMED' || b.status === 'PAID' || b.status === 'COMPLETED') && (
                  <button
                    onClick={() => setChatBooking(b)}
                    className="px-4 py-2 rounded-xl border border-purple-600/40 bg-purple-950/20 text-purple-400 hover:bg-purple-950/40 text-xs font-medium flex items-center space-x-1.5"
                  >
                    <MessageCircle className="w-3.5 h-3.5" />
                    <span>Chat</span>
                  </button>
                )}

                {/* 4. Edit Booking (if CONFIRMED only) */}
                {b.status === 'CONFIRMED' && (
                  <button
                    onClick={() => handleOpenEditBooking(b)}
                    className="px-4 py-2 rounded-xl border border-blue-600/40 bg-blue-950/20 text-blue-400 hover:bg-blue-950/40 text-xs font-medium flex items-center space-x-1.5"
                  >
                    <Edit3 className="w-3.5 h-3.5" />
                    <span>Edit Details</span>
                  </button>
                )}

                {/* 5. Leave Review / Edit Review (if COMPLETED) */}
                {b.status === 'COMPLETED' && (
                  reviews[b.bookingId] ? (
                    <div className="flex items-center gap-2">
                      <button
                        onClick={() => setEditReview({
                          reviewId: reviews[b.bookingId].reviewId,
                          bookingId: b.bookingId,
                          starRating: reviews[b.bookingId].starRating,
                          reviewComment: reviews[b.bookingId].reviewComment
                        })}
                        className="px-3 py-2 rounded-xl border border-blue-600/40 bg-blue-950/20 text-blue-400 hover:bg-blue-950/40 text-xs font-medium flex items-center space-x-1.5"
                      >
                        <Star className="w-3.5 h-3.5" />
                        <span>Edit Review</span>
                      </button>
                      <button
                        onClick={() => handleDeleteReview(reviews[b.bookingId].reviewId)}
                        disabled={deletingReviewId === reviews[b.bookingId].reviewId}
                        className="px-3 py-2 rounded-xl border border-red-600/30 bg-red-950/10 text-red-400 hover:bg-red-950/30 text-xs font-medium flex items-center space-x-1.5"
                      >
                        <XCircle className="w-3.5 h-3.5" />
                        <span>{deletingReviewId === reviews[b.bookingId].reviewId ? 'Deleting...' : 'Delete Review'}</span>
                      </button>
                    </div>
                  ) : (
                    <button
                      onClick={() => setReviewBooking(b)}
                      className="px-4 py-2 rounded-xl border border-gold/30 bg-gold/5 text-gold hover:bg-gold/15 text-xs font-medium flex items-center space-x-1.5"
                    >
                      <Star className="w-3.5 h-3.5" />
                      <span>Write Testimonial</span>
                    </button>
                  )
                )}

                {/* 6. Cancel (if PENDING) */}
                {b.status === 'PENDING_ADMIN_APPROVAL' && (
                  <button
                    onClick={() => handleCancelBooking(b.bookingId)}
                    className="px-4 py-2 rounded-xl text-xs text-red-400 hover:text-red-300 hover:bg-red-950/20 transition-colors ml-auto"
                  >
                    Cancel Booking
                  </button>
                )}
              </div>

            </div>
          ))}
        </div>
      )}


      {/* ── MODAL: Photo Gallery ─────────────────────────────────── */}
      {galleryBooking && (
        <div className="fixed inset-0 z-50 bg-black/90 backdrop-blur-md flex items-center justify-center p-4">
          <div className="glass-card rounded-3xl p-6 sm:p-8 max-w-4xl w-full max-h-[85vh] flex flex-col border border-gold/40 space-y-6">
            <div className="flex items-center justify-between border-b border-gray-800 pb-4">
              <div>
                <span className="text-xs uppercase font-mono tracking-wider text-gold">
                  Event Gallery · Booking #{galleryBooking.bookingId}
                </span>
                <h3 className="font-serif-title text-xl font-bold text-white">
                  {galleryBooking.packageName}
                </h3>
              </div>
              <div className="flex items-center space-x-3">
                <button
                  onClick={() => setShowFavoritesOnly(f => !f)}
                  className={`flex items-center space-x-1.5 px-3 py-1.5 rounded-xl text-xs font-semibold border transition-all ${
                    showFavoritesOnly
                      ? 'bg-red-950/40 border-red-500/50 text-red-400'
                      : 'border-gray-700 text-gray-400 hover:border-red-500/50 hover:text-red-400'
                  }`}
                >
                  <Heart className={`w-3.5 h-3.5 ${showFavoritesOnly ? 'fill-red-400 text-red-400' : ''}`} />
                  <span>{showFavoritesOnly ? 'Favorites' : 'All Photos'}</span>
                </button>
                <button onClick={() => { setGalleryBooking(null); setShowFavoritesOnly(false); }} className="p-2 text-gray-400 hover:text-white rounded-lg">
                  <X className="w-5 h-5" />
                </button>
              </div>
            </div>

            <div className="overflow-y-auto flex-1 pr-2">
              {loadingPhotos ? (
                <div className="flex justify-center py-12">
                  <div className="w-8 h-8 border-3 border-gold/20 border-t-gold rounded-full animate-spin"></div>
                </div>
              ) : galleryPhotos.length === 0 ? (
                <div className="text-center py-12 space-y-3">
                  <Camera className="w-10 h-10 text-gray-600 mx-auto" />
                  <p className="text-xs text-gray-400">
                    Photos for this celebration have not been uploaded yet. Your photographer will upload them within 48 hours following the event.
                  </p>
                </div>
              ) : (
                <div className="grid grid-cols-2 sm:grid-cols-3 gap-4">
                  {galleryPhotos
                    .filter(photo => !showFavoritesOnly || photo.isFavorite)
                    .map((photo) => (
                    <div
                      key={photo.photoId}
                      className="group relative rounded-xl overflow-hidden border border-gray-800 aspect-[4/3] bg-black"
                    >
                      <img
                        src={getImageUrl(photo.photoUrl)}
                        alt="Celebration Photo"
                        className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-300"
                        onError={(e) => {
                          e.target.src = 'https://images.unsplash.com/photo-1530103862676-de8c9debad1d?auto=format&fit=crop&w=800&q=80';
                        }}
                      />
                      {/* Favorite button — always visible */}
                      <button
                        onClick={() => handleToggleFavorite(photo.photoId)}
                        className={`absolute top-2 right-2 p-1.5 rounded-full border transition-all ${
                          photo.isFavorite
                            ? 'bg-red-950/80 border-red-500/60 text-red-400'
                            : 'bg-black/60 border-gray-700/60 text-gray-400 hover:text-red-400 hover:border-red-500/50'
                        }`}
                        title={photo.isFavorite ? 'Remove from favorites' : 'Add to favorites'}
                      >
                        <Heart className={`w-3.5 h-3.5 ${photo.isFavorite ? 'fill-red-400' : ''}`} />
                      </button>
                      <div className="absolute inset-0 bg-black/60 opacity-0 group-hover:opacity-100 transition-opacity flex items-center justify-center p-2 gap-2">
                        <a
                          href={getImageUrl(photo.photoUrl)}
                          target="_blank"
                          rel="noreferrer"
                          className="px-3 py-1.5 rounded-lg bg-black/80 hover:bg-black text-gold text-xs font-semibold flex items-center space-x-1 border border-gold/40"
                          title="View High Resolution"
                        >
                          <Eye className="w-3.5 h-3.5" />
                          <span>View</span>
                        </a>
                        <button
                          onClick={() => downloadImage(photo.photoUrl, `Pixora-Booking-${galleryBooking.bookingId}-${photo.photoId}.jpg`)}
                          className="px-3 py-1.5 rounded-lg btn-gold text-xs font-semibold flex items-center space-x-1 shadow"
                          title="Direct Download"
                        >
                          <Download className="w-3.5 h-3.5" />
                          <span>Download</span>
                        </button>
                      </div>
                    </div>
                  ))}
                  {showFavoritesOnly && galleryPhotos.filter(p => p.isFavorite).length === 0 && (
                    <div className="col-span-3 text-center py-8 text-xs text-gray-500">
                      No favorites yet. Click the ♥ heart icon on any photo to favorite it.
                    </div>
                  )}
                </div>
              )}
            </div>
          </div>
        </div>
      )}


      {/* ── MODAL: Submit Payment (Card / Bank Transfer) ─────────── */}
      {paymentBooking && (
        <div className="fixed inset-0 z-50 bg-black/90 backdrop-blur-md flex items-center justify-center p-4">
          <div className="glass-card rounded-3xl p-6 sm:p-8 max-w-lg w-full border border-gold/40 space-y-5 max-h-[90vh] overflow-y-auto">
            {/* Header */}
            <div className="flex items-center justify-between border-b border-gray-800 pb-3">
              <h3 className="font-serif-title text-lg font-bold text-white flex items-center gap-2">
                <CreditCard className="w-5 h-5 text-gold" /> Submit Payment
              </h3>
              <button
                onClick={() => setPaymentBooking(null)}
                className="p-1 text-gray-400 hover:text-white"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            {/* Booking summary */}
            <div className="p-3.5 rounded-xl bg-black/40 border border-gray-800 text-xs text-gray-300 space-y-1">
              <p><span className="text-gray-500">Booking:</span> #{paymentBooking.bookingId} — {paymentBooking.packageName}</p>
              <p><span className="text-gray-500">Amount Due:</span> <span className="text-gold font-bold text-sm">{formatLKR(paymentBooking.totalAmountLkr)}</span></p>
            </div>

            {/* Payment method tabs */}
            <div className="flex rounded-xl overflow-hidden border border-gray-700">
              <button
                type="button"
                onClick={() => setModalPaymentMethod('CARD')}
                className={`flex-1 py-2.5 text-xs font-semibold flex items-center justify-center gap-1.5 transition-all ${
                  modalPaymentMethod === 'CARD'
                    ? 'bg-gold text-black'
                    : 'bg-transparent text-gray-400 hover:text-white'
                }`}
              >
                <CreditCard className="w-3.5 h-3.5" /> Credit / Debit Card
              </button>
              <button
                type="button"
                onClick={() => setModalPaymentMethod('BANK_TRANSFER')}
                className={`flex-1 py-2.5 text-xs font-semibold flex items-center justify-center gap-1.5 transition-all border-l border-gray-700 ${
                  modalPaymentMethod === 'BANK_TRANSFER'
                    ? 'bg-gold text-black'
                    : 'bg-transparent text-gray-400 hover:text-white'
                }`}
              >
                <Building className="w-3.5 h-3.5" /> Bank Transfer
              </button>
            </div>

            <form onSubmit={handleSubmitPayment} className="space-y-4">

              {/* ── CARD SECTION ── */}
              {modalPaymentMethod === 'CARD' && (
                <>
                  {/* Virtual card preview */}
                  <div className="relative rounded-2xl p-5 overflow-hidden"
                    style={{ background: 'linear-gradient(135deg, #1a1a1a 0%, #2a2215 50%, #1a1a1a 100%)', border: '1px solid rgba(212,175,55,0.35)' }}>
                    <div className="absolute top-3 right-4 opacity-20 text-gold">
                      <Sparkles className="w-8 h-8" />
                    </div>
                    <p className="text-[10px] font-mono text-gold uppercase tracking-widest mb-4">Pixora Secure Pay</p>
                    <p className="font-mono text-sm text-white tracking-[0.2em] mb-4">
                      {modalCardNumber || '•••• •••• •••• ••••'}
                    </p>
                    <div className="flex justify-between items-end">
                      <div>
                        <p className="text-[9px] text-gray-500 uppercase tracking-wider">Cardholder</p>
                        <p className="text-xs text-white font-semibold truncate max-w-[140px]">
                          {modalCardName || 'YOUR NAME'}
                        </p>
                      </div>
                      <div className="text-right">
                        <p className="text-[9px] text-gray-500 uppercase tracking-wider">Expires</p>
                        <p className="text-xs text-white font-mono">{modalCardExpiry || 'MM/YY'}</p>
                      </div>
                    </div>
                  </div>

                  {/* Cardholder name */}
                  <div>
                    <label className="block text-xs font-semibold text-gray-300 mb-1.5">Cardholder Name *</label>
                    <input
                      type="text"
                      placeholder="As printed on card"
                      value={modalCardName}
                      onChange={(e) => setModalCardName(e.target.value)}
                      className="w-full bg-[#151515] border border-gray-700 rounded-xl px-4 py-3 text-sm text-white focus:outline-none focus:border-gold"
                      required
                    />
                  </div>

                  {/* Card number */}
                  <div>
                    <label className="block text-xs font-semibold text-gray-300 mb-1.5">Card Number *</label>
                    <div className="relative">
                      <input
                        type="text"
                        placeholder="1234 5678 9012 3456"
                        value={modalCardNumber}
                        onChange={handleModalCardNumberChange}
                        maxLength={19}
                        className="w-full bg-[#151515] border border-gray-700 rounded-xl px-4 py-3 pr-12 text-sm text-white focus:outline-none focus:border-gold font-mono"
                        required
                      />
                      <CreditCard className="absolute right-4 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-500" />
                    </div>
                  </div>

                  {/* Expiry + CVV */}
                  <div className="grid grid-cols-2 gap-3">
                    <div>
                      <label className="block text-xs font-semibold text-gray-300 mb-1.5">Expiry Date *</label>
                      <input
                        type="text"
                        placeholder="MM/YY"
                        value={modalCardExpiry}
                        onChange={handleModalCardExpiryChange}
                        maxLength={5}
                        className="w-full bg-[#151515] border border-gray-700 rounded-xl px-4 py-3 text-sm text-white focus:outline-none focus:border-gold font-mono"
                        required
                      />
                    </div>
                    <div>
                      <label className="block text-xs font-semibold text-gray-300 mb-1.5">CVV *</label>
                      <div className="relative">
                        <input
                          type="password"
                          placeholder="•••"
                          value={modalCardCvv}
                          onChange={handleModalCardCvvChange}
                          maxLength={4}
                          className="w-full bg-[#151515] border border-gray-700 rounded-xl px-4 py-3 pr-10 text-sm text-white focus:outline-none focus:border-gold font-mono"
                          required
                        />
                        <Lock className="absolute right-3 top-1/2 -translate-y-1/2 w-3.5 h-3.5 text-gray-500" />
                      </div>
                    </div>
                  </div>

                  <p className="text-[10px] text-gray-500 flex items-center gap-1">
                    <Lock className="w-3 h-3" /> Card details are used for demo only. No real charge will be made.
                  </p>
                </>
              )}

              {/* ── BANK TRANSFER SECTION ── */}
              {modalPaymentMethod === 'BANK_TRANSFER' && (
                <div className="space-y-3">
                  <div className="p-3.5 rounded-xl bg-blue-950/30 border border-blue-800/40 text-xs text-blue-300 space-y-1">
                    <p className="font-semibold text-blue-200">Bank Transfer Instructions</p>
                    <p>Account Name: <span className="text-white">Pixora Photography Ltd.</span></p>
                    <p>Bank: <span className="text-white">Commercial Bank of Ceylon</span></p>
                    <p>Account No: <span className="text-white font-mono">8001-4823-9917</span></p>
                    <p>Branch: <span className="text-white">Colombo 03</span></p>
                  </div>
                  <div>
                    <label className="block text-xs font-semibold text-gray-300 mb-1.5">
                      Bank Transaction Reference / Deposit Slip No. *
                    </label>
                    <input
                      type="text"
                      placeholder="e.g. TXN-984321 or COMB-DEP-1234"
                      value={paymentRef}
                      onChange={(e) => setPaymentRef(e.target.value)}
                      className="w-full bg-[#151515] border border-gray-700 rounded-xl px-4 py-3 text-sm text-white focus:outline-none focus:border-gold font-mono"
                      required
                    />
                  </div>
                </div>
              )}

              <div className="flex justify-end space-x-3 pt-2">
                <button
                  type="button"
                  onClick={() => setPaymentBooking(null)}
                  className="px-4 py-2.5 rounded-xl border border-gray-700 text-xs text-gray-300"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={submittingPayment}
                  className="px-6 py-2.5 rounded-xl btn-gold text-xs font-semibold"
                >
                  {submittingPayment ? 'Processing...' : modalPaymentMethod === 'CARD' ? 'Pay Now' : 'Submit Slip'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}


      {/* ── MODAL: Submit Review ─────────────────────────────────── */}
      {reviewBooking && (
        <div className="fixed inset-0 z-50 bg-black/90 backdrop-blur-md flex items-center justify-center p-4">
          <div className="glass-card rounded-3xl p-6 sm:p-8 max-w-md w-full border border-gold/40 space-y-5">
            <div className="flex items-center justify-between border-b border-gray-800 pb-3">
              <h3 className="font-serif-title text-lg font-bold text-white">
                Review Your Experience
              </h3>
              <button
                onClick={() => setReviewBooking(null)}
                className="p-1 text-gray-400 hover:text-white"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            <form onSubmit={handleSubmitReview} className="space-y-4">
              <div>
                <label className="block text-xs font-semibold text-gray-300 mb-2">
                  Star Rating (1 to 5)
                </label>
                <div className="flex items-center space-x-2">
                  {[1, 2, 3, 4, 5].map((star) => (
                    <button
                      key={star}
                      type="button"
                      onClick={() => setStarRating(star)}
                      className="p-1 text-gold focus:outline-none"
                    >
                      <Star
                        className={`w-6 h-6 ${
                          star <= starRating ? 'fill-gold text-gold' : 'text-gray-600'
                        }`}
                      />
                    </button>
                  ))}
                  <span className="text-xs text-gold font-bold ml-2 font-mono">
                    {starRating}.0 / 5.0
                  </span>
                </div>
              </div>

              <div>
                <label className="block text-xs font-semibold text-gray-300 mb-1.5">
                  Your Feedback <span className="text-gray-500">(3–1000 characters)</span>
                </label>
                <textarea
                  rows={4}
                  placeholder="e.g. Great photos and wonderful celebration memories!"
                  value={reviewComment}
                  onChange={(e) => setReviewComment(e.target.value)}
                  maxLength={1000}
                  className="w-full bg-[#151515] border border-gray-700 rounded-xl p-3 text-xs text-white focus:outline-none focus:border-gold"
                />
                <p className={`text-[10px] mt-1 ${reviewComment.length < 3 || reviewComment.length > 1000 ? 'text-red-400' : 'text-green-400'}`}>
                  {reviewComment.length}/1000 characters
                </p>
              </div>

              <div className="flex justify-end space-x-3 pt-2">
                <button
                  type="button"
                  onClick={() => setReviewBooking(null)}
                  className="px-4 py-2.5 rounded-xl border border-gray-700 text-xs text-gray-300"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={submittingReview}
                  className="px-6 py-2.5 rounded-xl btn-gold text-xs font-semibold"
                >
                  {submittingReview ? 'Submitting...' : 'Post Review'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* ── MODAL: Edit Review ───────────────────────────────────── */}
      {editReview && (
        <div className="fixed inset-0 z-50 bg-black/90 backdrop-blur-md flex items-center justify-center p-4">
          <div className="glass-card rounded-3xl p-6 sm:p-8 max-w-md w-full border border-gold/40 space-y-5">
            <div className="flex items-center justify-between border-b border-gray-800 pb-3">
              <h3 className="font-serif-title text-lg font-bold text-white">Edit Your Review</h3>
              <button onClick={() => setEditReview(null)} className="p-1 text-gray-400 hover:text-white">
                <X className="w-5 h-5" />
              </button>
            </div>
            <form onSubmit={handleSaveEditReview} className="space-y-4">
              <div>
                <label className="block text-xs font-semibold text-gray-300 mb-2">Star Rating</label>
                <div className="flex items-center space-x-2">
                  {[1, 2, 3, 4, 5].map((star) => (
                    <button
                      key={star}
                      type="button"
                      onClick={() => setEditReview({ ...editReview, starRating: star })}
                      className="p-1 text-gold focus:outline-none"
                    >
                      <Star className={`w-6 h-6 ${star <= editReview.starRating ? 'fill-gold text-gold' : 'text-gray-600'}`} />
                    </button>
                  ))}
                  <span className="text-xs text-gold font-bold ml-2 font-mono">{editReview.starRating}.0 / 5.0</span>
                </div>
              </div>
              <div>
                <label className="block text-xs font-semibold text-gray-300 mb-1.5">
                  Feedback <span className="text-gray-500">(3–1000 characters)</span>
                </label>
                <textarea
                  rows={4}
                  placeholder="e.g. Great photos and wonderful celebration memories!"
                  value={editReview.reviewComment}
                  onChange={(e) => setEditReview({ ...editReview, reviewComment: e.target.value })}
                  maxLength={1000}
                  className="w-full bg-[#151515] border border-gray-700 rounded-xl p-3 text-xs text-white focus:outline-none focus:border-gold"
                />
                <p className={`text-[10px] mt-1 ${editReview.reviewComment.length < 3 || editReview.reviewComment.length > 1000 ? 'text-red-400' : 'text-green-400'}`}>
                  {editReview.reviewComment.length}/1000 characters
                </p>
              </div>
              <div className="flex justify-end space-x-3 pt-2">
                <button type="button" onClick={() => setEditReview(null)} className="px-4 py-2.5 rounded-xl border border-gray-700 text-xs text-gray-300">
                  Cancel
                </button>
                <button type="submit" className="px-6 py-2.5 rounded-xl btn-gold text-xs font-semibold">
                  Save Changes
                </button>
              </div>
            </form>
          </div>
        </div>
      )}


      {/* ── MODAL: Edit Booking ───────────────────────────────────── */}
      {editBookingModal && (
        <div className="fixed inset-0 z-50 bg-black/90 backdrop-blur-md flex items-center justify-center p-4">
          <div className="glass-card rounded-3xl p-6 sm:p-8 max-w-md w-full border border-blue-500/30 space-y-5">
            <div className="flex items-center justify-between border-b border-gray-800 pb-3">
              <div>
                <div className="flex items-center space-x-2 mb-0.5">
                  <Edit3 className="w-4 h-4 text-blue-400" />
                  <span className="text-xs font-mono text-blue-400 uppercase tracking-wider">Edit Booking</span>
                </div>
                <h3 className="font-serif-title text-base font-bold text-white">
                  Booking #{editBookingModal.bookingId}
                </h3>
              </div>
              <button onClick={() => setEditBookingModal(null)} className="p-1 text-gray-400 hover:text-white">
                <X className="w-5 h-5" />
              </button>
            </div>
            <div className="p-3 rounded-xl bg-blue-950/30 border border-blue-800/40 text-xs text-blue-300">
              ℹ️ Editing event details will require your photographer to re-accept the booking.
            </div>
            <form onSubmit={handleSaveEditBooking} className="space-y-4">
              <div>
                <label className="block text-xs font-semibold text-gray-300 mb-1.5">Event Date *</label>
                <input type="date" value={editEventDate} onChange={e => setEditEventDate(e.target.value)}
                  className="w-full bg-[#151515] border border-gray-700 rounded-xl px-4 py-2.5 text-sm text-white focus:outline-none focus:border-gold" required />
              </div>
              <div>
                <label className="block text-xs font-semibold text-gray-300 mb-1.5">Event Time *</label>
                <input type="time" value={editEventTime} onChange={e => setEditEventTime(e.target.value)}
                  className="w-full bg-[#151515] border border-gray-700 rounded-xl px-4 py-2.5 text-sm text-white focus:outline-none focus:border-gold" required />
              </div>
              <div>
                <label className="block text-xs font-semibold text-gray-300 mb-1.5">Venue Address *</label>
                <input type="text" value={editVenueAddress} onChange={e => setEditVenueAddress(e.target.value)}
                  className="w-full bg-[#151515] border border-gray-700 rounded-xl px-4 py-2.5 text-sm text-white focus:outline-none focus:border-gold" required />
              </div>
              <div>
                <label className="block text-xs font-semibold text-gray-300 mb-1.5">Notes to Photographer</label>
                <textarea rows={2} value={editClientNotes} onChange={e => setEditClientNotes(e.target.value)}
                  className="w-full bg-[#151515] border border-gray-700 rounded-xl px-4 py-2.5 text-xs text-white focus:outline-none focus:border-gold"
                  placeholder="Any special requests or updates..." />
              </div>
              <div className="flex justify-end space-x-3 pt-2">
                <button type="button" onClick={() => setEditBookingModal(null)} className="px-4 py-2.5 rounded-xl border border-gray-700 text-xs text-gray-300">Cancel</button>
                <button type="submit" disabled={savingEdit} className="px-6 py-2.5 rounded-xl btn-gold text-xs font-semibold disabled:opacity-50">
                  {savingEdit ? 'Saving...' : 'Save Changes'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* ── MODAL: Booking Chat ───────────────────────────────────── */}
      {chatBooking && (
        <BookingChatModal booking={chatBooking} onClose={() => setChatBooking(null)} />
      )}

    </div>
  );
};

export default ClientDashboard;
