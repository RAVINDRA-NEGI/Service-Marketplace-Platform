package com.marketplace.exception;

public class ConversationNotFoundException extends MessagingException {
    public ConversationNotFoundException(String message) {
        super(message);
    }

    public ConversationNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}