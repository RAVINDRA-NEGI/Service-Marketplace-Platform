package com.marketplace.exception;

public class AvailabilityNotFoundException extends BookingException {
    public AvailabilityNotFoundException(String message) {
        super(message);
    }
}