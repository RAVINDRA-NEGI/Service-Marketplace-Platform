package com.marketplace.service.impl;

import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.marketplace.enums.Role;
import com.marketplace.exception.ConversationNotFoundException;
import com.marketplace.exception.MessagingException;
import com.marketplace.model.Booking;
import com.marketplace.model.ClientProfile;
import com.marketplace.model.Conversation;
import com.marketplace.model.ProfessionalProfile;
import com.marketplace.model.User;
import com.marketplace.repository.ConversationRepository;
import com.marketplace.service.ConversationService;
import com.marketplace.service.UserService; // Assuming you have this

@Service
@Transactional
public class ConversationServiceImpl implements ConversationService {

    private static final Logger logger = LoggerFactory.getLogger(ConversationServiceImpl.class);

    private final ConversationRepository conversationRepository;
    private final UserService userService;

    public ConversationServiceImpl(ConversationRepository conversationRepository, UserService userService) {
        this.conversationRepository = conversationRepository;
        this.userService = userService;
    }

    @Override
    @Transactional(readOnly = true)
    public Conversation getConversationById(Long conversationId) {
        return conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ConversationNotFoundException("Conversation not found with ID: " + conversationId));
    }

    @Override
    public Conversation createConversation(User client, User professional) {
        // Get ClientProfile and ProfessionalProfile
        ClientProfile clientProfile = client.getClientProfile(); // Assuming getter exists
        ProfessionalProfile professionalProfile = professional.getProfessionalProfile(); // Assuming getter exists
        
        if (clientProfile == null) {
            throw new MessagingException("Client profile not found for user: " + client.getId());
        }
        
        if (professionalProfile == null) {
            throw new MessagingException("Professional profile not found for user: " + professional.getId());
        }

        // Check if conversation already exists
        Optional<Conversation> existingConversation = conversationRepository.findByClientAndProfessional(clientProfile, professionalProfile);
        if (existingConversation.isPresent()) {
            throw new MessagingException("Conversation already exists between client and professional");
        }

        Conversation conversation = new Conversation(clientProfile, professionalProfile);
        Conversation savedConversation = conversationRepository.save(conversation);
        
        logger.info("Created conversation with ID: {} between client {} and professional {}", 
                   savedConversation.getId(), client.getId(), professional.getId());
        
        return savedConversation;
    }

    @Override
    public Conversation createConversation(User client, User professional, Long bookingId) {
        // Get ClientProfile and ProfessionalProfile
        ClientProfile clientProfile = client.getClientProfile();
        ProfessionalProfile professionalProfile = professional.getProfessionalProfile();
        
        if (clientProfile == null) {
            throw new MessagingException("Client profile not found for user: " + client.getId());
        }
        
        if (professionalProfile == null) {
            throw new MessagingException("Professional profile not found for user: " + professional.getId());
        }

        // Check if conversation already exists
        Optional<Conversation> existingConversation = conversationRepository.findByClientAndProfessional(clientProfile, professionalProfile);
        if (existingConversation.isPresent()) {
            return existingConversation.get(); // Return existing conversation
        }

        // Get booking if provided
        Booking booking = null;
        if (bookingId != null) {
            // Assuming you have a BookingService to get the booking
            // booking = bookingService.getBookingById(bookingId);
            // For now, we'll assume booking exists or throw an exception
            throw new MessagingException("Booking service not implemented yet");
        }

        Conversation conversation = new Conversation(clientProfile, professionalProfile, booking);
        Conversation savedConversation = conversationRepository.save(conversation);
        
        logger.info("Created conversation with booking ID: {} between client {} and professional {}", 
                   bookingId, client.getId(), professional.getId());
        
        return savedConversation;
    }

    @Override
    public Conversation getOrCreateConversation(User user1, User user2) {
        // Determine which is client and which is professional
        ClientProfile clientProfile = null;
        ProfessionalProfile professionalProfile = null;
        
        // This logic depends on your user role system
        // You might need to adjust based on your specific implementation
        if (user1.getRole() == Role.CLIENT && user2.getRole() == Role.PROFESSIONAL) {
            clientProfile = user1.getClientProfile();
            professionalProfile = user2.getProfessionalProfile();
        } else if (user1.getRole() == Role.PROFESSIONAL && user2.getRole() == Role.CLIENT) {
            clientProfile = user2.getClientProfile();
            professionalProfile = user1.getProfessionalProfile();
        } else {
            throw new MessagingException("Invalid user roles for conversation: " + user1.getRole() + ", " + user2.getRole());
        }

        if (clientProfile == null || professionalProfile == null) {
            throw new MessagingException("Invalid profiles for conversation");
        }

        // Try to find existing conversation
        Optional<Conversation> existingConversation = conversationRepository.findByClientAndProfessional(clientProfile, professionalProfile);
        if (existingConversation.isPresent()) {
            return existingConversation.get();
        }

        // Create new conversation
        Conversation conversation = new Conversation(clientProfile, professionalProfile);
        return conversationRepository.save(conversation);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Conversation> getConversationsForUser(User user) {
        if (user.getRole() == Role.CLIENT) {
            ClientProfile clientProfile = user.getClientProfile();
            if (clientProfile == null) {
                throw new MessagingException("Client profile not found for user: " + user.getId());
            }
            return conversationRepository.findByClientOrderByLastMessageSentAtDesc(clientProfile);
        } else if (user.getRole() == Role.PROFESSIONAL) {
            return conversationRepository.findByProfessionalUserOrderByLastMessageSentAtDesc(user);
        } else {
            throw new MessagingException("Invalid user role: " + user.getRole());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<Conversation> getConversationsForClient(User client) {
        if (client.getRole() != Role.CLIENT) {
            throw new MessagingException("User is not a client: " + client.getId());
        }
        
        ClientProfile clientProfile = client.getClientProfile();
        if (clientProfile == null) {
            throw new MessagingException("Client profile not found for user: " + client.getId());
        }
        
        return conversationRepository.findByClientOrderByLastMessageSentAtDesc(clientProfile);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Conversation> getConversationsForProfessional(User professional) {
        if (professional.getRole() != Role.PROFESSIONAL) {
            throw new MessagingException("User is not a professional: " + professional.getId());
        }
        
        return conversationRepository.findByProfessionalUserOrderByLastMessageSentAtDesc(professional);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasAccessToConversation(Conversation conversation, User user) {
        boolean isClient = conversation.getClient().getUser().getId().equals(user.getId());
        boolean isProfessional = conversation.getProfessional().getUser().getId().equals(user.getId());
        return isClient || isProfessional;
    }

    @Override
    public void updateLastMessage(Conversation conversation, String content, com.marketplace.enums.MessageType type) {
        conversation.setLastMessageContent(content);
        conversation.setLastMessageType(type);
        conversation.setLastMessageSentAt(java.time.LocalDateTime.now());
        conversationRepository.save(conversation);
    }

    @Override
    public void incrementUnreadCount(Conversation conversation, User user) {
        conversation.incrementUnreadCount(user);
        conversationRepository.save(conversation);
    }

    @Override
    public void resetUnreadCount(Conversation conversation, User user) {
        conversation.resetUnreadCount(user);
        conversationRepository.save(conversation);
    }

    @Override
    @Transactional(readOnly = true)
    public Conversation getConversationByBookingId(Long bookingId) {
        return conversationRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new ConversationNotFoundException("Conversation not found for booking ID: " + bookingId));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean conversationExists(User client, User professional) {
        ClientProfile clientProfile = client.getClientProfile();
        ProfessionalProfile professionalProfile = professional.getProfessionalProfile();
        
        if (clientProfile == null || professionalProfile == null) {
            return false;
        }
        
        return conversationRepository.findByClientAndProfessional(clientProfile, professionalProfile).isPresent();
    }
}