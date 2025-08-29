package com.marketplace.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Table(name = "availability", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"professional_id", "date", "start_time"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Availability {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "professional_id", nullable = false)
    @ToString.Exclude
    private ProfessionalProfile professional;

    @Column(name = "date", nullable = false)
    private LocalDate date;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column(name = "is_booked", nullable = false)
    private boolean isBooked = false;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    // Constructor for creating availability
    public Availability(ProfessionalProfile professional, LocalDate date, LocalTime startTime, LocalTime endTime) {
        this.professional = professional;
        this.date = date;
        this.startTime = startTime;
        this.endTime = endTime;
    }
    
    // Check if time slot overlaps with another
    public boolean overlapsWith(LocalTime otherStart, LocalTime otherEnd) {
        return this.startTime.isBefore(otherEnd) && otherStart.isBefore(this.endTime);
    }
}