package com.marketplace.service.impl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.marketplace.dto.MessageDto;
import com.marketplace.dto.MessageFileDto;
import com.marketplace.dto.SendMessageDto;
import com.marketplace.enums.MessageStatus;
import com.marketplace.enums.MessageType;
import com.marketplace.model.ClientProfile;
import com.marketplace.model.Conversation;
import com.marketplace.model.Message;
import com.marketplace.model.MessageFile;
import com.marketplace.model.ProfessionalProfile;
import com.marketplace.model.User;
import com.marketplace.repository.ClientProfileRepository;
import com.marketplace.repository.MessageFileRepository;
import com.marketplace.repository.MessageRepository;
import com.marketplace.repository.ProfessionalProfileRepository;
import com.marketplace.repository.UserRepository;
import com.marketplace.service.FileService;
import com.marketplace.service.MessageService;

@Service
@Transactional
public class MessageServiceImpl implements MessageService {

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private MessageFileRepository messageFileRepository;

    @Autowired
    private FileService fileService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProfessionalProfileRepository professionalProfileRepository; // Add this

    @Autowired
    private ClientProfileRepository clientProfileRepository; // Add this

    @Override
    public Message createMessage(Conversation conversation, User sender, String content, String messageType) {
        MessageType type = MessageType.TEXT;
        try {
            type = MessageType.valueOf(messageType.toUpperCase());
        } catch (IllegalArgumentException e) {
            // Default to TEXT if invalid type
        }

        Message message = new Message(conversation, sender, content, type);
        return messageRepository.save(message);
    }

    @Override
    public Message processSendMessage(SendMessageDto sendMessageDto, User sender, Conversation conversation) {
        Message message = new Message(conversation, sender, sendMessageDto.getContent(), MessageType.TEXT);
        return saveMessage(message);
    }

    @Override
    public Message saveMessage(Message message) {
        return messageRepository.save(message);
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
    public List<Message> getMessagesBySender(User sender) {
        return messageRepository.findBySenderOrderByCreatedAtDesc(sender);
    }

    @Override
    @Transactional(readOnly = true)
    public Message getMessageById(Long messageId) {
        return messageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Message not found"));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Message> getUnreadMessages(Conversation conversation, User user) {
        return messageRepository.findUnreadMessages(conversation, user, MessageStatus.READ);
    }

    @Override
    public void markMessageAsRead(Message message) {
        message.markAsRead();
        messageRepository.save(message);
    }

    @Override
    public void markMessageAsDelivered(Message message) {
        message.markAsDelivered();
        messageRepository.save(message);
    }

    @Override
    public void markAllAsRead(Conversation conversation, User user) {
        List<Message> unreadMessages = getUnreadMessages(conversation, user);
        for (Message message : unreadMessages) {
            markMessageAsRead(message);
        }
    }

    @Override
    public void updateMessageStatus(Long messageId, MessageStatus status) {
        Message message = getMessageById(messageId);
        if (status == MessageStatus.READ) {
            markMessageAsRead(message);
        } else if (status == MessageStatus.DELIVERED) {
            markMessageAsDelivered(message);
        }
        // SENT status is default, no need to update
    }

    @Override
    @Transactional(readOnly = true)
    public List<Message> searchMessages(Conversation conversation, String query) {
        return messageRepository.findByConversationOrderByCreatedAtAsc(conversation).stream()
                .filter(msg -> msg.getContent().toLowerCase().contains(query.toLowerCase()))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Message> searchMessages(Conversation conversation, String query, Pageable pageable) {
        List<Message> allMessages = messageRepository.findByConversationOrderByCreatedAtAsc(conversation);
        List<Message> filteredMessages = allMessages.stream()
                .filter(msg -> msg.getContent().toLowerCase().contains(query.toLowerCase()))
                .collect(Collectors.toList());

        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), filteredMessages.size());
        
        List<Message> pageContent = filteredMessages.subList(start, end);
        return new PageImpl<>(pageContent, pageable, filteredMessages.size());
    }

    @Override
    public void deleteMessage(Long messageId, User user) {
        Message message = getMessageById(messageId);
        
        // Only the sender can delete their own message
        if (!message.getSender().getId().equals(user.getId())) {
            throw new RuntimeException("Access denied: Cannot delete message");
        }

        // Delete associated files
        List<MessageFile> files = message.getFiles();
        for (MessageFile file : files) {
            fileService.deleteFile(file.getId());
        }

        messageRepository.delete(message);
    }

