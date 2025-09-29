package com.marketplace.exception;

public class MessageNotFoundException extends MessagingException {
    public MessageNotFoundException(String message) {
        super(message);
    }

    public MessageNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}