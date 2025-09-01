package com.marketplace.enums;

public enum BookingStatus {
    PENDING,      // Booking requested but not confirmed
    CONFIRMED,    // Booking confirmed by professional
    CANCELLED,    // Booking cancelled by client or professional
    COMPLETED,    // Service completed
    REJECTED      // Booking rejected by professional
}