import React, { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useCart } from '../context/CartContext';
import { useAuth } from '../context/AuthContext';
import api from '../api/axios';
import { 
  ShieldCheck, 
  CreditCard, 
  Building, 
  Calendar, 
  Clock, 
  MapPin, 
  User, 
  ArrowLeft,
  Lock,
  Sparkles,
  X,
  Tag,
  Check,
  Truck,
  HardDrive
} from 'lucide-react';
import { toast } from 'react-toastify';

const ADDON_OPTIONS = [
  { id: 'drone', label: 'Drone Aerial 4K Coverage', price: 8000, desc: 'Cinematic aerial video & high-res overhead angles' },
  { id: 'polaroid', label: 'Instant Polaroid Souvenirs (30 Prints)', price: 5000, desc: 'Tangible vintage photo prints gifted to guests on-site' },
  { id: 'photobook', label: 'Hardcover Gold-Embossed Photobook', price: 12000, desc: '30-page luxury archival heirloom photo album' },
  { id: 'extra_hour', label: 'Extra Coverage Hour (+1 Hour)', price: 6000, desc: 'Extended celebration coverage for after-party or dinner' },
];

const DELIVERY_TIERS = [
  { id: 'STANDARD', label: 'Standard Delivery', fee: 0, time: '7 Business Days', vault: '3 Months Cloud Storage' },
  { id: 'EXPRESS', label: 'Express Delivery', fee: 5000, time: '48 Hours Rush Turnaround', vault: '1 Year Cloud Vault' },
  { id: 'VIP', label: 'Ultra VIP Priority', fee: 10000, time: '24 Hours Instant Delivery', vault: 'Lifetime Cloud Vault + Custom USB Drive' },
];

