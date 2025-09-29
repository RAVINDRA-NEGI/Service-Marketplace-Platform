package com.marketplace.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.marketplace.dto.SendMessageDto;
import com.marketplace.model.Conversation;
import com.marketplace.model.Message;
import com.marketplace.model.User;

public interface MessageService {
    
    // Create messages
    Message createMessage(Conversation conversation, User sender, SendMessageDto sendMessageDto);
    Message createSystemMessage(Conversation conversation, String content);
    
    // Get messages
    Message getMessageById(Long messageId);
    List<Message> getMessagesByConversation(Conversation conversation);
    Page<Message> getMessagesByConversation(Conversation conversation, Pageable pageable);
    List<Message> getRecentMessages(Conversation conversation, int offset, int limit);
    List<Message> getMessagesBySender(User sender);
    
    // Mark as read/delivered
    void markMessagesAsRead(Conversation conversation, User user);
    void markMessagesAsDelivered(Conversation conversation, User user);
    
    // Message counts
    long countMessagesByConversation(Conversation conversation);
    long countUnreadMessages(Conversation conversation, User user);
    
    // Delete messages
    void deleteMessage(Message message);
    void deleteMessagesByConversation(Conversation conversation);
    
    // Update message status
    void updateMessageStatus(Long messageId, com.marketplace.enums.MessageStatus status);
    
    // Get last message
    Message getLastMessage(Conversation conversation);
}