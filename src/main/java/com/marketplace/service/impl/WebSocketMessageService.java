package com.marketplace.service.impl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.marketplace.dto.MessageDto;
import com.marketplace.dto.ReadReceiptMessage;
import com.marketplace.dto.TypingIndicatorMessage;
import com.marketplace.enums.MessageStatus;
import com.marketplace.model.Conversation;
import com.marketplace.model.Message;
import com.marketplace.model.User;
import com.marketplace.repository.ConversationRepository;
import com.marketplace.repository.MessageFileRepository;
import com.marketplace.repository.MessageRepository;
import com.marketplace.repository.ProfessionalProfileRepository;
import com.marketplace.repository.UserRepository;
import com.marketplace.security.service.UserDetailsImpl;

@Service
public class WebSocketMessageService {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketMessageService.class);

    @Autowired
    private SimpMessageSendingOperations messagingTemplate;

    @Autowired
    private ConversationRepository conversationRepository;

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private MessageFileRepository messageFileRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProfessionalProfileRepository professionalProfileRepository;

    // In-memory storage for online users (use Redis in production)
    private final ConcurrentHashMap<String, LocalDateTime> onlineUsers = new ConcurrentHashMap<>();

    public void sendMessage(Long conversationId, MessageDto messageDto) {
        try {
            // Send message to specific conversation
            messagingTemplate.convertAndSend(
                "/topic/conversation/" + conversationId, 
                messageDto
            );
            
            // Send to sender's private queue (for read receipts)
            messagingTemplate.convertAndSendToUser(
                messageDto.getSenderId().toString(),
                "/queue/messages",
                messageDto
            );
            
            logger.info("Message sent to conversation: {} by user: {}", 
                conversationId, messageDto.getSenderId());
        } catch (Exception e) {
            logger.error("Error sending message: ", e);
        }
    }

    public void sendTypingIndicator(Long conversationId, String username, boolean isTyping) {
        try {
            TypingIndicatorMessage indicator = new TypingIndicatorMessage(username, isTyping);
            messagingTemplate.convertAndSend(
                "/topic/conversation/" + conversationId + "/typing", 
                indicator
            );
        } catch (Exception e) {
            logger.error("Error sending typing indicator: ", e);
        }
    }

    public void sendReadReceipt(Long conversationId, Long messageId, String username) {
        try {
            ReadReceiptMessage receipt = new ReadReceiptMessage(messageId, username, LocalDateTime.now());
            messagingTemplate.convertAndSend(
                "/topic/conversation/" + conversationId + "/receipts", 
                receipt
            );
        } catch (Exception e) {
            logger.error("Error sending read receipt: ", e);
        }
    }

    public void markMessageAsRead(Long messageId, Long conversationId, User user) {
        try {
            // Update message status in database
            Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Message not found"));
            
            if (!message.isFromUser(user)) {
                message.markAsRead();
                messageRepository.save(message);
                
                // Notify sender that message was read
                sendReadReceipt(conversationId, messageId, user.getFullName());
            }
        } catch (Exception e) {
            logger.error("Error marking message as read: ", e);
        }
    }

    public void updateConversationLastMessage(Long conversationId, String content, User sender) {
        try {
            Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new RuntimeException("Conversation not found"));
            
            conversation.setLastMessageContent(content);
            conversation.setLastMessageSentAt(LocalDateTime.now());
            
            // Update unread counts
            if (sender.getId().equals(conversation.getClient().getId())) {
                conversation.setUnreadCountProfessional(conversation.getUnreadCountProfessional() + 1);
            } else if (sender.getId().equals(conversation.getProfessional().getUser().getId())) {
                conversation.setUnreadCountClient(conversation.getUnreadCountClient() + 1);
            }
            
            conversationRepository.save(conversation);
        } catch (Exception e) {
            logger.error("Error updating conversation last message: ", e);
        }
    }

    public void updateUserOnlineStatus(String username, boolean online) {
        if (online) {
            onlineUsers.put(username, LocalDateTime.now());
        } else {
            onlineUsers.remove(username);
        }
    }

    public boolean isUserOnline(String username) {
        return onlineUsers.containsKey(username);
    }

    public void markConversationAsRead(Long conversationId, User user) {
        try {
            Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new RuntimeException("Conversation not found"));
            
            // Reset unread count for the user
            conversation.resetUnreadCount(user);
            conversationRepository.save(conversation);
            
            // Mark all unread messages as read
            List<Message> unreadMessages = messageRepository.findUnreadMessages(
                conversation, user, MessageStatus.READ);
            
            for (Message message : unreadMessages) {
                message.markAsRead();
                messageRepository.save(message);
            }
        } catch (Exception e) {
            logger.error("Error marking conversation as read: ", e);
        }
    }

    // Helper method to get current user
    public User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserDetailsImpl) {
            UserDetailsImpl userDetails = (UserDetailsImpl) auth.getPrincipal();
            return userRepository.findById(userDetails.getId()).orElse(null);
        }
        return null;
    }
}