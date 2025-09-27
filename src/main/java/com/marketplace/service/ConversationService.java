package com.marketplace.service;

import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.marketplace.dto.ConversationDto;
import com.marketplace.dto.MessageDto;
import com.marketplace.dto.SendMessageDto;
import com.marketplace.model.ClientProfile;
import com.marketplace.model.Conversation;
import com.marketplace.model.Message;
import com.marketplace.model.MessageFile;
import com.marketplace.model.ProfessionalProfile;
import com.marketplace.model.User;

public interface ConversationService {
    
    // Conversation management
    ConversationDto createOrGetConversation(ClientProfile client, Long professionalId, Long bookingId);
    Conversation getConversationByIdAndUser(Long conversationId, User user);
    ConversationDto getConversationByIdAndUserDto(Long conversationId, User user);
    Page<ConversationDto> getConversationsByClient(ClientProfile client, String status, Pageable pageable);
    Page<ConversationDto> getConversationsByProfessional(ProfessionalProfile professional, String status, Pageable pageable);
    void markConversationAsRead(Long conversationId, User user);
    void closeConversation(Long conversationId, User user);
    List<ConversationDto> getRecentConversations(ClientProfile client);
    List<ConversationDto> getRecentConversations(ProfessionalProfile professional);
    Map<String, Object> getUnreadCountsForClient(ClientProfile client);
    Map<String, Object> getUnreadCountsForProfessional(ProfessionalProfile professional);
    Map<String, Object> getConversationStatistics(ClientProfile client);
    Map<String, Object> getConversationStatistics(ProfessionalProfile professional);
    boolean isUserOnline(Long userId);
    
    // Message operations
    Message processSendMessage(SendMessageDto sendMessageDto, User sender, Conversation conversation);
    Page<MessageDto> getMessagesByConversation(Long conversationId, User user, Pageable pageable);
    Page<MessageDto> searchMessages(Long conversationId, String query, Pageable pageable);
    MessageDto convertToMessageDto(Message message, User currentUser);
    
    // File operations
    MessageFile createMessageFile(Long messageId, String originalFilename, String storedFilename, 
                                String filePath, String fileUrl, Long fileSize, String contentType);
    MessageFile getMessageFileById(Long fileId);
    boolean hasAccessToFile(MessageFile messageFile, User user);
    boolean canDeleteFile(MessageFile messageFile, User user);
    void deleteMessageFile(Long fileId);
}