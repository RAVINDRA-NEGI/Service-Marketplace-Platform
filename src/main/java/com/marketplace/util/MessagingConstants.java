package com.marketplace.util;

public class MessagingConstants {
    
    // WebSocket destinations
    public static final String WS_TOPIC_CONVERSATIONS = "/topic/conversations";
    public static final String WS_TOPIC_CONVERSATION = "/topic/conversation/";
    public static final String WS_TOPIC_TYPING = "/topic/conversation/%s/typing";
    public static final String WS_TOPIC_RECEIPTS = "/topic/conversation/%s/receipts";
    public static final String WS_TOPIC_ONLINE_STATUS = "/topic/online-status";
    
    // WebSocket user queues
    public static final String WS_USER_QUEUE_MESSAGES = "/queue/messages";
    public static final String WS_USER_QUEUE_ERRORS = "/queue/errors";
    
    // File upload constants
    public static final String UPLOAD_DIR = "uploads/messages/";
    public static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB
    public static final String[] ALLOWED_FILE_TYPES = {
        "image/jpeg", "image/png", "image/gif", "image/webp",
        "application/pdf", "text/plain",
        "application/msword", "application/vnd.openxmlformats-officedocument",
        "application/vnd.ms-excel", "application/vnd.openxmlformats-officedocument.spreadsheetml"
    };
    
    // Error messages
    public static final String ERROR_CONVERSATION_NOT_FOUND = "Conversation not found";
    public static final String ERROR_MESSAGE_NOT_FOUND = "Message not found";
    public static final String ERROR_ACCESS_DENIED = "Access denied";
    public static final String ERROR_FILE_UPLOAD_FAILED = "File upload failed";
    public static final String ERROR_INVALID_FILE_TYPE = "Invalid file type";
    
    // Success messages
    public static final String SUCCESS_MESSAGE_SENT = "Message sent successfully";
    public static final String SUCCESS_FILE_UPLOADED = "File uploaded successfully";
    public static final String SUCCESS_CONVERSATION_CREATED = "Conversation created successfully";
    
    // Quick reply suggestions
    public static final String[] QUICK_REPLIES = {
        "When are you available?",
        "Can you show a photo?",
        "What's your rate?",
        "How long will this take?",
        "Can you provide an estimate?",
        "I need this urgently",
        "Thanks for your help!"
    };
}