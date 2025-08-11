package com.marketplace.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.marketplace.model.Availability;
import com.marketplace.model.ProfessionalProfile;

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
}
