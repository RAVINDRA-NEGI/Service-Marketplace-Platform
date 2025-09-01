package com.marketplace.service;

import java.time.LocalDate;
import java.util.List;

import com.marketplace.dto.BookingDto;
import com.marketplace.enums.BookingStatus;
import com.marketplace.model.Booking;
import com.marketplace.model.User;

public interface BookingService {
    
    // Create booking
    Booking createBooking(User client, BookingDto bookingDto);
    
    // Get bookings
    List<Booking> getClientBookings(User client);
    List<Booking> getProfessionalBookings(User professional);
    List<Booking> getClientBookingsByStatus(User client, BookingStatus status);
    List<Booking> getProfessionalBookingsByStatus(User professional, BookingStatus status);
    List<Booking> getBookingsByDateRange(User user, LocalDate startDate, LocalDate endDate, boolean isClient);
    
    // Update booking
    Booking updateBookingStatus(Long bookingId, User user, BookingStatus status);
    Booking updateBookingDetails(Long bookingId, User user, String serviceDetails);
    
    // Delete/cancel booking
    void cancelBooking(Long bookingId, User user);
    
    // Check availability
    boolean isSlotBooked(Long availabilityId);
    boolean canBookSlot(Long availabilityId, User client);
    
    // Get by ID
    Booking getBookingById(Long id);
    Booking getBookingByIdAndUser(Long id, User user);
    
 // Add to existing interface
    List<Booking> getProfessionalBookingsWithDetails(User professional);
    Booking acceptBooking(Long bookingId, User professional);
    Booking rejectBooking(Long bookingId, User professional);
    Booking markAsCompleted(Long bookingId, User professional);
    boolean canManageBooking(Long bookingId, User professional);
}