package com.marketplace.exception;

public class SlotNotAvailableException extends BookingException {
    public SlotNotAvailableException(String message) {
        super(message);
    }
}