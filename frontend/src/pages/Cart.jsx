import React, { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useCart } from '../context/CartContext';
import { useAuth } from '../context/AuthContext';
import api from '../api/axios';
import { 
  ShoppingBag, 
  Trash2, 
  Calendar, 
  Clock, 
  MapPin, 
  User, 
  ArrowRight, 
  CheckCircle2, 
  Sparkles,
  AlertCircle
} from 'lucide-react';
import { toast } from 'react-toastify';

// Cart and booking configuration
const Cart = () => {
  const {
    selectedPackage,
    bookingDetails,
    updateBookingDetails,
    clearCart,
    totalAmount
  } = useCart();

  const { isAuthenticated } = useAuth();
  const navigate = useNavigate();

  // Photographer data and loading state
  const [availablePhotographers, setAvailablePhotographers] = useState([]);
  const [loadingPhotographers, setLoadingPhotographers] = useState(false);
  const [dateError, setDateError] = useState('');

  // Minimum date is today
  const todayStr = new Date().toISOString().split('T')[0];

  // Fetch photographers when date changes
  useEffect(() => {
    if (!bookingDetails.eventDate) return;

    const fetchAvailable = async () => {
      setLoadingPhotographers(true);
      setDateError('');

      try {
        // Get available photographers
        const res = await api.get(
          `/api/client/photographers/available?date=${bookingDetails.eventDate}`
        );

        setAvailablePhotographers(res.data);
      } catch (err) {
        console.warn(
          'Could not fetch available photographers',
          err
        );

        // Try public photographer list
        try {
          const publicRes = await api.get('/api/public/photographers');
          setAvailablePhotographers(publicRes.data);
        } catch (e) {
          setAvailablePhotographers([]);
        }
      } finally {
        setLoadingPhotographers(false);
      }
    };

    fetchAvailable();
  }, [bookingDetails.eventDate]);

  // Handle date selection
  const handleDateChange = (e) => {
    const newDate = e.target.value;

    // Reset photographer when date changes
    updateBookingDetails({
      eventDate: newDate,
      photographerId: null,
      photographerName: ''
    });
  };

  // Validate and proceed to checkout
  const handleProceedToCheckout = () => {

    // Check package selection
    if (!selectedPackage) {
      toast.error('Please select a birthday photography package first');
      return;
    }

    // Check event date
    if (!bookingDetails.eventDate) {
      toast.warning('Please choose your event date');
      return;
    }

    // Check event time
    if (!bookingDetails.eventTime) {
      toast.warning('Please choose your event time');
      return;
    }

    // Check venue address
    if (
      !bookingDetails.venueAddress ||
      bookingDetails.venueAddress.trim().length < 5
    ) {
      toast.warning(
        'Please enter the celebration venue address (at least 5 characters)'
      );
      return;
    }

    // Login required for checkout
    if (!isAuthenticated) {
      toast.info(
        'Please sign in or create an account to complete your reservation'
      );

      navigate('/login', {
        state: {
          from: { pathname: '/checkout' }
        }
      });

      return;
    }

    // Go to checkout
    navigate('/checkout');
  };

  // Format amount as LKR
  const formatLKR = (amount) => {
    return new Intl.NumberFormat('en-LK', {
      style: 'currency',
      currency: 'LKR',
      maximumFractionDigits: 0
    })
      .format(amount)
      .replace('LKR', 'Rs.');
  };

  // Show empty cart
  if (!selectedPackage) {
    return (
      <div className="max-w-4xl mx-auto px-4 py-24 text-center space-y-6">
        <div className="w-20 h-20 rounded-full border border-gold/30 bg-gold/5 flex items-center justify-center text-gold mx-auto">
          <ShoppingBag className="w-10 h-10" />
        </div>

        <h2 className="font-serif-title text-3xl font-bold text-white">
          Your Cart is Empty
        </h2>

        <p className="text-sm text-gray-400 max-w-md mx-auto">
          You have not selected a photography package yet.
          Explore our curated collections for kids and milestone
          birthday celebrations.
        </p>

        <Link
          to="/packages"
          className="inline-flex items-center space-x-2 px-8 py-3.5 rounded-xl btn-gold text-sm font-semibold shadow-lg shadow-gold/20"
        >
          <span>Explore Birthday Packages</span>
          <ArrowRight className="w-4 h-4" />
        </Link>
      </div>
    );
  }

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-14 space-y-10">

      {/* Page title */}
      <div className="border-b border-gray-800 pb-6 flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="font-serif-title text-3xl sm:text-4xl font-bold text-white">
            Your Booking Reservation
          </h1>

          <p className="text-xs sm:text-sm text-gray-400 mt-1">
            Configure your celebration date, time, venue, and preferred photographer.
          </p>
        </div>

        {/* Remove package */}
        <button
          onClick={clearCart}
          className="text-xs text-red-400 hover:text-red-300 flex items-center space-x-1.5 self-start sm:self-auto"
        >
          <Trash2 className="w-4 h-4" />
          <span>Remove Package</span>
        </button>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-10">

        {/* Package and event details */}
        <div className="lg:col-span-2 space-y-8">

          {/* Selected package */}
          <div className="glass-card rounded-2xl p-6 sm:p-8 border border-gold/40 relative overflow-hidden">
            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
              <div>
                <span className="text-[11px] uppercase tracking-widest text-gold font-mono block mb-1">
                  Selected Birthday Package
                </span>

                <h3 className="font-serif-title text-2xl font-bold text-white">
                  {selectedPackage.packageName}
                </h3>

                <p className="text-xs text-gray-400 mt-2 max-w-xl leading-relaxed">
                  {selectedPackage.description}
                </p>
              </div>

              {/* Package price */}
              <div className="text-left sm:text-right shrink-0">
                <span className="text-xs text-gray-400 block">
                  Package Price
                </span>

                <span className="font-serif-title text-2xl sm:text-3xl font-bold text-gold">
                  {formatLKR(selectedPackage.priceLkr)}
                </span>
              </div>
            </div>

            {/* Package features */}
            <div className="mt-6 pt-5 border-t border-gray-800 grid grid-cols-2 sm:grid-cols-3 gap-3 text-xs text-gray-300">
              <div className="flex items-center space-x-2">
                <CheckCircle2 className="w-4 h-4 text-gold shrink-0" />
                <span>High-Res Digital Edits</span>
              </div>

              <div className="flex items-center space-x-2">
                <CheckCircle2 className="w-4 h-4 text-gold shrink-0" />
                <span>Online Client Gallery</span>
              </div>

              <div className="flex items-center space-x-2">
                <CheckCircle2 className="w-4 h-4 text-gold shrink-0" />
                <span>Official PDF Receipt</span>
              </div>
            </div>
          </div>

          {/* Event details */}
          <div className="glass-card rounded-2xl p-6 sm:p-8 border border-gray-800 space-y-6">

            <h3 className="font-serif-title text-xl font-bold text-white flex items-center space-x-2">
              <Sparkles className="w-5 h-5 text-gold" />
              <span>Event Details & Logistics</span>
            </h3>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-6">

              {/* Event date */}
              <div>
                <label className="block text-xs font-semibold text-gray-300 mb-2 flex items-center space-x-1.5">
                  <Calendar className="w-3.5 h-3.5 text-gold" />
                  <span>Celebration Date *</span>
                </label>

                <input
                  type="date"
                  min={todayStr}
                  value={bookingDetails.eventDate || ''}
                  onChange={handleDateChange}
                  className="w-full bg-[#151515] border border-gray-700 rounded-xl px-4 py-3 text-sm text-white focus:outline-none focus:border-gold transition-colors"
                  required
                />

                <p className="text-[11px] text-gray-500 mt-1">
                  Select the day of the birthday party
                </p>
              </div>

              {/* Event time */}
              <div>
                <label className="block text-xs font-semibold text-gray-300 mb-2 flex items-center space-x-1.5">
                  <Clock className="w-3.5 h-3.5 text-gold" />
                  <span>Event Start Time *</span>
                </label>

                <input
                  type="time"
                  value={bookingDetails.eventTime || '14:00'}
                  onChange={(e) =>
                    updateBookingDetails({ eventTime: e.target.value })
                  }
                  className="w-full bg-[#151515] border border-gray-700 rounded-xl px-4 py-3 text-sm text-white focus:outline-none focus:border-gold transition-colors"
                  required
                />

                <p className="text-[11px] text-gray-500 mt-1">
                  Photographer arrives 15 mins prior
                </p>
              </div>
            </div>

            {/* Venue address */}
            <div>
              <label className="block text-xs font-semibold text-gray-300 mb-2 flex items-center space-x-1.5">
                <MapPin className="w-3.5 h-3.5 text-gold" />
                <span>Celebration Venue Address *</span>
              </label>

              <textarea
                rows={3}
                placeholder="e.g. Shangri-La Hotel Colombo / No. 45, Lotus Road, Colombo 03"
                value={bookingDetails.venueAddress || ''}
                onChange={(e) =>
                  updateBookingDetails({
                    venueAddress: e.target.value
                  })
                }
                className="w-full bg-[#151515] border border-gray-700 rounded-xl p-4 text-sm text-white focus:outline-none focus:border-gold transition-colors"
                required
              />

              <p className="text-[11px] text-gray-500 mt-1">
                Include hotel name, hall number, or home address
              </p>
            </div>

            {/* Photographer selection */}
            <div className="pt-2 border-t border-gray-800">

              <label className="block text-xs font-semibold text-gray-300 mb-2 flex items-center space-x-1.5">
                <User className="w-3.5 h-3.5 text-gold" />
                <span>Preferred Photographer (Optional)</span>
              </label>

              {!bookingDetails.eventDate ? (

                // No date selected
                <div className="p-4 rounded-xl bg-gray-900/60 border border-gray-800 text-xs text-gray-400 flex items-center space-x-2">
                  <AlertCircle className="w-4 h-4 text-gold shrink-0" />
                  <span>
                    Choose an event date above to view available photographers,
                    or let Pixora assign a certified pro.
                  </span>
                </div>

              ) : loadingPhotographers ? (

                // Loading photographers
                <div className="p-4 text-xs text-gold flex items-center space-x-2">
                  <div className="w-4 h-4 border-2 border-gold border-t-transparent rounded-full animate-spin"></div>
                  <span>
                    Checking photographer availability for {bookingDetails.eventDate}...
                  </span>
                </div>

              ) : (

                // Photographer list
                <div className="space-y-3">

                  <select
                    value={bookingDetails.photographerId || ''}
                    onChange={(e) => {
                      const id = e.target.value
                        ? Number(e.target.value)
                        : null;

                      const found = availablePhotographers.find(
                        (p) => p.userId === id
                      );

                      updateBookingDetails({
                        photographerId: id,
                        photographerName: found ? found.fullName : ''
                      });
                    }}
                    className="w-full bg-[#151515] border border-gray-700 rounded-xl px-4 py-3 text-sm text-white focus:outline-none focus:border-gold"
                  >
                    <option value="">
                      Auto-Assign Top Available Photographer (Recommended)
                    </option>

                    {availablePhotographers.map((p) => (
                      <option key={p.userId} value={p.userId}>
                        {p.fullName} (Available on {bookingDetails.eventDate})
                      </option>
                    ))}
                  </select>

                  {/* Availability message */}
                  {availablePhotographers.length > 0 ? (
                    <p className="text-[11px] text-green-400">
                      ✓ {availablePhotographers.length} verified photographer(s)
                      available on your selected date.
                    </p>
                  ) : (
                    <p className="text-[11px] text-gold/80">
                      ℹ All specific photographers are booked or unassigned;
                      Pixora administration will review and confirm your slot.
                    </p>
                  )}
                </div>
              )}
            </div>
          </div>
        </div>

        {/* Order summary */}
        <div className="space-y-6">
          <div className="glass-card rounded-2xl p-6 sm:p-8 border border-gold/30 space-y-6 sticky top-28">

            <h3 className="font-serif-title text-xl font-bold text-white border-b border-gray-800 pb-4">
              Order Summary
            </h3>

            <div className="space-y-3 text-xs">

              {/* Package price */}
              <div className="flex justify-between text-gray-300">
                <span>{selectedPackage.packageName}</span>
                <span className="font-mono font-medium">
                  {formatLKR(selectedPackage.priceLkr)}
                </span>
              </div>

              <div className="flex justify-between text-gray-300">
                <span>Taxes & Service Charge</span>
                <span className="text-green-400 font-mono">
                  Rs. 0.00 (Included)
                </span>
              </div>

              <div className="flex justify-between text-gray-300">
                <span>Official PDF Invoice</span>
                <span className="text-gold">Free</span>
              </div>

              {/* Total */}
              <div className="pt-3 border-t border-gray-800 flex justify-between items-baseline">
                <span className="font-serif-title text-base font-bold text-white">
                  Total Amount
                </span>

                <span className="font-serif-title text-2xl font-bold text-gold">
                  {formatLKR(totalAmount)}
                </span>
              </div>
            </div>

            {/* Booking information */}
            <div className="p-3.5 rounded-xl bg-black/40 border border-gray-800 text-[11px] text-gray-400 space-y-1.5">
              <p className="font-semibold text-gray-300">
                ✓ Booking Assurance:
              </p>
              <p>1. Instant reservation hold upon checkout</p>
              <p>2. Direct bank slip / transfer verification</p>
              <p>3. Official downloadable PDF receipt</p>
            </div>

            {/* Checkout button */}
            <button
              onClick={handleProceedToCheckout}
              className="w-full py-3.5 rounded-xl btn-gold text-sm font-semibold shadow-lg shadow-gold/20 flex items-center justify-center space-x-2"
            >
              <span>Proceed to Checkout</span>
              <ArrowRight className="w-4 h-4" />
            </button>

            {/* Back to packages */}
            <Link
              to="/packages"
              className="block text-center text-xs text-gray-400 hover:text-gold transition-colors"
            >
              Change or Compare Packages
            </Link>
          </div>
        </div>
      </div>
    </div>
  );
};

export default Cart;