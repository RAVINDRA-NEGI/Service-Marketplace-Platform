package com.marketplace.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProfessionalProfileDto {

    private Long id;

    @NotBlank(message = "Bio is required")
    private String bio;

    private String certification;

    private String profilePhotoUrl;

    @NotNull(message = "Category is required")
    private Long categoryId;

    @Positive(message = "Hourly rate must be positive")
    private Double hourlyRate;

    @NotBlank(message = "Service area city is required")
    private String serviceAreaCity;

    @NotBlank(message = "Service area state is required")
    private String serviceAreaState;

   
    private String availabilityDates; 
    private String availabilityTimes; 
}