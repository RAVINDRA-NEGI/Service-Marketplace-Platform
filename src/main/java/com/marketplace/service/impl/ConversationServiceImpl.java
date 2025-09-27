package com.marketplace.service.impl;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.marketplace.dto.ConversationDto;
import com.marketplace.dto.MessageDto;
import com.marketplace.dto.SendMessageDto;
import com.marketplace.enums.MessageStatus;
import com.marketplace.enums.MessageType;
import com.marketplace.model.Booking;
import com.marketplace.model.ClientProfile;
import com.marketplace.model.Conversation;
import com.marketplace.model.Message;
import com.marketplace.model.MessageFile;
import com.marketplace.model.ProfessionalProfile;
import com.marketplace.model.User;
import com.marketplace.repository.ClientProfileRepository;
import com.marketplace.repository.ConversationRepository;
import com.marketplace.repository.MessageFileRepository;
import com.marketplace.repository.MessageRepository;
import com.marketplace.repository.ProfessionalProfileRepository;
import com.marketplace.repository.UserRepository;
import com.marketplace.service.ConversationService;
import com.marketplace.service.FileService;
import com.marketplace.service.MessageService;
import com.marketplace.util.MessagingConstants;

@Service
@Transactional
public class ConversationServiceImpl implements ConversationService {

    private static final Logger logger = LoggerFactory.getLogger(ConversationServiceImpl.class);

    @Autowired
    private ConversationRepository conversationRepository;

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private MessageFileRepository messageFileRepository;

    @Autowired
    private UserRepository userRepository;

    
    @Autowired
    private ClientProfileRepository clientProfileRepository;

    @Autowired
    private ProfessionalProfileRepository professionalProfileRepository;

    @Autowired
    private MessageService messageService;

    @Autowired
    private FileService fileService;

    // In-memory storage for online users (use Redis in production)
    private final ConcurrentHashMap<String, LocalDateTime> onlineUsers = new ConcurrentHashMap<>();

    @Override
    public ConversationDto createOrGetConversation(ClientProfile client, Long professionalId, Long bookingId) {
        logger.info("Creating or getting conversation for client {} and professional {}", 
                   client.getId(), professionalId);

        // Check if conversation already exists
        ProfessionalProfile professional = professionalProfileRepository.findById(professionalId)
                .orElseThrow(() -> new RuntimeException("Professional not found"));

        Optional<Conversation> existingConversation = conversationRepository.findByClientAndProfessional(client.getUser(), professional);
        
        if (existingConversation.isPresent()) {
            Conversation conversation = existingConversation.get();
            logger.info("Existing conversation found: {}", conversation.getId());
            return convertToDto(conversation, client.getUser());
        }

        // Create new conversation
        Conversation conversation = new Conversation(client, professional);
        if (bookingId != null) {
            Booking booking = new Booking(); // This would be fetched from database in real implementation
            booking.setId(bookingId);
            conversation.setBooking(booking);
        }

        Conversation savedConversation = conversationRepository.save(conversation);
        logger.info("New conversation created: {}", savedConversation.getId());
        
        return convertToDto(savedConversation, client.getUser());
    }

