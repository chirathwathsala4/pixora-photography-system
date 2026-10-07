import React, { useEffect, useState, useRef } from 'react';
import { useAuth } from '../../context/AuthContext';
import api from '../../api/axios';
import {
  Camera,
  Upload,
  Calendar,
  Clock,
  MapPin,
  User,
  CheckCircle2,
  AlertCircle,
  Trash2,
  ExternalLink,
  Image as ImageIcon,
  X,
  Eye,
  Download,
  MessageCircle
} from 'lucide-react';
import { toast } from 'react-toastify';
import { getImageUrl, downloadImage } from '../../utils/imageUrl';
import BookingChatModal from '../../components/BookingChatModal';

const PhotographerDashboard = () => {
  const { user } = useAuth();
  const [bookings, setBookings] = useState([]);
  const [loading, setLoading] = useState(true);
  const [chatBooking, setChatBooking] = useState(null);

  // Upload modal state
  const [uploadBooking, setUploadBooking] = useState(null);
  const [bookingPhotos, setBookingPhotos] = useState([]);
  const [loadingPhotos, setLoadingPhotos] = useState(false);
  const [uploading, setUploading] = useState(false);
  const fileInputRef = useRef(null);

  const [respondingId, setRespondingId] = useState(null);

  const fetchPhotographerBookings = async () => {
    try {
      const res = await api.get('/api/photographer/bookings');
      setBookings(res.data);
    } catch (err) {
      console.error('Error fetching photographer bookings', err);
      toast.error('Could not load assigned bookings');
    } finally {
      setLoading(false);
    }
  };

  const handleRespond = async (bookingId, action) => {
    setRespondingId(bookingId);
    try {
      await api.put(`/api/photographer/bookings/${bookingId}/respond?action=${action}`);
      if (action === 'ACCEPT') {
        toast.success(`Booking #${bookingId} accepted! It has been added to your schedule.`);
      } else {
        toast.info(`Booking #${bookingId} declined. The administration team has been notified.`);
      }
      fetchPhotographerBookings();
    } catch (err) {
      console.error('Error responding to booking assignment', err);
      const msg = err.response?.data?.error || err.response?.data?.message || 'Failed to update assignment';
      toast.error(msg);
    } finally {
      setRespondingId(null);
    }
  };

  useEffect(() => {
    fetchPhotographerBookings();
  }, []);

  const openUploadModal = async (booking) => {
    setUploadBooking(booking);
    setLoadingPhotos(true);
    try {
      const res = await api.get(`/api/photographer/bookings/${booking.bookingId}/photos`);
      setBookingPhotos(res.data);
    } catch (err) {
      console.error('Error fetching photos', err);
    } finally {
      setLoadingPhotos(false);
    }
  };

  const handleFileUpload = async (e) => {
    const files = e.target.files;
    if (!files || files.length === 0) return;

    setUploading(true);
    let successCount = 0;

    for (let i = 0; i < files.length; i++) {
      const file = files[i];
      const formData = new FormData();
      formData.append('file', file);

      try {
        await api.post(`/api/photographer/bookings/${uploadBooking.bookingId}/photos`, formData, {
          headers: { 'Content-Type': 'multipart/form-data' },
        });
        successCount++;
      } catch (err) {
        console.error('Error uploading file', file.name, err);
      }
    }

    setUploading(false);
    if (fileInputRef.current) fileInputRef.current.value = '';

    if (successCount > 0) {
      toast.success(`Successfully uploaded ${successCount} photo(s)!`);
      // Refresh photos
      try {
        const res = await api.get(`/api/photographer/bookings/${uploadBooking.bookingId}/photos`);
        setBookingPhotos(res.data);
      } catch (e) {}
    } else {
      toast.error('Failed to upload photos');
    }
  };

  const handleDeletePhoto = async (photoId) => {
    if (!window.confirm('Delete this photo from the event gallery?')) return;
    try {
      await api.delete(`/api/photographer/photos/${photoId}`);
      toast.success('Photo removed');
      setBookingPhotos((prev) => prev.filter((p) => p.photoId !== photoId));
    } catch (err) {
      toast.error('Failed to delete photo');
    }
  };

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-14 space-y-10">

      {/* Header */}
      <div className="border-b border-gray-800 pb-6 flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <span className="text-xs uppercase font-mono tracking-widest text-gold">
            Photographer Studio
          </span>
          <h1 className="font-serif-title text-3xl sm:text-4xl font-bold text-white mt-1">
            Welcome, {user?.fullName}
          </h1>
          <p className="text-xs sm:text-sm text-gray-400 mt-1">
            View your assigned birthday event assignments, client logistics, and upload edited deliverables.
          </p>
        </div>

        <div className="flex items-center space-x-2 self-start sm:self-auto px-4 py-2 rounded-xl bg-green-950/40 border border-green-500/30 text-green-400 text-xs font-semibold">
          <CheckCircle2 className="w-4 h-4" />
          <span>Active Partner Photographer</span>
        </div>
      </div>

      {/* Bookings List */}
      {loading ? (
        <div className="flex justify-center py-20">
          <div className="w-10 h-10 border-4 border-gold/20 border-t-gold rounded-full animate-spin"></div>
        </div>
      ) : bookings.length === 0 ? (
        <div className="glass-card rounded-3xl p-14 text-center max-w-xl mx-auto space-y-4">
          <Camera className="w-12 h-12 text-gold mx-auto opacity-70" />
          <h3 className="font-serif-title text-xl font-bold text-white">No Assigned Bookings</h3>
          <p className="text-xs text-gray-400 leading-relaxed">
            You currently have no birthday celebrations assigned to your schedule. As clients book packages matching your availability, the administration team will assign events to you.
          </p>
        </div>
      ) : (
        <div className="space-y-10">
          {/* Pending Assignment Requests Section */}
          {bookings.filter(b => b.staffStatus === 'PENDING_ACCEPTANCE').length > 0 && (
            <div className="space-y-4">
              <div className="flex items-center space-x-2">
                <span className="w-2.5 h-2.5 rounded-full bg-amber-400 animate-pulse"></span>
                <h3 className="font-serif-title text-xl font-bold text-amber-400">
                  New Assignment Requests ({bookings.filter(b => b.staffStatus === 'PENDING_ACCEPTANCE').length})
                </h3>
              </div>
              <p className="text-xs text-gray-400">
                The Pixora admin has assigned you to the following celebrations. Please review the details and accept or decline.
              </p>

              <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                {bookings.filter(b => b.staffStatus === 'PENDING_ACCEPTANCE').map((b) => (
                  <div
                    key={b.bookingId}
                    className="glass-card rounded-2xl p-6 sm:p-8 border border-amber-500/40 bg-amber-950/10 hover:border-amber-400 transition-all flex flex-col justify-between space-y-6"
                  >
                    <div>
                      <div className="flex items-center justify-between mb-2">
                        <span className="font-mono text-xs text-amber-400 font-semibold">Request #{b.bookingId}</span>
                        <span className="text-[10px] font-mono px-2.5 py-0.5 rounded bg-amber-900/60 text-amber-300 border border-amber-500/40">
                          {b.clientNotes ? 'Schedule Update Re-Approval' : 'Awaiting Your Response'}
                        </span>
                      </div>

                      <h4 className="font-serif-title text-xl font-bold text-white">
                        {b.packageName}
                      </h4>

                      <div className="mt-4 space-y-2.5 text-xs text-gray-300">
                        <div className="flex items-start space-x-2.5">
                          <User className="w-4 h-4 text-amber-400 shrink-0 mt-0.5" />
                          <div>
                            <span className="text-gray-500 block">Client</span>
                            <span className="font-semibold text-white">{b.clientName}</span>
                          </div>
                        </div>

                        <div className="flex items-start space-x-2.5">
                          <Calendar className="w-4 h-4 text-amber-400 shrink-0 mt-0.5" />
                          <div>
                            <span className="text-gray-500 block">Event Date & Time</span>
                            <span className="font-semibold text-white">{b.eventDate} at {b.eventTime}</span>
                          </div>
                        </div>

                        <div className="flex items-start space-x-2.5">
                          <MapPin className="w-4 h-4 text-amber-400 shrink-0 mt-0.5" />
                          <div>
                            <span className="text-gray-500 block">Venue Location</span>
                            <span className="font-semibold text-white">{b.venueAddress}</span>
                          </div>
                        </div>

                        {b.clientNotes && (
                          <div className="p-2.5 rounded-lg bg-black/40 border border-amber-800/40 text-[11px] text-amber-200">
                            <span className="text-gray-400 font-semibold block">Client Update Notes:</span>
                            {b.clientNotes}
                          </div>
                        )}
                      </div>
                    </div>

                    <div className="pt-4 border-t border-amber-900/40 flex items-center gap-2">
                      <button
                        onClick={() => handleRespond(b.bookingId, 'ACCEPT')}
                        disabled={respondingId === b.bookingId}
                        className="flex-1 py-2.5 rounded-xl bg-gradient-to-r from-emerald-600 to-green-600 hover:from-emerald-500 hover:to-green-500 text-white font-semibold text-xs transition-all shadow flex items-center justify-center space-x-1.5"
                      >
                        <CheckCircle2 className="w-3.5 h-3.5" />
                        <span>{respondingId === b.bookingId ? 'Updating...' : b.clientNotes ? 'Approve Changes' : 'Accept Assignment'}</span>
                      </button>
                      <button
                        onClick={() => handleRespond(b.bookingId, 'DECLINE')}
                        disabled={respondingId === b.bookingId}
                        className="px-3.5 py-2.5 rounded-xl border border-red-800/60 bg-red-950/20 hover:bg-red-950/40 text-red-400 text-xs font-semibold transition-all"
                      >
                        {b.clientNotes ? 'Decline Changes' : 'Decline'}
                      </button>
                      <button
                        onClick={() => setChatBooking(b)}
                        className="p-2.5 rounded-xl border border-purple-600/40 bg-purple-950/20 text-purple-400 hover:bg-purple-950/40 transition-all"
                        title="Chat with Client"
                      >
                        <MessageCircle className="w-4 h-4" />
                      </button>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* Confirmed / Active Schedule Section */}
          <div className="space-y-4">
            <h3 className="font-serif-title text-xl font-bold text-white">
              Confirmed Celebrations Schedule ({bookings.filter(b => b.staffStatus === 'STAFFED' || (!b.staffStatus && b.status !== 'CANCELLED')).length})
            </h3>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
              {bookings.filter(b => b.staffStatus === 'STAFFED' || (!b.staffStatus && b.status !== 'CANCELLED')).map((b) => (
                <div
                  key={b.bookingId}
                  className="glass-card rounded-2xl p-6 sm:p-8 border border-gray-800 hover:border-gold/40 transition-all flex flex-col justify-between space-y-6"
                >
                  <div>
                    <div className="flex items-center justify-between mb-2">
                      <span className="font-mono text-xs text-gold">Booking #{b.bookingId}</span>
                      <div className="flex items-center space-x-2">
                        <span className="text-[10px] font-mono px-2 py-0.5 rounded bg-emerald-950/60 text-emerald-400 border border-emerald-800/40">
                          Staffed
                        </span>
                        <span className={`text-[10px] font-mono px-2 py-0.5 rounded ${
                          b.status === 'CONFIRMED' ? 'bg-green-950 text-green-400 border border-green-800' :
                          b.status === 'COMPLETED' ? 'bg-blue-950 text-blue-400 border border-blue-800' :
                          b.status === 'CANCELLED' ? 'bg-red-950 text-red-400 border border-red-800' :
                          'bg-yellow-950 text-yellow-400 border border-yellow-800'
                        }`}>
                          {b.status}
                        </span>
                      </div>
                    </div>

                    <h4 className="font-serif-title text-xl font-bold text-white">
                      {b.packageName}
                    </h4>

                    <div className="mt-5 space-y-3 text-xs text-gray-300">
                      <div className="flex items-start space-x-2.5">
                        <User className="w-4 h-4 text-gold shrink-0 mt-0.5" />
                        <div>
                          <span className="text-gray-500 block">Client Name</span>
                          <span className="font-semibold text-white">{b.clientName}</span>
                        </div>
                      </div>

                      <div className="flex items-start space-x-2.5">
                        <Calendar className="w-4 h-4 text-gold shrink-0 mt-0.5" />
                        <div>
                          <span className="text-gray-500 block">Date & Time</span>
                          <span className="font-semibold text-white">{b.eventDate} at {b.eventTime}</span>
                        </div>
                      </div>

                      <div className="flex items-start space-x-2.5">
                        <MapPin className="w-4 h-4 text-gold shrink-0 mt-0.5" />
                        <div>
                          <span className="text-gray-500 block">Venue Location</span>
                          <span className="font-semibold text-white">{b.venueAddress}</span>
                        </div>
                      </div>
                    </div>
                  </div>

                  <div className="pt-4 border-t border-gray-800 flex items-center gap-3">
                    <button
                      onClick={() => setChatBooking(b)}
                      className="px-4 py-3 rounded-xl border border-purple-600/40 bg-purple-950/20 hover:bg-purple-950/40 text-purple-400 text-xs font-semibold flex items-center justify-center space-x-1.5 transition-all shadow"
                    >
                      <MessageCircle className="w-4 h-4" />
                      <span>Chat</span>
                    </button>
                    <button
                      onClick={() => openUploadModal(b)}
                      className="flex-1 py-3 rounded-xl btn-gold text-xs font-semibold flex items-center justify-center space-x-2 shadow"
                    >
                      <Upload className="w-4 h-4" />
                      <span>Upload / Manage Event Photos</span>
                    </button>
                  </div>
                </div>
              ))}
            </div>
          </div>
        </div>
      )}


      {/* ── MODAL: Upload & Manage Photos ────────────────────────── */}
      {uploadBooking && (
        <div className="fixed inset-0 z-50 bg-black/90 backdrop-blur-md flex items-center justify-center p-4">
          <div className="glass-card rounded-3xl p-6 sm:p-8 max-w-4xl w-full max-h-[90vh] flex flex-col border border-gold/40 space-y-6">

            {/* Modal Header */}
            <div className="flex items-center justify-between border-b border-gray-800 pb-4">
              <div>
                <span className="text-xs font-mono text-gold uppercase">
                  Event Deliverables · Booking #{uploadBooking.bookingId}
                </span>
                <h3 className="font-serif-title text-xl font-bold text-white">
                  {uploadBooking.clientName}'s Birthday ({uploadBooking.packageName})
                </h3>
              </div>
              <button
                onClick={() => setUploadBooking(null)}
                className="p-1.5 text-gray-400 hover:text-white"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            {/* Upload Area */}
            <div className="p-6 rounded-2xl border-2 border-dashed border-gold/40 bg-gold/[0.02] text-center space-y-3">
              <Upload className="w-8 h-8 text-gold mx-auto" />
              <div>
                <p className="text-xs font-semibold text-white">
                  Upload High-Resolution Edited Birthday Photos
                </p>
                <p className="text-[11px] text-gray-400">
                  Select JPEG/PNG files. Photos will immediately appear in the client's private gallery.
                </p>
              </div>

              <input
                type="file"
                multiple
                accept="image/*"
                ref={fileInputRef}
                onChange={handleFileUpload}
                className="hidden"
                id="photo-file-upload"
              />

              <label
                htmlFor="photo-file-upload"
                className={`inline-flex items-center space-x-2 px-6 py-2.5 rounded-xl btn-gold text-xs font-semibold cursor-pointer shadow ${
                  uploading ? 'opacity-50 pointer-events-none' : ''
                }`}
              >
                {uploading ? (
                  <>
                    <div className="w-3.5 h-3.5 border-2 border-black border-t-transparent rounded-full animate-spin"></div>
                    <span>Uploading deliverables...</span>
                  </>
                ) : (
                  <>
                    <Camera className="w-4 h-4" />
                    <span>Select Images to Upload</span>
                  </>
                )}
              </label>
            </div>

            {/* Uploaded Gallery Grid */}
            <div className="flex-1 overflow-y-auto pr-2 space-y-3">
              <h4 className="text-xs uppercase font-semibold text-gray-300">
                Uploaded Photos ({bookingPhotos.length})
              </h4>

              {loadingPhotos ? (
                <div className="flex justify-center py-10">
                  <div className="w-8 h-8 border-3 border-gold/20 border-t-gold rounded-full animate-spin"></div>
                </div>
              ) : bookingPhotos.length === 0 ? (
                <p className="text-xs text-gray-500 py-6 text-center">No photos uploaded yet for this booking.</p>
              ) : (
                <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
                  {bookingPhotos.map((photo) => (
                    <div
                      key={photo.photoId}
                      className="group relative rounded-xl overflow-hidden border border-gray-800 aspect-square bg-black"
                    >
                      <img
                        src={getImageUrl(photo.photoUrl)}
                        alt="Uploaded photo"
                        className="w-full h-full object-cover"
                        onError={(e) => {
                          e.target.src = 'https://images.unsplash.com/photo-1530103862676-de8c9debad1d?auto=format&fit=crop&w=800&q=80';
                        }}
                      />
                      <div className="absolute inset-0 bg-black/70 opacity-0 group-hover:opacity-100 transition-opacity flex items-center justify-center space-x-2 p-2">
                        <a
                          href={getImageUrl(photo.photoUrl)}
                          target="_blank"
                          rel="noreferrer"
                          className="p-2 rounded-lg bg-black/80 hover:bg-black text-gold border border-gold/40 transition-colors"
                          title="View High Resolution"
                        >
                          <Eye className="w-3.5 h-3.5" />
                        </a>
                        <button
                          onClick={() => downloadImage(photo.photoUrl, `Pixora-Event-${uploadBooking.bookingId}-${photo.photoId}.jpg`)}
                          className="p-2 rounded-lg btn-gold text-xs shadow"
                          title="Download High-Res"
                        >
                          <Download className="w-3.5 h-3.5" />
                        </button>
                        <button
                          onClick={() => handleDeletePhoto(photo.photoId)}
                          className="p-2 rounded-lg bg-red-950 text-red-400 hover:bg-red-900 border border-red-700/50"
                          title="Delete Photo"
                        >
                          <Trash2 className="w-3.5 h-3.5" />
                        </button>
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>

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

export default PhotographerDashboard;
