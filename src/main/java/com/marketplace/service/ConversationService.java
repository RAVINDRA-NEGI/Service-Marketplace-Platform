package com.marketplace.service;

import java.util.List;

import com.marketplace.model.Conversation;
import com.marketplace.model.User;

public interface ConversationService {
    
    // Create/get conversations
    Conversation createConversation(User client, User professional);
    Conversation createConversation(User client, User professional, Long bookingId);
    Conversation getConversationById(Long conversationId);
    Conversation getOrCreateConversation(User user1, User user2);
    
    // Get conversations for user
    List<Conversation> getConversationsForUser(User user);
    List<Conversation> getConversationsForClient(User client);
    List<Conversation> getConversationsForProfessional(User professional);
    
    // Check access
    boolean hasAccessToConversation(Conversation conversation, User user);
    
    // Update conversation
    void updateLastMessage(Conversation conversation, String content, com.marketplace.enums.MessageType type);
    void incrementUnreadCount(Conversation conversation, User user);
    void resetUnreadCount(Conversation conversation, User user);
    
    // Get conversation by booking
    Conversation getConversationByBookingId(Long bookingId);
    
    // Check if conversation exists
    boolean conversationExists(User client, User professional);
}