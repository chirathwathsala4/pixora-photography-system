import React, { useEffect, useState } from 'react';
import api from '../api/axios';
import { Camera, Sparkles, X, Eye, Maximize2 } from 'lucide-react';
import { getImageUrl } from '../utils/imageUrl';

const Portfolio = () => {
  const [photos, setPhotos] = useState([]);
  const [loading, setLoading] = useState(true);
  const [selectedPhoto, setSelectedPhoto] = useState(null);

  useEffect(() => {
    const fetchPortfolio = async () => {
      try {
        const res = await api.get('/api/public/portfolio');
        setPhotos(res.data);
      } catch (err) {
        console.error('Error loading portfolio', err);
      } finally {
        setLoading(false);
      }
    };
    fetchPortfolio();
  }, []);

  // Curated fallback photos to show aesthetic gallery if DB doesn't have published photos yet
  const sampleShowcase = [
    {
      photoId: 's1',
      photoUrl: 'https://images.unsplash.com/photo-1530103862676-de8c9debad1d?auto=format&fit=crop&w=1200&q=80',
      photographerName: 'Pixora Studio',
      title: '1st Milestone Celebration'
    },
    {
      photoId: 's2',
      photoUrl: 'https://images.unsplash.com/photo-1513151233558-d860c5398176?auto=format&fit=crop&w=1200&q=80',
      photographerName: 'Pixora Studio',
      title: 'Golden Confetti Joy'
    },
    {
      photoId: 's3',
      photoUrl: 'https://images.unsplash.com/photo-1527529482837-4698179dc6ce?auto=format&fit=crop&w=1200&q=80',
      photographerName: 'Pixora Studio',
      title: 'Birthday Cake Surprise'
    },
    {
      photoId: 's4',
      photoUrl: 'https://images.unsplash.com/photo-1464349153735-7db50ed83c84?auto=format&fit=crop&w=1200&q=80',
      photographerName: 'Pixora Studio',
      title: 'Magical Candle Wishes'
    },
    {
      photoId: 's5',
      photoUrl: 'https://images.unsplash.com/photo-1514525253161-7a46d19cd819?auto=format&fit=crop&w=1200&q=80',
      photographerName: 'Pixora Studio',
      title: 'Party Vibes & Laughs'
    },
    {
      photoId: 's6',
      photoUrl: 'https://images.unsplash.com/photo-1528605248644-14dd04022da1?auto=format&fit=crop&w=1200&q=80',
      photographerName: 'Pixora Studio',
      title: 'Family Gathering Moments'
    }
  ];

  const displayPhotos = photos.length > 0 ? photos : sampleShowcase;

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-16 space-y-12">
      
      {/* Header */}
      <div className="text-center max-w-2xl mx-auto space-y-4">
        <div className="inline-flex items-center space-x-2 px-3 py-1 rounded-full border border-gold/30 bg-gold/5 text-gold text-xs font-semibold tracking-wider uppercase">
          <Sparkles className="w-3.5 h-3.5" />
          <span>Curated Client Moments</span>
        </div>
        <h1 className="font-serif-title text-4xl sm:text-5xl font-bold text-white">
          Event Portfolio Gallery
        </h1>
        <p className="text-sm text-gray-400">
          Explore captured birthday celebrations, cake smashes, and milestones delivered by our verified photographers.
        </p>
      </div>

      {/* Gallery Grid */}
      {loading ? (
        <div className="flex justify-center py-20">
          <div className="w-10 h-10 border-4 border-gold/20 border-t-gold rounded-full animate-spin"></div>
        </div>
      ) : (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
          {displayPhotos.map((item, idx) => (
            <div
              key={item.photoId || idx}
              onClick={() => setSelectedPhoto(item)}
              className="group relative rounded-2xl overflow-hidden glass-card border border-gray-800 cursor-pointer aspect-[4/3] transition-all duration-300 hover:border-gold/60 hover:shadow-[0_10px_30px_-5px_rgba(212,175,55,0.2)]"
            >
              <img
                src={getImageUrl(item.photoUrl)}
                alt={item.title || 'Birthday Event Photo'}
                className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-500"
                loading="lazy"
                onError={(e) => {
                  e.target.src = 'https://images.unsplash.com/photo-1530103862676-de8c9debad1d?auto=format&fit=crop&w=1200&q=80';
                }}
              />
              <div className="absolute inset-0 bg-gradient-to-t from-black/90 via-black/30 to-transparent opacity-0 group-hover:opacity-100 transition-opacity duration-300 flex flex-col justify-end p-6">
                <div className="flex items-center justify-between text-gold">
                  <span className="text-xs uppercase font-mono tracking-wider">
                    {item.photographerName ? `By ${item.photographerName}` : 'Pixora Verified'}
                  </span>
                  <Maximize2 className="w-4 h-4" />
                </div>
                <h3 className="text-white font-serif-title text-base font-semibold mt-1">
                  {item.title || `Celebration #${item.bookingId || idx + 1}`}
                </h3>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Fullscreen Lightbox Modal */}
      {selectedPhoto && (
        <div
          className="fixed inset-0 z-50 bg-black/95 backdrop-blur-md flex items-center justify-center p-4"
          onClick={() => setSelectedPhoto(null)}
        >
          <button
            onClick={() => setSelectedPhoto(null)}
            className="absolute top-6 right-6 p-3 rounded-full bg-white/10 text-white hover:bg-gold hover:text-black transition-all"
          >
            <X className="w-6 h-6" />
          </button>
          
          <div
            className="max-w-4xl max-h-[85vh] flex flex-col items-center"
            onClick={(e) => e.stopPropagation()}
          >
            <img
              src={getImageUrl(selectedPhoto.photoUrl)}
              alt="Expanded view"
              className="max-w-full max-h-[75vh] object-contain rounded-xl shadow-2xl border border-gold/30"
              onError={(e) => {
                e.target.src = 'https://images.unsplash.com/photo-1530103862676-de8c9debad1d?auto=format&fit=crop&w=1200&q=80';
              }}
            />
            <div className="mt-4 text-center">
              <p className="text-gold font-mono text-xs uppercase tracking-widest">
                {selectedPhoto.photographerName ? `Captured by ${selectedPhoto.photographerName}` : 'Pixora Exclusive'}
              </p>
              <h3 className="text-white font-serif-title text-lg font-bold mt-1">
                {selectedPhoto.title || `Celebration #${selectedPhoto.bookingId || ''}`}
              </h3>
            </div>
          </div>
        </div>
      )}

    </div>
  );
};

export default Portfolio;