import React, { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import api from '../api/axios';
import { useCart } from '../context/CartContext';
import { Camera, ExternalLink, ShieldCheck, Star, Calendar, ArrowRight, UserCheck } from 'lucide-react';

const Photographers = () => {
  const [photographers, setPhotographers] = useState([]);
  const [reviewsMap, setReviewsMap] = useState({});
  const [loading, setLoading] = useState(true);
  const { updateBookingDetails } = useCart();
  const navigate = useNavigate();

  useEffect(() => {
    const fetchPhotographers = async () => {
      try {
        const res = await api.get('/api/public/photographers');
        const phList = res.data;
        setPhotographers(phList);

        const revMap = {};
        await Promise.all(
          phList.map(async (p) => {
            try {
              const r = await api.get(`/api/public/photographers/${p.userId}/reviews`);
              revMap[p.userId] = r.data || [];
            } catch {
              revMap[p.userId] = [];
            }
          })
        );
        setReviewsMap(revMap);
      } catch (err) {
        console.error('Error fetching photographers', err);
      } finally {
        setLoading(false);
      }
    };
    fetchPhotographers();
  }, []);

  const handleSelectPhotographer = (p) => {
    updateBookingDetails({
      photographerId: p.userId,
      photographerName: p.fullName
    });
    navigate('/cart');
  };

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-16 space-y-16">

      {/* Header */}
      <div className="text-center max-w-3xl mx-auto space-y-4">
        <div className="inline-flex items-center space-x-2 px-3 py-1 rounded-full border border-gold/30 bg-gold/5 text-gold text-xs font-semibold tracking-wider uppercase">
          <UserCheck className="w-3.5 h-3.5" />
          <span>Vetted Creative Masters</span>
        </div>
        <h1 className="font-serif-title text-4xl sm:text-5xl font-bold text-white">
          Meet Our Photographers
        </h1>
        <p className="text-sm sm:text-base text-gray-400">
          Our verified birthday photographers are experienced in capturing kids, families, cake smashes, and milestones with exceptional artistic flair.
        </p>
      </div>

      {/* Photographers Grid */}
      {loading ? (
        <div className="flex justify-center py-20">
          <div className="w-10 h-10 border-4 border-gold/20 border-t-gold rounded-full animate-spin"></div>
        </div>
      ) : photographers.length === 0 ? (
        <div className="glass-card rounded-3xl p-12 text-center max-w-xl mx-auto space-y-4">
          <Camera className="w-12 h-12 text-gold mx-auto opacity-70" />
          <h3 className="font-serif-title text-xl font-bold text-white">No Photographers Listed Yet</h3>
          <p className="text-xs text-gray-400">
            Our photography roster is being updated by administrators. You can still select any package and an expert photographer will be assigned to your booking automatically.
          </p>
          <div className="pt-2">
            <Link to="/packages" className="px-6 py-2.5 rounded-xl btn-gold text-xs font-semibold inline-block">
              Browse Packages
            </Link>
          </div>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-8">
          {photographers.map((p) => (
            <div
              key={p.userId}
              className="glass-card rounded-3xl p-8 flex flex-col justify-between border border-gray-800 hover:border-gold/50 transition-all duration-300 space-y-6"
            >
              <div>
                <div className="flex items-start justify-between">
                  <div className="w-16 h-16 rounded-2xl bg-gradient-to-br from-gold/20 to-black border border-gold/40 flex items-center justify-center text-gold font-serif-title text-2xl font-bold">
                    {p.fullName ? p.fullName.charAt(0).toUpperCase() : 'P'}
                  </div>
                  <span className="inline-flex items-center space-x-1 px-2.5 py-1 rounded-full bg-green-950/40 border border-green-500/30 text-green-400 text-[11px] font-medium">
                    <ShieldCheck className="w-3 h-3" />
                    <span>Verified Pro</span>
                  </span>
                </div>

                <div className="mt-5 space-y-1">
                  <h3 className="font-serif-title text-xl font-bold text-white">
                    {p.fullName}
                  </h3>
                  <p className="text-xs text-gold font-mono">
                    Specialist: Birthday & Milestone Photography
                  </p>
                </div>

                {/* Submitted Client Reviews Section */}
                <div className="mt-4 pt-4 border-t border-gray-800 space-y-3">
                  <div className="flex items-center justify-between">
                    <span className="text-xs uppercase font-mono tracking-wider text-gold font-semibold">
                      Client Reviews ({reviewsMap[p.userId]?.length || 0})
                    </span>
                    {reviewsMap[p.userId] && reviewsMap[p.userId].length > 0 && (
                      <div className="flex items-center text-gold font-mono text-xs font-bold space-x-1">
                        <Star className="w-3.5 h-3.5 fill-gold text-gold" />
                        <span>
                          {(
                            reviewsMap[p.userId].reduce((sum, r) => sum + r.starRating, 0) /
                            reviewsMap[p.userId].length
                          ).toFixed(1)} / 5.0
                        </span>
                      </div>
                    )}
                  </div>

                  {reviewsMap[p.userId] && reviewsMap[p.userId].length > 0 ? (
                    <div className="space-y-2.5 max-h-56 overflow-y-auto pr-1">
                      {reviewsMap[p.userId].map((r) => (
                        <div
                          key={r.reviewId}
                          className="p-3 rounded-xl bg-black/40 border border-gray-800/80 space-y-1.5"
                        >
                          <div className="flex items-center justify-between text-xs">
                            <span className="font-semibold text-white truncate max-w-[130px]">
                              {r.clientName || 'Verified Client'}
                            </span>
                            <div className="flex items-center space-x-0.5 text-gold">
                              {[...Array(5)].map((_, i) => (
                                <Star
                                  key={i}
                                  className={`w-3 h-3 ${
                                    i < r.starRating ? 'fill-gold text-gold' : 'text-gray-700'
                                  }`}
                                />
                              ))}
                            </div>
                          </div>
                          <p className="text-xs text-gray-300 italic leading-relaxed">
                            "{r.reviewComment}"
                          </p>
                          <div className="text-[10px] text-gray-400 font-mono text-right">
                            {r.createdAt
                              ? new Date(r.createdAt).toLocaleDateString('en-US', {
                                  month: 'short',
                                  day: 'numeric',
                                  year: 'numeric'
                                })
                              : 'Recent'}
                          </div>
                        </div>
                      ))}
                    </div>
                  ) : (
                    <div className="p-3 rounded-xl bg-black/20 border border-gray-800/40 text-center text-xs text-gray-400">
                      No client reviews yet. Be the first to book and share your experience!
                    </div>
                  )}
                </div>

                {p.portfolioUrl && (
                  <div className="mt-4 pt-4 border-t border-gray-800">
                    <a
                      href={p.portfolioUrl.startsWith('http') ? p.portfolioUrl : `https://${p.portfolioUrl}`}
                      target="_blank"
                      rel="noopener noreferrer"
                      className="inline-flex items-center space-x-1.5 text-xs text-gray-300 hover:text-gold transition-colors"
                    >
                      <ExternalLink className="w-3.5 h-3.5 text-gold" />
                      <span className="underline underline-offset-4">View External Portfolio</span>
                    </a>
                  </div>
                )}
              </div>

              <div className="pt-4 border-t border-gray-800 space-y-3">
                <button
                  onClick={() => handleSelectPhotographer(p)}
                  className="w-full py-2.5 rounded-xl border border-gold/40 text-gold hover:bg-gold hover:text-black font-semibold text-xs transition-all flex items-center justify-center space-x-2"
                >
                  <Calendar className="w-3.5 h-3.5" />
                  <span>Choose for My Celebration</span>
                </button>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Join as Photographer Banner */}
      <div className="glass-card rounded-3xl p-10 border border-gold/20 flex flex-col md:flex-row items-center justify-between gap-6">
        <div className="space-y-2 text-center md:text-left">
          <h3 className="font-serif-title text-2xl font-bold text-white">
            Are You a Birthday Event Photographer?
          </h3>
          <p className="text-xs text-gray-400 max-w-xl">
            Join the Pixora photographer network. Gain direct access to premium birthday bookings across Sri Lanka, direct bank payouts, and professional client management tools.
          </p>
        </div>
        <Link
          to="/register"
          className="px-6 py-3 rounded-xl btn-gold text-xs font-semibold shrink-0 shadow-lg"
        >
          Submit Photographer Application
        </Link>
      </div>

    </div>
  );
};

export default Photographers;
