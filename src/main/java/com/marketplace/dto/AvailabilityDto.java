package com.marketplace.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;

import jakarta.validation.constraints.AssertTrue;
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
    
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;
    
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;
    
    @NotNull(message = "Bulk start time is required", groups = BulkValidation.class)
    @DateTimeFormat(pattern = "HH:mm")
    private LocalTime bulkStartTime;
    
    @NotNull(message = "Bulk end time is required", groups = BulkValidation.class)
    @DateTimeFormat(pattern = "HH:mm")
    private LocalTime bulkEndTime;
    
    // For weekly recurrence
    private boolean weeklyRecurrence = false;
    private Integer recurrenceWeeks = 1;
    
    // Custom validation for single availability
    @AssertTrue(message = "End time must be after start time")
    public boolean isValidTimeRange() {
        if (startTime != null && endTime != null) {
            return startTime.isBefore(endTime);
        }
        return true; // Let @NotNull handle null validation
    }
    
    // Custom validation for bulk availability
    @AssertTrue(message = "Bulk end time must be after bulk start time", groups = BulkValidation.class)
    public boolean isValidBulkTimeRange() {
        if (bulkStartTime != null && bulkEndTime != null) {
            return bulkStartTime.isBefore(bulkEndTime);
        }
        return true;
    }
    
    // Custom validation for date range
    @AssertTrue(message = "End date must be after or equal to start date", groups = BulkValidation.class)
    public boolean isValidDateRange() {
        if (startDate != null && endDate != null) {
            return !startDate.isAfter(endDate);
        }
        return true;
    }
    
    // Generate date range from start and end dates
    public List<LocalDate> generateDateRange() {
        List<LocalDate> dateRange = new ArrayList<>();
        
        if (startDate != null && endDate != null) {
            LocalDate current = startDate;
            while (!current.isAfter(endDate)) {
                dateRange.add(current);
                current = current.plusDays(1);
            }
        }
        
        return dateRange;
    }
    
    // Generate dates with weekly recurrence
    public List<LocalDate> generateRecurringDates() {
        List<LocalDate> recurringDates = new ArrayList<>();
        
        if (startDate != null && endDate != null && weeklyRecurrence && recurrenceWeeks != null) {
            // Get base dates for the first week
            List<LocalDate> baseDates = generateDateRange();
            
            // Add recurring weeks
            for (int week = 0; week < recurrenceWeeks; week++) {
                for (LocalDate baseDate : baseDates) {
                    LocalDate recurringDate = baseDate.plusWeeks(week);
                    if (!recurringDate.isAfter(endDate.plusWeeks(recurrenceWeeks - 1))) {
                        recurringDates.add(recurringDate);
                    }
                }
            }
        } else {
           
            recurringDates = generateDateRange();
        }
        
        return recurringDates;
    }
    
    
    public interface BulkValidation {}
}