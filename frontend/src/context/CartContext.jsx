import React, { createContext, useContext, useState, useEffect } from 'react';

// Create cart context
const CartContext = createContext(null);

export const CartProvider = ({ children }) => {

  // Store selected package
  const [selectedPackage, setSelectedPackage] = useState(() => {
    try {
      // Load package from local storage
      const saved = localStorage.getItem('pixora_cart_package');
      return saved ? JSON.parse(saved) : null;
    } catch {
      return null;
    }
  });

  // Store booking details
  const [bookingDetails, setBookingDetails] = useState(() => {
    try {
      // Load booking details from local storage
      const saved = localStorage.getItem('pixora_cart_booking');

      return saved ? JSON.parse(saved) : {
        eventDate: '',
        eventTime: '14:00',
        venueAddress: '',
        photographerId: null,
        photographerName: '',
      };
    } catch {
      return {
        eventDate: '',
        eventTime: '14:00',
        venueAddress: '',
        photographerId: null,
        photographerName: '',
      };
    }
  });

  // Save selected package to local storage
  useEffect(() => {
    if (selectedPackage) {
      localStorage.setItem(
        'pixora_cart_package',
        JSON.stringify(selectedPackage)
      );
    } else {
      localStorage.removeItem('pixora_cart_package');
    }
  }, [selectedPackage]);

  // Save booking details to local storage
  useEffect(() => {
    localStorage.setItem(
      'pixora_cart_booking',
      JSON.stringify(bookingDetails)
    );
  }, [bookingDetails]);

  // Select a package
  const selectPackage = (pkg) => {
    setSelectedPackage(pkg);
  };

  // Update booking details
  const updateBookingDetails = (updates) => {
    setBookingDetails((prev) => ({ ...prev, ...updates }));
  };

  // Clear cart data
  const clearCart = () => {
    setSelectedPackage(null);

    setBookingDetails({
      eventDate: '',
      eventTime: '14:00',
      venueAddress: '',
      photographerId: null,
      photographerName: '',
    });

    // Remove saved cart data
    localStorage.removeItem('pixora_cart_package');
    localStorage.removeItem('pixora_cart_booking');
  };

  // Calculate package total
  const totalAmount = selectedPackage
    ? selectedPackage.priceLkr
    : 0;

  // Values shared through context
  const value = {
    selectedPackage,
    bookingDetails,
    itemCount: selectedPackage ? 1 : 0,
    totalAmount,
    selectPackage,
    updateBookingDetails,
    clearCart,
  };

  return (
    // Provide cart data to child components
    <CartContext.Provider value={value}>
      {children}
    </CartContext.Provider>
  );
};

// Custom hook to access cart context
export const useCart = () => {
  const context = useContext(CartContext);

  // Check provider usage
  if (!context) {
    throw new Error('useCart must be used within a CartProvider');
  }

  return context;
};