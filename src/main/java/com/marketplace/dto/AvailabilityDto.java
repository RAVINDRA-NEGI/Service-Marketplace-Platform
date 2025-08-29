package com.marketplace.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AvailabilityDto {

    private Long id;
    
    @NotNull(message = "Date is required")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate date;
    
    @NotNull(message = "Start time is required")
    @DateTimeFormat(pattern = "HH:mm")
    private LocalTime startTime;
    
    @NotNull(message = "End time is required")
    @DateTimeFormat(pattern = "HH:mm")
    private LocalTime endTime;
    
    // For bulk creation
    private List<LocalDate> dates;
    private LocalTime bulkStartTime;
    private LocalTime bulkEndTime;
    
    // For weekly recurrence
    private boolean weeklyRecurrence = false;
    private Integer recurrenceWeeks = 1;
}