const Checkout = () => {
  const { selectedPackage, bookingDetails, clearCart } = useCart();
  const { user } = useAuth();
  const navigate = useNavigate();

  // Add-ons & delivery state
  const [selectedAddons, setSelectedAddons] = useState([]);
  const [selectedTier, setSelectedTier] = useState('STANDARD');

  // Promo code state
  const [promoInput, setPromoInput] = useState('');
  const [appliedPromo, setAppliedPromo] = useState(null);
  const [validatingPromo, setValidatingPromo] = useState(false);

  // Payment state
  const [paymentMethod, setPaymentMethod] = useState('CARD'); // 'CARD' | 'BANK_TRANSFER'
  const [cardholderName, setCardholderName] = useState(user?.fullName || '');
  const [cardNumber, setCardNumber] = useState('');
  const [expiryDate, setExpiryDate] = useState('');
  const [cvv, setCvv] = useState('');
  const [transactionRef, setTransactionRef] = useState('');
  const [agreeTerms, setAgreeTerms] = useState(false);
  const [submitting, setSubmitting] = useState(false);

  if (!selectedPackage || !bookingDetails.eventDate) {
    return (
      <div className="max-w-xl mx-auto px-4 py-24 text-center space-y-4">
        <p className="text-gray-400 text-sm">No active booking to checkout.</p>
        <Link to="/cart" className="inline-block px-6 py-2.5 rounded-xl btn-gold text-xs font-semibold">
          Return to Cart
        </Link>
      </div>
    );
  }

  const formatLKR = (amount) => {
    return new Intl.NumberFormat('en-LK', {
      style: 'currency',
      currency: 'LKR',
      maximumFractionDigits: 0
    }).format(amount).replace('LKR', 'Rs.');
  };

  // Price calculations
  const basePackagePrice = Number(selectedPackage.priceLkr) || 0;
  const addonsTotal = selectedAddons.reduce((sum, addonId) => {
    const item = ADDON_OPTIONS.find(a => a.id === addonId);
    return sum + (item ? item.price : 0);
  }, 0);
  const tierItem = DELIVERY_TIERS.find(t => t.id === selectedTier) || DELIVERY_TIERS[0];
  const deliveryFee = tierItem.fee;
  const subtotal = basePackagePrice + addonsTotal + deliveryFee;
  const discountAmount = appliedPromo ? Number(appliedPromo.discountAmount) || 0 : 0;
  const totalAmount = Math.max(0, subtotal - discountAmount);

  const toggleAddon = (id) => {
    setSelectedAddons(prev => 
      prev.includes(id) ? prev.filter(x => x !== id) : [...prev, id]
    );
  };

  const handleValidatePromo = async (e) => {
    e.preventDefault();
    if (!promoInput.trim()) {
      toast.warning('Please enter a promo code');
      return;
    }
    setValidatingPromo(true);
    try {
      const res = await api.post('/api/public/promo/validate', {
        code: promoInput.trim().toUpperCase(),
        bookingAmount: subtotal
      });
      const data = res.data;
      if (data.valid) {
        const discountVal = data.discountAmountLkr || data.discountAmount || 0;
        setAppliedPromo({
          code: data.code || promoInput.trim().toUpperCase(),
          discountAmount: discountVal,
          discountPercent: data.discountPercent
        });
        toast.success(data.message || `Promo code applied! Saved ${formatLKR(discountVal)}`);
      } else {
        toast.error(data.message || 'Invalid or expired promo code');
      }
    } catch (err) {
      toast.error('Failed to validate promo code');
    } finally {
      setValidatingPromo(false);
    }
  };

  const handleRemovePromo = () => {
    setAppliedPromo(null);
    setPromoInput('');
  };

  // Card input formatters
  const handleCardNumberChange = (e) => {
    const raw = e.target.value.replace(/\D/g, '').slice(0, 16);
    setCardNumber(raw.replace(/(\d{4})(?=\d)/g, '$1 '));
  };

  const handleExpiryChange = (e) => {
    const raw = e.target.value.replace(/\D/g, '').slice(0, 4);
    if (raw.length >= 3) {
      setExpiryDate(`${raw.slice(0, 2)}/${raw.slice(2)}`);
    } else {
      setExpiryDate(raw);
    }
  };

  const handleCvvChange = (e) => {
    const raw = e.target.value.replace(/\D/g, '').slice(0, 4);
    setCvv(raw);
  };
  //Check the date validation 
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

  const handlePlaceBooking = async (e) => {
    e.preventDefault();

    if (paymentMethod === 'CARD') {
      if (!cardholderName.trim()) {
        toast.warning('Please enter the Cardholder Name');
        return;
      }
      //Check wheather card must have the 16 digits 
      const rawCard = cardNumber.replace(/\s/g, '');
      if (rawCard.length !== 16) {
        toast.warning('Please enter a valid 16-digit Card Number');
        return;
      }
      if (!validateExpiry(expiryDate)) {
        toast.warning('Please enter a valid future Expiry Date (MM/YY)');
        return;
      }
      //check the cvv has exactly 4 digits 
      if (!/^\d{3,4}$/.test(cvv)) {
        toast.warning('Please enter a valid 3 or 4-digit CVV security code');
        return;
      }
    } else {
      if (!transactionRef.trim()) {
        toast.warning('Please enter your Bank Transfer Reference / Deposit Slip number');
        return;
      }
    }

    if (!agreeTerms) {
      toast.warning('Please agree to Pixora booking conditions');
      return;
    }

    setSubmitting(true);
    try {
      // Step 1: Create booking with add-ons, delivery tier, and promo discount
      const addonNames = selectedAddons
        .map(id => ADDON_OPTIONS.find(a => a.id === id)?.label)
        .filter(Boolean)
        .join(', ');

      const bookingPayload = {
        packageId: selectedPackage.packageId,
        photographerId: bookingDetails.photographerId || null,
        eventDate: bookingDetails.eventDate,
        eventTime: bookingDetails.eventTime.length === 5 ? `${bookingDetails.eventTime}:00` : bookingDetails.eventTime,
        venueAddress: bookingDetails.venueAddress,
        addons: addonNames,
        deliveryTier: selectedTier,
        deliveryFeeLkr: deliveryFee,
        discountAmountLkr: discountAmount,
        promoCode: appliedPromo ? appliedPromo.code : null,
      };

      const bookingRes = await api.post('/api/client/bookings', bookingPayload);
      const createdBooking = bookingRes.data;

      // Step 2: Submit payment details with calculated total
      if (paymentMethod === 'CARD') {
        const rawCard = cardNumber.replace(/\s/g, '');
        const paymentPayload = {
          paymentMethod: 'CARD',
          cardholderName: cardholderName.trim(),
          cardNumber: rawCard,
          expiryDate: expiryDate.trim(),
          cvv: cvv.trim(),
          amountPaidLkr: totalAmount,
        };
        await api.post(`/api/client/bookings/${createdBooking.bookingId}/payment`, paymentPayload);
        toast.success('💳 Payment successful! Booking status updated to PAID.');
      } else {
        const paymentPayload = {
          paymentMethod: 'BANK_TRANSFER',
          transactionRef: transactionRef.trim(),
          amountPaidLkr: totalAmount,
        };
        await api.post(`/api/client/bookings/${createdBooking.bookingId}/payment`, paymentPayload);
        toast.success('🎉 Booking successfully placed! Admin will verify your payment shortly.');
      }

      clearCart();
      navigate('/client', { state: { newBookingId: createdBooking.bookingId } });

    } catch (err) {
      console.error('Booking submission failed', err);
      const errMsg = err.response?.data?.error || err.message || 'Failed to complete booking. Please try again.';
      toast.error(errMsg);
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-14 space-y-10">
      
      {/* Back Link */}
      <div>
        <Link
          to="/cart"
          className="inline-flex items-center space-x-2 text-xs text-gray-400 hover:text-gold transition-colors"
        >
          <ArrowLeft className="w-4 h-4" />
          <span>Back to Cart Configuration</span>
        </Link>
      </div>

      <div className="border-b border-gray-800 pb-4">
        <h1 className="font-serif-title text-3xl sm:text-4xl font-bold text-white">
          Complete Your Reservation
        </h1>
        <p className="text-xs sm:text-sm text-gray-400 mt-1">
          Customize event add-ons, choose your delivery tier, apply promo codes, and submit secure payment.
        </p>
      </div>

      <form onSubmit={handlePlaceBooking} className="grid grid-cols-1 lg:grid-cols-3 gap-10">
        
        {/* Left 2 Columns */}
        <div className="lg:col-span-2 space-y-8">
          
          {/* Reservation Summary Card */}
          <div className="glass-card rounded-2xl p-6 sm:p-8 border border-gray-800 space-y-5">
            <h3 className="font-serif-title text-lg font-bold text-white flex items-center space-x-2">
              <Calendar className="w-4 h-4 text-gold" />
              <span>Event Summary</span>
            </h3>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 text-xs">
              <div className="p-4 rounded-xl bg-black/40 border border-gray-800/80 space-y-1">
                <span className="text-gray-500 uppercase font-mono tracking-wider">Package</span>
                <p className="font-semibold text-white text-sm">{selectedPackage.name || selectedPackage.packageName}</p>
                <p className="text-gold font-bold">{formatLKR(selectedPackage.priceLkr)}</p>
              </div>

              <div className="p-4 rounded-xl bg-black/40 border border-gray-800/80 space-y-1">
                <span className="text-gray-500 uppercase font-mono tracking-wider">Celebration Date & Time</span>
                <p className="font-semibold text-white text-sm">
                  {bookingDetails.eventDate} at {bookingDetails.eventTime}
                </p>
                <p className="text-gray-400">Photographer arrival: 15 mins prior</p>
              </div>

              <div className="p-4 rounded-xl bg-black/40 border border-gray-800/80 space-y-1 sm:col-span-2">
                <span className="text-gray-500 uppercase font-mono tracking-wider">Celebration Venue</span>
                <p className="font-semibold text-white">{bookingDetails.venueAddress}</p>
              </div>

              <div className="p-4 rounded-xl bg-black/40 border border-gray-800/80 space-y-1 sm:col-span-2">
                <span className="text-gray-500 uppercase font-mono tracking-wider">Assigned Photographer</span>
                <p className="font-semibold text-white">
                  {bookingDetails.photographerName || 'Pixora Auto-Assigned Master Photographer'}
                </p>
              </div>
            </div>
          </div>

          {/* ── Feature 8: Add-ons Selection ──────────────────────── */}
          <div className="glass-card rounded-2xl p-6 sm:p-8 border border-gold/30 space-y-5">
            <div>
              <h3 className="font-serif-title text-lg font-bold text-white flex items-center space-x-2">
                <Sparkles className="w-4 h-4 text-gold" />
                <span>Luxury Celebration Add-ons</span>
              </h3>
              <p className="text-xs text-gray-400 mt-0.5">
                Elevate your birthday photography experience with specialized inclusions.
              </p>
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3.5">
              {ADDON_OPTIONS.map((addon) => {
                const isSelected = selectedAddons.includes(addon.id);
                return (
                  <div
                    key={addon.id}
                    onClick={() => toggleAddon(addon.id)}
                    className={`p-4 rounded-xl border cursor-pointer transition-all flex items-start space-x-3 ${
                      isSelected
                        ? 'border-gold bg-gold/10 shadow-[0_0_15px_rgba(212,175,55,0.15)]'
                        : 'border-gray-800 bg-black/40 hover:border-gray-700'
                    }`}
                  >
                    <div className={`w-5 h-5 rounded-md border flex items-center justify-center shrink-0 mt-0.5 transition-all ${
                      isSelected ? 'bg-gold border-gold text-black' : 'border-gray-600 bg-transparent'
                    }`}>
                      {isSelected && <Check className="w-3.5 h-3.5 stroke-[3]" />}
                    </div>
                    <div className="flex-1 min-w-0">
                      <div className="flex items-center justify-between">
                        <p className={`text-xs font-semibold ${isSelected ? 'text-white' : 'text-gray-200'}`}>
                          {addon.label}
                        </p>
                        <span className="text-xs font-mono font-bold text-gold shrink-0 ml-2">
                          +{formatLKR(addon.price)}
                        </span>
                      </div>
                      <p className="text-[11px] text-gray-400 mt-1 leading-snug">{addon.desc}</p>
                    </div>
                  </div>
                );
              })}
            </div>
          </div>

          {/* ── Feature 8: Delivery & Cloud Storage Tiers ───────────── */}
          <div className="glass-card rounded-2xl p-6 sm:p-8 border border-gold/30 space-y-5">
            <div>
              <h3 className="font-serif-title text-lg font-bold text-white flex items-center space-x-2">
                <Truck className="w-4 h-4 text-gold" />
                <span>Delivery & Cloud Vault Tiers</span>
              </h3>
              <p className="text-xs text-gray-400 mt-0.5">
                Choose the turnaround timeframe and cloud storage duration for your celebration gallery.
              </p>
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-3 gap-3.5">
              {DELIVERY_TIERS.map((tier) => {
                const isSelected = selectedTier === tier.id;
                return (
                  <div
                    key={tier.id}
                    onClick={() => setSelectedTier(tier.id)}
                    className={`p-4 rounded-xl border cursor-pointer transition-all flex flex-col justify-between space-y-3 ${
                      isSelected
                        ? 'border-gold bg-gold/10 shadow-[0_0_15px_rgba(212,175,55,0.15)]'
                        : 'border-gray-800 bg-black/40 hover:border-gray-700'
                    }`}
                  >
                    <div>
                      <div className="flex items-center justify-between mb-1.5">
                        <span className={`text-xs font-bold uppercase font-mono tracking-wider ${isSelected ? 'text-gold' : 'text-gray-300'}`}>
                          {tier.label}
                        </span>
                        <div className={`w-4 h-4 rounded-full border flex items-center justify-center ${
                          isSelected ? 'border-gold bg-gold' : 'border-gray-600'
                        }`}>
                          {isSelected && <div className="w-1.5 h-1.5 rounded-full bg-black" />}
                        </div>
                      </div>
                      <p className="text-sm font-bold text-white font-mono">
                        {tier.fee === 0 ? 'FREE' : `+${formatLKR(tier.fee)}`}
                      </p>
                    </div>

                    <div className="pt-2 border-t border-gray-800/60 space-y-1 text-[11px] text-gray-400">
                      <p className="flex items-center space-x-1.5">
                        <Clock className="w-3 h-3 text-gold shrink-0" />
                        <span>{tier.time}</span>
                      </p>
                      <p className="flex items-center space-x-1.5">
                        <HardDrive className="w-3 h-3 text-gold shrink-0" />
                        <span>{tier.vault}</span>
                      </p>
                    </div>
                  </div>
                );
              })}
            </div>
          </div>

          {/* Payment Method Selector & Details */}
          <div className="glass-card rounded-2xl p-6 sm:p-8 border border-gold/40 space-y-6">
            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 border-b border-gray-800 pb-4">
              <div>
                <h3 className="font-serif-title text-xl font-bold text-white flex items-center space-x-2">
                  <CreditCard className="w-5 h-5 text-gold" />
                  <span>Payment Method</span>
                </h3>
                <p className="text-xs text-gray-400 mt-0.5">Select credit/debit card or direct bank transfer</p>
              </div>
              
              <div className="flex p-1 bg-black/60 rounded-xl border border-gray-800">
                <button
                  type="button"
                  onClick={() => setPaymentMethod('CARD')}
                  className={`px-3 py-1.5 rounded-lg text-xs font-semibold transition-all flex items-center space-x-1.5 ${
                    paymentMethod === 'CARD'
                      ? 'btn-gold text-black shadow-md'
                      : 'text-gray-400 hover:text-white'
                  }`}
                >
                  <CreditCard className="w-3.5 h-3.5" />
                  <span>Credit / Debit Card</span>
                </button>
                <button
                  type="button"
                  onClick={() => setPaymentMethod('BANK_TRANSFER')}
                  className={`px-3 py-1.5 rounded-lg text-xs font-semibold transition-all flex items-center space-x-1.5 ${
                    paymentMethod === 'BANK_TRANSFER'
                      ? 'btn-gold text-black shadow-md'
                      : 'text-gray-400 hover:text-white'
                  }`}
                >
                  <Building className="w-3.5 h-3.5" />
                  <span>Bank Transfer</span>
                </button>
              </div>
            </div>

            {/* CARD PAYMENT */}
            {paymentMethod === 'CARD' && (
              <div className="space-y-6">
                {/* Virtual Card Preview */}
                <div className="relative mx-auto max-w-sm rounded-2xl p-6 bg-gradient-to-tr from-[#121214] via-[#1a1813] to-[#252219] border border-gold/40 shadow-2xl overflow-hidden text-white">
                  <div className="absolute top-0 right-0 w-32 h-32 bg-gold/10 rounded-full blur-2xl pointer-events-none"></div>
                  
                  <div className="flex items-center justify-between">
                    <span className="font-serif-title font-bold text-gold tracking-wider text-base">PIXORA VIP</span>
                    <div className="w-9 h-7 rounded bg-gradient-to-br from-amber-300 via-amber-500 to-amber-700 p-0.5 opacity-90 shadow-sm flex items-center justify-center">
                      <div className="w-full h-full border border-black/30 rounded-sm"></div>
                    </div>
                  </div>

                  <div className="my-6">
                    <span className="text-[10px] uppercase font-mono text-gray-400 tracking-widest block mb-1">Card Number</span>
                    <p className="font-mono text-lg font-bold tracking-widest text-gold-light">
                      {cardNumber || '•••• •••• •••• ••••'}
                    </p>
                  </div>

                  <div className="flex items-end justify-between text-xs">
                    <div>
                      <span className="text-[9px] uppercase font-mono text-gray-400 block">Cardholder</span>
                      <p className="font-semibold uppercase tracking-wider text-gray-200 truncate max-w-[160px]">
                        {cardholderName || 'CARDHOLDER NAME'}
                      </p>
                    </div>
                    <div>
                      <span className="text-[9px] uppercase font-mono text-gray-400 block">Expires</span>
                      <p className="font-mono text-gray-200">
                        {expiryDate || 'MM/YY'}
                      </p>
                    </div>
                  </div>
                </div>

                {/* Card Input Form */}
                <div className="space-y-4 pt-2">
                  <div>
                    <label className="block text-xs font-semibold text-gray-300 mb-1.5">
                      Cardholder Full Name *
                    </label>
                    <input
                      type="text"
                      placeholder="e.g. Alexander Vance"
                      value={cardholderName}
                      onChange={(e) => setCardholderName(e.target.value)}
                      className="w-full bg-[#151515] border border-gray-700 rounded-xl px-4 py-3 text-sm text-white focus:outline-none focus:border-gold transition-colors"
                      required={paymentMethod === 'CARD'}
                    />
                  </div>

                  <div>
                    <label className="block text-xs font-semibold text-gray-300 mb-1.5 flex items-center justify-between">
                      <span>Card Number (16 Digits) *</span>
                      <span className="text-[11px] text-gray-400 font-mono">Visa / Mastercard / Amex</span>
                    </label>
                    <div className="relative">
                      <input
                        type="text"
                        placeholder="4532 •••• •••• 8892"
                        value={cardNumber}
                        onChange={handleCardNumberChange}
                        maxLength={19}
                        className="w-full bg-[#151515] border border-gray-700 rounded-xl pl-4 pr-11 py-3 text-sm text-white font-mono focus:outline-none focus:border-gold transition-colors"
                        required={paymentMethod === 'CARD'}
                      />
                      <CreditCard className="w-5 h-5 text-gold absolute right-3.5 top-3 pointer-events-none" />
                    </div>
                  </div>

                  <div className="grid grid-cols-2 gap-4">
                    <div>
                      <label className="block text-xs font-semibold text-gray-300 mb-1.5">
                        Expiry Date (MM/YY) *
                      </label>
                      <input
                        type="text"
                        placeholder="MM/YY"
                        value={expiryDate}
                        onChange={handleExpiryChange}
                        maxLength={5}
                        className="w-full bg-[#151515] border border-gray-700 rounded-xl px-4 py-3 text-sm text-white font-mono focus:outline-none focus:border-gold transition-colors"
                        required={paymentMethod === 'CARD'}
                      />
                    </div>

                    <div>
                      <label className="block text-xs font-semibold text-gray-300 mb-1.5 flex items-center justify-between">
                        <span>CVV / CVC *</span>
                        <span className="text-[10px] text-gray-500 font-mono">3-4 digits</span>
                      </label>
                      <div className="relative">
                        <input
                          type="password"
                          placeholder="•••"
                          value={cvv}
                          onChange={handleCvvChange}
                          maxLength={4}
                          className="w-full bg-[#151515] border border-gray-700 rounded-xl pl-4 pr-10 py-3 text-sm text-white font-mono focus:outline-none focus:border-gold transition-colors"
                          required={paymentMethod === 'CARD'}
                        />
                        <Lock className="w-4 h-4 text-gold/70 absolute right-3.5 top-3.5 pointer-events-none" />
                      </div>
                    </div>
                  </div>

                  <div className="p-3.5 rounded-xl bg-gold/5 border border-gold/20 flex items-center space-x-2 text-[11px] text-gold">
                    <Sparkles className="w-4 h-4 shrink-0" />
                    <span>Instant Authorization: Your reservation will be marked as <strong>PAID</strong> immediately upon checkout.</span>
                  </div>
                </div>
              </div>
            )}

            {/* BANK TRANSFER */}
            {paymentMethod === 'BANK_TRANSFER' && (
              <div className="space-y-6">
                <div className="p-5 rounded-2xl bg-[#0e0e0e] border border-gold/20 space-y-3 text-xs">
                  <p className="text-gray-300">
                    Please transfer the exact amount of <span className="text-gold font-bold">{formatLKR(totalAmount)}</span> to Pixora's corporate bank account:
                  </p>
                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 pt-2 text-xs">
                    <div>
                      <span className="text-gray-500 block">Bank Name:</span>
                      <span className="text-white font-semibold">Commercial Bank of Ceylon PLC</span>
                    </div>
                    <div>
                      <span className="text-gray-500 block">Account Name:</span>
                      <span className="text-white font-semibold">Pixora Event Photography Ltd</span>
                    </div>
                    <div>
                      <span className="text-gray-500 block">Account Number:</span>
                      <span className="text-gold font-mono font-bold text-sm tracking-wider">1000 8923 4451</span>
                    </div>
                    <div>
                      <span className="text-gray-500 block">Branch:</span>
                      <span className="text-white font-semibold">Colombo Main Branch</span>
                    </div>
                  </div>
                </div>

                <div className="space-y-2">
                  <label className="block text-xs font-semibold text-gray-300">
                    Bank Transaction Reference / Deposit Slip ID *
                  </label>
                  <input
                    type="text"
                    placeholder="e.g. TXN-8932481 or COMB-DEP-59218"
                    value={transactionRef}
                    onChange={(e) => setTransactionRef(e.target.value)}
                    className="w-full bg-[#151515] border border-gray-700 rounded-xl px-4 py-3.5 text-sm text-white focus:outline-none focus:border-gold transition-colors font-mono"
                    required={paymentMethod === 'BANK_TRANSFER'}
                  />
                  <p className="text-[11px] text-gray-400">
                    Enter the reference number from your bank app, ATM receipt, or counter slip.
                  </p>
                </div>
              </div>
            )}

            {/* Agree Terms Checkbox */}
            <div className="pt-2">
              <label className="flex items-start space-x-3 cursor-pointer">
                <input
                  type="checkbox"
                  checked={agreeTerms}
                  onChange={(e) => setAgreeTerms(e.target.checked)}
                  className="mt-0.5 w-4 h-4 rounded border-gray-700 text-gold focus:ring-gold bg-black/40"
                  required
                />
                <span className="text-xs text-gray-300 leading-relaxed">
                  I confirm that the celebration event details, add-on inclusions, and delivery tier are correct. I understand my booking will be officially reserved and an itemized official downloadable PDF receipt will be generated.
                </span>
              </label>
            </div>

          </div>

        </div>

        {/* Right Column: Promo Code & Price Summary */}
        <div className="space-y-6">
          <div className="glass-card rounded-2xl p-6 sm:p-8 border border-gold/30 space-y-6 sticky top-28">
            <h3 className="font-serif-title text-xl font-bold text-white border-b border-gray-800 pb-4">
              Payment Summary
            </h3>

            {/* ── Feature 6: Promo Code Input ─────────────────────── */}
            <div className="space-y-2">
              <label className="block text-xs font-semibold text-gray-300 flex items-center space-x-1.5">
                <Tag className="w-3.5 h-3.5 text-gold" />
                <span>Promo / Discount Voucher</span>
              </label>
              {appliedPromo ? (
                <div className="p-3 rounded-xl bg-emerald-950/40 border border-emerald-600/40 flex items-center justify-between text-xs">
                  <div className="flex items-center space-x-2">
                    <span className="font-mono font-bold text-emerald-400">{appliedPromo.code}</span>
                    <span className="text-emerald-300">({appliedPromo.discountPercent}% OFF)</span>
                  </div>
                  <button
                    type="button"
                    onClick={handleRemovePromo}
                    className="p-1 text-gray-400 hover:text-red-400"
                    title="Remove Promo Code"
                  >
                    <X className="w-4 h-4" />
                  </button>
                </div>
              ) : (
                <div className="flex space-x-2">
                  <input
                    type="text"
                    placeholder="e.g. CELEBRATE10"
                    value={promoInput}
                    onChange={(e) => setPromoInput(e.target.value.toUpperCase())}
                    className="flex-1 bg-[#151515] border border-gray-700 rounded-xl px-3 py-2 text-xs text-white uppercase font-mono focus:outline-none focus:border-gold"
                  />
                  <button
                    type="button"
                    onClick={handleValidatePromo}
                    disabled={validatingPromo || !promoInput.trim()}
                    className="px-3.5 py-2 rounded-xl btn-gold text-xs font-semibold disabled:opacity-50"
                  >
                    {validatingPromo ? '...' : 'Apply'}
                  </button>
                </div>
              )}
            </div>

            {/* Detailed Price Breakdown */}
            <div className="space-y-2.5 text-xs border-t border-gray-800 pt-4">
              <div className="flex justify-between text-gray-300">
                <span>{selectedPackage.name || selectedPackage.packageName}</span>
                <span className="font-mono">{formatLKR(basePackagePrice)}</span>
              </div>

              {selectedAddons.length > 0 && (
                <div className="space-y-1 pl-2 border-l border-gold/30">
                  {selectedAddons.map(id => {
                    const item = ADDON_OPTIONS.find(a => a.id === id);
                    return item ? (
                      <div key={id} className="flex justify-between text-[11px] text-gray-400">
                        <span>+ {item.label}</span>
                        <span className="font-mono text-gold">{formatLKR(item.price)}</span>
                      </div>
                    ) : null;
                  })}
                </div>
              )}

              <div className="flex justify-between text-gray-300">
                <span>Delivery: {tierItem.label}</span>
                <span className="font-mono text-gold">
                  {deliveryFee === 0 ? 'FREE' : formatLKR(deliveryFee)}
                </span>
              </div>

              {appliedPromo && (
                <div className="flex justify-between text-emerald-400 font-semibold">
                  <span>Promo Discount ({appliedPromo.code})</span>
                  <span className="font-mono">-{formatLKR(discountAmount)}</span>
                </div>
              )}

              <div className="pt-3 border-t border-gray-800 flex justify-between items-baseline">
                <span className="font-serif-title text-base font-bold text-white">Amount Due</span>
                <span className="font-serif-title text-2xl font-bold text-gold">
                  {formatLKR(totalAmount)}
                </span>
              </div>
            </div>

            <button
              type="submit"
              disabled={submitting}
              className="w-full py-4 rounded-xl btn-gold text-sm font-semibold shadow-lg shadow-gold/20 flex items-center justify-center space-x-2 disabled:opacity-50 disabled:cursor-not-allowed"
            >
              {submitting ? (
                <>
                  <div className="w-4 h-4 border-2 border-black border-t-transparent rounded-full animate-spin"></div>
                  <span>Securing Your Booking...</span>
                </>
              ) : (
                <>
                  <Lock className="w-4 h-4" />
                  <span>Confirm & Complete Booking</span>
                </>
              )}
            </button>

            <div className="flex items-center justify-center space-x-2 text-[11px] text-gray-400 pt-2 border-t border-gray-800">
              <ShieldCheck className="w-4 h-4 text-gold shrink-0" />
              <span>Encrypted & Verified by Pixora Admin</span>
            </div>
          </div>
        </div>

      </form>

    </div>
  );
};

export default Checkout;
