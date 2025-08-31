package com.marketplace.repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.marketplace.model.Availability;
import com.marketplace.model.ProfessionalProfile;

import jakarta.transaction.Transactional;

@Repository
public interface AvailabilityRepository extends JpaRepository<Availability, Long> {
    
    List<Availability> findByProfessionalAndDateOrderByStartTime(ProfessionalProfile professional, LocalDate date);
    
    List<Availability> findByProfessionalOrderByDateAscStartTimeAsc(ProfessionalProfile professional);
    
    @Query("SELECT a FROM Availability a WHERE a.professional = :professional " +
           "AND a.date >= :startDate AND a.date <= :endDate AND a.isBooked = false " +
           "ORDER BY a.date ASC, a.startTime ASC")
    List<Availability> findAvailableSlotsByProfessionalAndDateRange(
        @Param("professional") ProfessionalProfile professional,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );
    
    @Query("SELECT a FROM Availability a WHERE a.professional = :professional " +
           "AND a.date = :date AND a.isBooked = false " +
           "AND NOT EXISTS (SELECT 1 FROM Availability a2 WHERE a2.professional = :professional " +
           "AND a2.date = :date AND a2.isBooked = false AND " +
           "a2.startTime < :endTime AND a2.endTime > :startTime) " +
           "ORDER BY a.startTime ASC")
    List<Availability> findNonOverlappingAvailableSlots(
        @Param("professional") ProfessionalProfile professional,
        @Param("date") LocalDate date,
        @Param("startTime") LocalTime startTime,
        @Param("endTime") LocalTime endTime
    );
    
    boolean existsByProfessionalAndDateAndStartTimeAndEndTime(
        ProfessionalProfile professional, 
        LocalDate date, 
        LocalTime startTime, 
        LocalTime endTime
    );
    
    Optional<Availability> findByProfessionalAndDateAndStartTimeAndEndTime(
        ProfessionalProfile professional, 
        LocalDate date, 
        LocalTime startTime, 
        LocalTime endTime
    );
    
    List<Availability> findByProfessionalAndDateAndIsBookedFalse(ProfessionalProfile professional, LocalDate date);
    
    List<Availability> findByProfessionalAndIsBookedFalseOrderByDateAscStartTimeAsc(ProfessionalProfile professional);
    /**
     * Atomically marks an availability slot as booked if it's currently available
     * Using native SQL query to avoid JPA property mapping issues
     * @param availabilityId the ID of the availability slot
     * @return the number of rows updated (1 if successful, 0 if slot was already booked)
     */
    @Modifying
    @Transactional
    @Query(value = "UPDATE availability SET is_booked = true WHERE id = :availabilityId AND is_booked = false", 
           nativeQuery = true)
    int markAsBookedIfAvailable(@Param("availabilityId") Long availabilityId);
    
    /**
     * Atomically releases a booked slot
     * Using native SQL query to avoid JPA property mapping issues
     * @param availabilityId the ID of the availability slot
     * @return the number of rows updated
     */
    @Modifying
    @Transactional
    @Query(value = "UPDATE availability SET is_booked = false WHERE id = :availabilityId", 
           nativeQuery = true)
    int releaseSlot(@Param("availabilityId") Long availabilityId);
    
    // Alternative JPQL version if you prefer (try this if the above doesn't work)
    /*
    @Modifying
    @Transactional
    @Query("UPDATE Availability a SET a.isBooked = true WHERE a.id = :availabilityId AND a.isBooked = false")
    int markAsBookedIfAvailable(@Param("availabilityId") Long availabilityId);
    
    @Modifying
    @Transactional
    @Query("UPDATE Availability a SET a.isBooked = false WHERE a.id = :availabilityId")
    int releaseSlot(@Param("availabilityId") Long availabilityId);
    */
}
