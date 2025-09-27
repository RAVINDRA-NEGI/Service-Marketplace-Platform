package com.marketplace.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.marketplace.dto.MessageDto;
import com.marketplace.dto.SendMessageDto;
import com.marketplace.enums.MessageStatus;
import com.marketplace.model.Conversation;
import com.marketplace.model.Message;
import com.marketplace.model.User;

public interface MessageService {
    
    // Message creation and processing
    Message createMessage(Conversation conversation, User sender, String content, String messageType);
    Message processSendMessage(SendMessageDto sendMessageDto, User sender, Conversation conversation);
    Message saveMessage(Message message);
    
    // Message retrieval
    List<Message> getMessagesByConversation(Conversation conversation);
    Page<Message> getMessagesByConversation(Conversation conversation, Pageable pageable);
    List<Message> getMessagesBySender(User sender);
    Message getMessageById(Long messageId);
    List<Message> getUnreadMessages(Conversation conversation, User user);
    
    // Message status updates
    void markMessageAsRead(Message message);
    void markMessageAsDelivered(Message message);
    void markAllAsRead(Conversation conversation, User user);
    void updateMessageStatus(Long messageId, MessageStatus status);
    
    // Message search and filtering
    List<Message> searchMessages(Conversation conversation, String query);
    Page<Message> searchMessages(Conversation conversation, String query, Pageable pageable);
    
    // Message deletion
    void deleteMessage(Long messageId, User user);
    void deleteMessagesByConversation(Conversation conversation);
    
    // DTO conversion
    MessageDto convertToMessageDto(Message message, User currentUser);
    List<MessageDto> convertToMessageDtos(List<Message> messages, User currentUser);
    
    // Message statistics
    long countMessagesByConversation(Conversation conversation);
    long countUnreadMessages(Conversation conversation, User user);
    Message getLastMessage(Conversation conversation);
}