package com.marketplace.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.marketplace.model.Conversation;
import com.marketplace.model.ProfessionalProfile;
import com.marketplace.model.User;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, Long> {
    
    // Find conversation by client and professional
    Optional<Conversation> findByClientAndProfessional(User client, ProfessionalProfile professional);
    
    // Find conversations for a client
    List<Conversation> findByClientOrderByLastMessageSentAtDesc(User client);
    
    // Find conversations for a professional
    List<Conversation> findByProfessionalUserOrderByLastMessageSentAtDesc(User professional);
    
    // Find active conversations for a client
    List<Conversation> findByClientAndIsActiveTrueOrderByLastMessageSentAtDesc(User client);
    
    // Find active conversations for a professional
    List<Conversation> findByProfessionalUserAndIsActiveTrueOrderByLastMessageSentAtDesc(User professional);
    
    // Find conversations with unread messages for client
    @Query("SELECT c FROM Conversation c WHERE c.client = :user AND c.unreadCountClient > 0")
    List<Conversation> findConversationsWithUnreadForClient(@Param("user") User user);
    
    // Find conversations with unread messages for professional
    @Query("SELECT c FROM Conversation c WHERE c.professional.user = :user AND c.unreadCountProfessional > 0")
    List<Conversation> findConversationsWithUnreadForProfessional(@Param("user") User user);
    
    // Find conversation by booking
    Optional<Conversation> findByBookingId(Long bookingId);
    
    // Count unread messages for a user in a conversation
    @Query("SELECT c.unreadCountClient FROM Conversation c WHERE c.id = :conversationId AND c.professional.user = :user")
    Integer countUnreadForProfessional(@Param("conversationId") Long conversationId, @Param("user") User user);
    
    @Query("SELECT c.unreadCountProfessional FROM Conversation c WHERE c.id = :conversationId AND c.client = :user")
    Integer countUnreadForClient(@Param("conversationId") Long conversationId, @Param("user") User user);
    
    // Update unread count
    @Query("UPDATE Conversation c SET c.unreadCountClient = 0 WHERE c.id = :conversationId AND c.professional.user = :user")
    void resetUnreadCountForProfessional(@Param("conversationId") Long conversationId, @Param("user") User user);
    
    @Query("UPDATE Conversation c SET c.unreadCountProfessional = 0 WHERE c.id = :conversationId AND c.client = :user")
    void resetUnreadCountForClient(@Param("conversationId") Long conversationId, @Param("user") User user);
}