    @Override
    public void deleteMessagesByConversation(Conversation conversation) {
        List<Message> messages = getMessagesByConversation(conversation);
        for (Message message : messages) {
            deleteMessage(message.getId(), message.getSender());
        }
    }

    @Override
    public MessageDto convertToMessageDto(Message message, User currentUser) {
        MessageDto dto = new MessageDto();
        dto.setId(message.getId());
        dto.setConversationId(message.getConversation().getId());
        dto.setSenderId(message.getSender().getId());
        dto.setSenderName(message.getSender().getFullName());
        
        // FIXED: Dynamic profile photo URL retrieval
        String profilePhotoUrl = getProfilePhotoUrl(message.getSender());
        dto.setSenderAvatarUrl(profilePhotoUrl);
        
        dto.setContent(message.getContent());
        dto.setMessageType(message.getMessageType());
        dto.setMessageStatus(message.getMessageStatus());
        dto.setCreatedAt(message.getCreatedAt());
        dto.setDeliveredAt(message.getDeliveredAt());
        dto.setReadAt(message.getReadAt());
        
        // Set if message is from current user
        if (currentUser != null) {
            dto.setIsFromCurrentUser(message.isFromUser(currentUser));
        }
        
        // Convert files to DTOs
        if (message.getFiles() != null && !message.getFiles().isEmpty()) {
            List<MessageFileDto> fileDtos = message.getFiles().stream()
                    .map(this::convertToFileDto)
                    .collect(Collectors.toList());
            dto.setFiles(fileDtos);
        }
        
        // Set date separator for grouping
        if (message.getCreatedAt() != null) {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime messageDate = message.getCreatedAt();
            
            if (messageDate.toLocalDate().equals(now.toLocalDate())) {
                dto.setDateSeparator("Today");
            } else if (messageDate.toLocalDate().equals(now.toLocalDate().minusDays(1))) {
                dto.setDateSeparator("Yesterday");
            } else {
                dto.setDateSeparator(messageDate.toLocalDate().toString());
            }
        }
        
        return dto;
    }

    @Override
    public List<MessageDto> convertToMessageDtos(List<Message> messages, User currentUser) {
        return messages.stream()
                .map(msg -> convertToMessageDto(msg, currentUser))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public long countMessagesByConversation(Conversation conversation) {
        return messageRepository.countByConversation(conversation);
    }

    @Override
    @Transactional(readOnly = true)
    public long countUnreadMessages(Conversation conversation, User user) {
        return messageRepository.countUnreadMessages(conversation, user);
    }

    @Override
    @Transactional(readOnly = true)
    public Message getLastMessage(Conversation conversation) {
        return messageRepository.findLastMessage(conversation);
    }

    // Dynamic method to get profile photo URL based on user type
    private String getProfilePhotoUrl(User user) {
        try {
            // Check if user is a professional
            ProfessionalProfile professionalProfile = professionalProfileRepository.findByUser(user).orElse(null);
            if (professionalProfile != null && professionalProfile.getProfilePhotoUrl() != null) {
                return professionalProfile.getProfilePhotoUrl();
            }
            
            // Check if user is a client
            ClientProfile clientProfile = clientProfileRepository.findByUser(user).orElse(null);
            if (clientProfile != null && clientProfile.getProfilePhotoUrl() != null) {
                return clientProfile.getProfilePhotoUrl();
            }
            
            // If no profile photo found, return default avatar
            return "/images/default-avatar.png";
            
        } catch (Exception e) {
            // In case of any error, return default avatar
            return "/images/default-avatar.png";
        }
    }

    // Helper method to convert MessageFile to MessageFileDto
    private MessageFileDto convertToFileDto(MessageFile file) {
        MessageFileDto dto = new MessageFileDto();
        dto.setId(file.getId());
        dto.setOriginalFilename(file.getOriginalFilename());
        dto.setStoredFilename(file.getStoredFilename());
        dto.setFileUrl(file.getFileUrl());
        dto.setFileSize(file.getFileSize());
        dto.setContentType(file.getContentType());
        dto.setFileType(file.getFileType());
        dto.setIconClass(fileService.getFileIconClass(file.getFileType()));
        
        // Generate thumbnail URL for images
        if ("photo".equals(file.getFileType())) {
            dto.setThumbnailUrl(fileService.generateThumbnailUrl(file.getStoredFilename()));
        }
        
        return dto;
    }
}