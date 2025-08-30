package com.marketplace.dto;

import java.time.LocalDate;
import java.time.LocalTime;

import org.springframework.format.annotation.DateTimeFormat;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookingDto {

    private Long id;
    
    @NotNull(message = "Professional is required")
    private Long professionalId;
    
    @NotNull(message = "Availability slot is required")
    private Long availabilityId;
    
    private String serviceDetails;
    
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate bookingDate;
    
    @DateTimeFormat(pattern = "HH:mm")
    private LocalTime startTime;
    
    @DateTimeFormat(pattern = "HH:mm")
    private LocalTime endTime;
}