package com.marketplace.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.marketplace.model.Booking;
import com.marketplace.model.Booking.BookingStatus;
import com.marketplace.model.ProfessionalProfile;
import com.marketplace.model.User;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {
    
    List<Booking> findByClientOrderByCreatedAtDesc(User client);
    
    List<Booking> findByProfessionalOrderByCreatedAtDesc(ProfessionalProfile professional);
    
    List<Booking> findByProfessionalAndStatusOrderByCreatedAtDesc(ProfessionalProfile professional, BookingStatus status);
    
    List<Booking> findByClientAndStatusOrderByCreatedAtDesc(User client, BookingStatus status);
    
    @Query("SELECT b FROM Booking b WHERE b.professional = :professional " +
           "AND b.bookingDate >= :startDate AND b.bookingDate <= :endDate " +
           "ORDER BY b.bookingDate ASC, b.startTime ASC")
    List<Booking> findBookingsByProfessionalAndDateRange(
        @Param("professional") ProfessionalProfile professional,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );
    
    @Query("SELECT b FROM Booking b WHERE b.client = :client " +
           "AND b.bookingDate >= :startDate AND b.bookingDate <= :endDate " +
           "ORDER BY b.bookingDate ASC, b.startTime ASC")
    List<Booking> findBookingsByClientAndDateRange(
        @Param("client") User client,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );
    
    List<Booking> findByAvailabilityId(Long availabilityId);
    
    boolean existsByAvailabilityIdAndStatusNot(Long availabilityId, BookingStatus status);
    
    List<Booking> findByProfessionalAndBookingDateAndStatusNot(
        ProfessionalProfile professional, 
        LocalDate date, 
        BookingStatus status
    );
}