    @Override
    @Transactional(readOnly = true)
    public Conversation getConversationByIdAndUser(Long conversationId, User user) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new RuntimeException(MessagingConstants.ERROR_CONVERSATION_NOT_FOUND));

        // Check if user has access to this conversation
        if (!conversation.getClient().getId().equals(user.getId()) && 
            !conversation.getProfessional().getUser().getId().equals(user.getId())) {
            throw new RuntimeException(MessagingConstants.ERROR_ACCESS_DENIED);
        }

        return conversation;
    }

    @Override
    @Transactional(readOnly = true)
    public ConversationDto getConversationByIdAndUserDto(Long conversationId, User user) {
        Conversation conversation = getConversationByIdAndUser(conversationId, user);
        return convertToDto(conversation, user);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ConversationDto> getConversationsByClient(ClientProfile client, String status, Pageable pageable) {
        List<Conversation> conversations;
        
        if (status.equals("active")) {
            conversations = conversationRepository.findByClientAndIsActiveTrueOrderByLastMessageSentAtDesc(client.getUser());
        } else {
            conversations = conversationRepository.findByClientOrderByLastMessageSentAtDesc(client.getUser());
        }

        // Convert to DTOs
        List<ConversationDto> dtoList = conversations.stream()
                .map(conv -> convertToDto(conv, client.getUser()))
                .collect(Collectors.toList());

        // Create page manually since we're filtering in memory
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), dtoList.size());
        
        List<ConversationDto> pageContent = dtoList.subList(start, end);
        return new PageImpl<>(pageContent, pageable, dtoList.size());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ConversationDto> getConversationsByProfessional(ProfessionalProfile professional, String status, Pageable pageable) {
        List<Conversation> conversations;
        
        if (status.equals("active")) {
            conversations = conversationRepository.findByProfessionalUserAndIsActiveTrueOrderByLastMessageSentAtDesc(professional.getUser());
        } else {
            conversations = conversationRepository.findByProfessionalUserOrderByLastMessageSentAtDesc(professional.getUser());
        }

        // Convert to DTOs
        List<ConversationDto> dtoList = conversations.stream()
                .map(conv -> convertToDto(conv, professional.getUser()))
                .collect(Collectors.toList());

        // Create page manually since we're filtering in memory
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), dtoList.size());
        
        List<ConversationDto> pageContent = dtoList.subList(start, end);
        return new PageImpl<>(pageContent, pageable, dtoList.size());
    }

    @Override
    public void markConversationAsRead(Long conversationId, User user) {
        Conversation conversation = getConversationByIdAndUser(conversationId, user);
        
        // Reset unread count for the user
        conversation.resetUnreadCount(user);
        conversationRepository.save(conversation);
        
        // Mark all unread messages as read
        List<Message> unreadMessages = messageRepository.findUnreadMessages(
            conversation, user, MessageStatus.READ);
        
        for (Message message : unreadMessages) {
            messageService.markMessageAsRead(message);
        }
        
        logger.info("Conversation {} marked as read for user {}", conversationId, user.getId());
    }

    @Override
    public void closeConversation(Long conversationId, User user) {
        Conversation conversation = getConversationByIdAndUser(conversationId, user);
        
        // Only the client can close the conversation
        if (!conversation.getClient().getId().equals(user.getId())) {
            throw new RuntimeException(MessagingConstants.ERROR_ACCESS_DENIED);
        }
        
        conversation.setIsClosed(true);
        conversation.setIsActive(false);
        conversationRepository.save(conversation);
        
        logger.info("Conversation {} closed by user {}", conversationId, user.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ConversationDto> getRecentConversations(ClientProfile client) {
        Pageable pageable = PageRequest.of(0, 10);
        Page<ConversationDto> page = getConversationsByClient(client, "active", pageable);
        return page.getContent();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ConversationDto> getRecentConversations(ProfessionalProfile professional) {
        Pageable pageable = PageRequest.of(0, 10);
        Page<ConversationDto> page = getConversationsByProfessional(professional, "active", pageable);
        return page.getContent();
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getUnreadCountsForClient(ClientProfile client) {
        List<Conversation> conversations = conversationRepository.findByClientOrderByLastMessageSentAtDesc(client.getUser());
        int unreadCount = conversations.stream()
                .mapToInt(conv -> conv.getUnreadCountClient())
                .sum();

        Map<String, Object> result = new HashMap<>();
        result.put("totalUnread", unreadCount);
        result.put("conversations", conversations.size());
        
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getUnreadCountsForProfessional(ProfessionalProfile professional) {
        List<Conversation> conversations = conversationRepository.findByProfessionalUserOrderByLastMessageSentAtDesc(professional.getUser());
        int unreadCount = conversations.stream()
                .mapToInt(conv -> conv.getUnreadCountProfessional())
                .sum();

        Map<String, Object> result = new HashMap<>();
        result.put("totalUnread", unreadCount);
        result.put("conversations", conversations.size());
        
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getConversationStatistics(ClientProfile client) {
        List<Conversation> conversations = conversationRepository.findByClientOrderByLastMessageSentAtDesc(client.getUser());

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalConversations", conversations.size());
        stats.put("activeConversations", (int) conversations.stream().filter(Conversation::getIsActive).count());
        stats.put("closedConversations", (int) conversations.stream().filter(conv -> !conv.getIsActive()).count());
        
        // Calculate average messages per conversation
        long totalMessages = conversations.stream()
                .mapToLong(conv -> messageRepository.countByConversation(conv))
                .sum();
        double avgMessages = conversations.isEmpty() ? 0 : (double) totalMessages / conversations.size();
        stats.put("averageMessagesPerConversation", avgMessages);

        return stats;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getConversationStatistics(ProfessionalProfile professional) {
        List<Conversation> conversations = conversationRepository.findByProfessionalUserOrderByLastMessageSentAtDesc(professional.getUser());

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalConversations", conversations.size());
        stats.put("activeConversations", (int) conversations.stream().filter(Conversation::getIsActive).count());
        stats.put("closedConversations", (int) conversations.stream().filter(conv -> !conv.getIsActive()).count());
        
        // Calculate average messages per conversation
        long totalMessages = conversations.stream()
                .mapToLong(conv -> messageRepository.countByConversation(conv))
                .sum();
        double avgMessages = conversations.isEmpty() ? 0 : (double) totalMessages / conversations.size();
        stats.put("averageMessagesPerConversation", avgMessages);

        return stats;
    }

    @Override
    public boolean isUserOnline(Long userId) {
        Optional<User> user = userRepository.findById(userId);
        return user.map(User::getUsername).map(onlineUsers::containsKey).orElse(false);
    }

    @Override
    public Message processSendMessage(SendMessageDto sendMessageDto, User sender, Conversation conversation) {
        try {
            // Create message
            MessageType messageType = MessageType.TEXT;
            if (sendMessageDto.getFile() != null && !sendMessageDto.getFile().isEmpty()) {
                messageType = MessageType.PHOTO;
            } else if (sendMessageDto.getFileUrl() != null) {
                messageType = MessageType.PHOTO;
            }

            Message message = new Message(conversation, sender, sendMessageDto.getContent(), messageType);
            Message savedMessage = messageService.saveMessage(message);

            // Handle file upload if present
            if (sendMessageDto.getFile() != null && !sendMessageDto.getFile().isEmpty()) {
                MessageFile messageFile = fileService.uploadFile(sendMessageDto.getFile(), savedMessage.getId());
                savedMessage.setFiles(Arrays.asList(messageFile));
            }

            // Update conversation with last message
            conversation.setLastMessageContent(sendMessageDto.getContent());
            conversation.setLastMessageSentAt(LocalDateTime.now());
            conversation.setLastMessageType(messageType);

            // Update unread counts
            if (sender.getId().equals(conversation.getClient().getId())) {
                conversation.setUnreadCountProfessional(conversation.getUnreadCountProfessional() + 1);
            } else if (sender.getId().equals(conversation.getProfessional().getUser().getId())) {
                conversation.setUnreadCountClient(conversation.getUnreadCountClient() + 1);
            }

            conversationRepository.save(conversation);

            logger.info("Message {} sent in conversation {}", savedMessage.getId(), conversation.getId());
            return savedMessage;

        } catch (Exception e) {
            logger.error("Error processing send message: ", e);
            throw new RuntimeException("Error sending message: " + e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<MessageDto> getMessagesByConversation(Long conversationId, User user, Pageable pageable) {
        Conversation conversation = getConversationByIdAndUser(conversationId, user);
        
        Page<Message> messagesPage = messageRepository.findByConversationOrderByCreatedAtAsc(conversation, pageable);
        
        List<MessageDto> messageDtos = messagesPage.getContent().stream()
                .map(msg -> messageService.convertToMessageDto(msg, user))
                .collect(Collectors.toList());

        return new PageImpl<>(messageDtos, pageable, messagesPage.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<MessageDto> searchMessages(Long conversationId, String query, Pageable pageable) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new RuntimeException(MessagingConstants.ERROR_CONVERSATION_NOT_FOUND));

        // This would be implemented with a custom query or full-text search
        // For now, we'll do a simple content search
        List<Message> allMessages = messageRepository.findByConversationOrderByCreatedAtAsc(conversation);
        List<Message> filteredMessages = allMessages.stream()
                .filter(msg -> msg.getContent().toLowerCase().contains(query.toLowerCase()))
                .collect(Collectors.toList());

        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), filteredMessages.size());
        
        List<Message> pageContent = filteredMessages.subList(start, end);
        List<MessageDto> messageDtos = pageContent.stream()
                .map(msg -> messageService.convertToMessageDto(msg, null)) // Current user context not needed for search
                .collect(Collectors.toList());

        return new PageImpl<>(messageDtos, pageable, filteredMessages.size());
    }

    @Override
    public MessageDto convertToMessageDto(Message message, User currentUser) {
        return messageService.convertToMessageDto(message, currentUser);
    }

    @Override
    public MessageFile createMessageFile(Long messageId, String originalFilename, String storedFilename, 
                                       String filePath, String fileUrl, Long fileSize, String contentType) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException(MessagingConstants.ERROR_MESSAGE_NOT_FOUND));

        MessageFile messageFile = new MessageFile(message, originalFilename, storedFilename, filePath, fileUrl, fileSize, contentType);
        return messageFileRepository.save(messageFile);
    }

    @Override
    @Transactional(readOnly = true)
    public MessageFile getMessageFileById(Long fileId) {
        return messageFileRepository.findById(fileId)
                .orElseThrow(() -> new RuntimeException("Message file not found"));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasAccessToFile(MessageFile messageFile, User user) {
        // Check if user has access to the conversation containing this file
        try {
            Message message = messageFile.getMessage();
            Conversation conversation = message.getConversation();
            
            return conversation.getClient().getId().equals(user.getId()) ||
                   conversation.getProfessional().getUser().getId().equals(user.getId());
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean canDeleteFile(MessageFile messageFile, User user) {
        // Only the sender of the message can delete the file
        try {
            Message message = messageFile.getMessage();
            return message.getSender().getId().equals(user.getId());
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void deleteMessageFile(Long fileId) {
        MessageFile messageFile = getMessageFileById(fileId);
        
        // Delete from filesystem
        fileService.deleteFileFromSystem(messageFile.getFilePath());
        
        // Delete from database
        messageFileRepository.deleteById(fileId);
    }

    // Helper method to convert Conversation to ConversationDto
    private ConversationDto convertToDto(Conversation conversation, User currentUser) {
        ConversationDto dto = new ConversationDto();
        dto.setId(conversation.getId());
        dto.setClientId(conversation.getClient().getId());
        dto.setClientName(conversation.getClient().getFullName());
        dto.setClientAvatarUrl(conversation.getClient().getProfilePhotoUrl());
        dto.setProfessionalId(conversation.getProfessional().getId());
        dto.setProfessionalName(conversation.getProfessional().getUser().getFullName());
        dto.setProfessionalAvatarUrl(conversation.getProfessional().getProfilePhotoUrl());
        
        if (conversation.getBooking() != null) {
            dto.setBookingId(conversation.getBooking().getId());
        }
        
        dto.setLastMessageContent(conversation.getLastMessageContent());
        dto.setLastMessageType(conversation.getLastMessageType());
        dto.setLastMessageSentAt(conversation.getLastMessageSentAt());
        
        // Set unread count based on current user
        if (currentUser.getId().equals(conversation.getClient().getId())) {
            dto.setUnreadCount(conversation.getUnreadCountClient());
        } else if (currentUser.getId().equals(conversation.getProfessional().getUser().getId())) {
            dto.setUnreadCount(conversation.getUnreadCountProfessional());
        }
        
        dto.setIsActive(conversation.getIsActive());
        dto.setIsClosed(conversation.getIsClosed());
        dto.setCreatedAt(conversation.getCreatedAt());
        dto.setUpdatedAt(conversation.getUpdatedAt());
        dto.setIsOnline(isUserOnline(conversation.getClient().getId()) || 
                       isUserOnline(conversation.getProfessional().getUser().getId()));
        
        return dto;
    }
}