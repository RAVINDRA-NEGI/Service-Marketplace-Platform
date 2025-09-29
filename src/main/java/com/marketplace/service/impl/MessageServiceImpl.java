package com.marketplace.service.impl;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.marketplace.dto.SendMessageDto;
import com.marketplace.enums.MessageStatus;
import com.marketplace.enums.MessageType;
import com.marketplace.exception.AccessDeniedException;
import com.marketplace.exception.MessageNotFoundException;
import com.marketplace.model.Conversation;
import com.marketplace.model.Message;
import com.marketplace.model.User;
import com.marketplace.repository.MessageRepository;
import com.marketplace.service.FileUploadService;
import com.marketplace.service.MessageService;

@Service
@Transactional
public class MessageServiceImpl implements MessageService {

    private static final Logger logger = LoggerFactory.getLogger(MessageServiceImpl.class);

    private final MessageRepository messageRepository;
    private final FileUploadService fileUploadService;

    public MessageServiceImpl(MessageRepository messageRepository, FileUploadService fileUploadService) {
        this.messageRepository = messageRepository;
        this.fileUploadService = fileUploadService;
    }

    @Override
    public Message createMessage(Conversation conversation, User sender, SendMessageDto sendMessageDto) {
        // Validate access
        if (!hasAccessToConversation(conversation, sender)) {
            throw new AccessDeniedException("User does not have access to this conversation");
        }

        MessageType messageType = sendMessageDto.getMessageType();
        String content = sendMessageDto.getContent();

        // Handle file upload if present
        if (sendMessageDto.getFile() != null && !sendMessageDto.getFile().isEmpty()) {
            messageType = MessageType.PHOTO; // Override to photo type
            // File will be handled separately in the controller or here
        }

        Message message = new Message(conversation, sender, content, messageType);
        Message savedMessage = messageRepository.save(message);

        // Update conversation's last message
        updateConversationLastMessage(conversation, savedMessage);

        logger.info("Created message with ID: {} in conversation: {}", savedMessage.getId(), conversation.getId());
        return savedMessage;
    }

    @Override
    public Message createSystemMessage(Conversation conversation, String content) {
        // System messages don't have a specific sender, but we need to set one
        // This could be the system user or a default admin user
        // For now, we'll assume a system user exists
        User systemUser = getSystemUser(); // You'll need to implement this
        Message message = new Message(conversation, systemUser, content, MessageType.SYSTEM);
        Message savedMessage = messageRepository.save(message);

        // Update conversation's last message
        updateConversationLastMessage(conversation, savedMessage);

        logger.info("Created system message with ID: {} in conversation: {}", savedMessage.getId(), conversation.getId());
        return savedMessage;
    }

    @Override
    @Transactional(readOnly = true)
    public Message getMessageById(Long messageId) {
        return messageRepository.findById(messageId)
                .orElseThrow(() -> new MessageNotFoundException("Message not found with ID: " + messageId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Message> getMessagesByConversation(Conversation conversation) {
        return messageRepository.findByConversationOrderByCreatedAtAsc(conversation);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Message> getMessagesByConversation(Conversation conversation, Pageable pageable) {
        return messageRepository.findByConversationOrderByCreatedAtAsc(conversation, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Message> getRecentMessages(Conversation conversation, int offset, int limit) {
        // This is a simplified version - you might want to implement proper pagination
        List<Message> allMessages = messageRepository.findByConversationOrderByCreatedAtAsc(conversation);
        int startIndex = Math.max(0, allMessages.size() - limit - offset);
        int endIndex = Math.min(allMessages.size() - offset, allMessages.size());
        
        if (startIndex >= endIndex) {
            return List.of();
        }
        
        return allMessages.subList(startIndex, endIndex);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Message> getMessagesBySender(User sender) {
        return messageRepository.findBySenderOrderByCreatedAtDesc(sender);
    }

    @Override
    public void markMessagesAsRead(Conversation conversation, User user) {
        messageRepository.markMessagesAsReadForUser(conversation, user);
        logger.info("Marked messages as read for user {} in conversation {}", user.getId(), conversation.getId());
    }

    @Override
    public void markMessagesAsDelivered(Conversation conversation, User user) {
        messageRepository.markMessagesAsDeliveredForUser(conversation, user);
        logger.info("Marked messages as delivered for user {} in conversation {}", user.getId(), conversation.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public long countMessagesByConversation(Conversation conversation) {
        return messageRepository.countByConversation(conversation);
    }

    @Override
    @Transactional(readOnly = true)
    public long countUnreadMessages(Conversation conversation, User user) {
        return messageRepository.countUnreadMessagesForUser(conversation, user);
    }

    @Override
    public void deleteMessage(Message message) {
        // Verify access before deletion
        // You might want to implement access control here
        messageRepository.delete(message);
        logger.info("Deleted message with ID: {}", message.getId());
    }

    @Override
    public void deleteMessagesByConversation(Conversation conversation) {
        messageRepository.deleteByConversation(conversation);
        logger.info("Deleted all messages for conversation: {}", conversation.getId());
    }

    @Override
    public void updateMessageStatus(Long messageId, MessageStatus status) {
        Message message = getMessageById(messageId);
        message.setMessageStatus(status);
        if (status == MessageStatus.READ) {
            message.setReadAt(java.time.LocalDateTime.now());
        } else if (status == MessageStatus.DELIVERED) {
            message.setDeliveredAt(java.time.LocalDateTime.now());
        }
        messageRepository.save(message);
        logger.info("Updated message {} status to: {}", messageId, status);
    }

    @Override
    @Transactional(readOnly = true)
    public Message getLastMessage(Conversation conversation) {
        return messageRepository.findLastMessage(conversation);
    }

    // Helper methods
    private boolean hasAccessToConversation(Conversation conversation, User user) {
        return conversation.getClient().getUser().getId().equals(user.getId()) ||
               conversation.getProfessional().getUser().getId().equals(user.getId());
    }

    private void updateConversationLastMessage(Conversation conversation, Message message) {
        // This would require ConversationService to be injected
        // For now, we'll just log it
        logger.info("Updating conversation {} last message to: {}", 
                   conversation.getId(), message.getContent());
    }

    private User getSystemUser() {
        // Implement system user retrieval
        // This could be a special user in your database
        throw new UnsupportedOperationException("System user retrieval not implemented");
    }
}