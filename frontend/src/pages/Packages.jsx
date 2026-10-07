import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '../api/axios';
import { useCart } from '../context/CartContext';
import { Check, Sparkles, Award, ArrowRight, Clock, Image, Shield } from 'lucide-react';

// Functional component for displaying available photography packages and comparison details
const Packages = () => {
  // State for storing fetched photography package data
  const [packages, setPackages] = useState([]);
  // Loading state while fetching packages from the backend API
  const [loading, setLoading] = useState(true);
  // Custom cart context hook to select and store a package for booking
  const { selectPackage } = useCart();
  // React Router hook for programmatic navigation
  const navigate = useNavigate();

  // Fetch package list from API on initial component mount with fallback dummy data
  useEffect(() => {
    const fetchPackages = async () => {
      try {
        // API call to retrieve public package offerings
        const res = await api.get('/api/public/packages');
        setPackages(res.data);
      } catch (err) {
        // Log API fetch error and fallback to static package defaults
        console.error('Error fetching packages', err);
        setPackages([
          {
            packageId: 1,
            packageName: 'Kids Birthday Basic',
            priceLkr: 10000.0,
            description: 'Perfect starter package for intimate kids birthday celebrations. Includes 2-hour coverage, 50 edited digital photos, and private online gallery access.'
          },
          {
            packageId: 2,
            packageName: 'Premium Birthday',
            priceLkr: 15000.0,
            description: 'Comprehensive birthday photography with 4-hour coverage, 100 edited digital photos, printed photo album, and private online gallery access.'
          },
          {
            packageId: 3,
            packageName: 'Deluxe Birthday',
            priceLkr: 25000.0,
            description: 'Our finest birthday experience with full-day coverage, 200+ edited digital photos, premium leather photo album, large canvas print, and priority 48-hour delivery.'
          }
        ]);
      } finally {
        // Hide loading spinner once data is set
        setLoading(false);
      }
    };
    fetchPackages();
  }, []);

  // Handle package selection and redirect user to the cart page
  const handleBook = (pkg) => {
    selectPackage(pkg);
    navigate('/cart');
  };

  // Helper function to format numeric price values to LKR currency representation
  const formatLKR = (amount) => {
    return new Intl.NumberFormat('en-LK', {
      style: 'currency',
      currency: 'LKR',
      maximumFractionDigits: 0
    }).format(amount).replace('LKR', 'Rs.');
  };

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-16 space-y-20">
      
      {/* Page Header and intro banner section */}
      <div className="text-center max-w-3xl mx-auto space-y-4">
        <div className="inline-flex items-center space-x-2 px-3 py-1 rounded-full border border-gold/30 bg-gold/5 text-gold text-xs font-semibold tracking-wider uppercase">
          <Sparkles className="w-3.5 h-3.5" />
          <span>Tailored Birthday Collections</span>
        </div>
        <h1 className="font-serif-title text-4xl sm:text-5xl font-bold text-white">
          Choose Your Birthday Package
        </h1>
        <p className="text-sm sm:text-base text-gray-400 leading-relaxed">
          From intimate cake smashes to grand celebrations, select a photography collection crafted to preserve every moment. All packages include high-resolution files, verified photographers, and official receipts.
        </p>
      </div>

      {/* Conditional rendering for package cards grid or loading spinner */}
      {loading ? (
        // Loading spinner animation during data fetch
        <div className="flex justify-center py-20">
          <div className="w-10 h-10 border-4 border-gold/20 border-t-gold rounded-full animate-spin"></div>
        </div>
      ) : (
        // Grid container displaying individual package tier cards
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-8 items-stretch">
          {packages.map((pkg, idx) => {
            // Flag to highlight the middle (second) package as recommended
            const isFeatured = idx === 1;
            return (
              <div
                key={pkg.packageId}
                className={`glass-card rounded-3xl p-8 sm:p-10 flex flex-col justify-between relative transition-all duration-300 ${
                  isFeatured
                    ? 'border-gold shadow-[0_0_35px_rgba(212,175,55,0.2)] bg-gradient-to-b from-[#18160f] to-[#0f0f0f]'
                    : 'hover:border-gold/40'
                }`}
              >
                {/* Recommended badge overlay for the featured package */}
                {isFeatured && (
                  <div className="absolute -top-3.5 left-1/2 -translate-x-1/2 px-4 py-1 rounded-full bg-gold text-black text-xs font-bold uppercase tracking-wider shadow-lg">
                    Recommended
                  </div>
                )}

                <div>
                  {/* Tier indicator and decorative icon */}
                  <div className="flex items-center justify-between mb-4">
                    <span className="text-xs uppercase font-mono tracking-widest text-gold">
                      Tier 0{idx + 1}
                    </span>
                    <Award className="w-5 h-5 text-gold/70" />
                  </div>

                  {/* Package title heading */}
                  <h2 className="font-serif-title text-2xl sm:text-3xl font-bold text-white mb-3">
                    {pkg.packageName}
                  </h2>

                  {/* Package description body text */}
                  <p className="text-xs text-gray-400 leading-relaxed min-h-[50px] mb-6">
                    {pkg.description}
                  </p>

                  {/* Package price box */}
                  <div className="p-5 rounded-2xl bg-black/60 border border-gray-800 mb-8">
                    <span className="text-xs text-gray-400 block mb-1">Fixed Package Price</span>
                    <div className="flex items-baseline space-x-2">
                      <span className="font-serif-title text-3xl sm:text-4xl font-bold text-gold">
                        {formatLKR(pkg.priceLkr)}
                      </span>
                      <span className="text-xs text-gray-500">/ celebration</span>
                    </div>
                  </div>

                  {/* Key inclusions checklist */}
                  <h4 className="text-xs uppercase font-semibold tracking-wider text-gray-300 mb-4">
                    Package Inclusions:
                  </h4>
                  <ul className="space-y-3.5 text-xs text-gray-300 mb-8">
                    <li className="flex items-start space-x-3">
                      <Check className="w-4 h-4 text-gold shrink-0 mt-0.5" />
                      <span>{idx === 0 ? '2 Hours' : idx === 1 ? '4 Hours' : 'Full Day'} Event Coverage</span>
                    </li>
                    <li className="flex items-start space-x-3">
                      <Check className="w-4 h-4 text-gold shrink-0 mt-0.5" />
                      <span>{idx === 0 ? '50+' : idx === 1 ? '100+' : '200