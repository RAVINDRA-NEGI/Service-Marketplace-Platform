package com.marketplace.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.marketplace.model.ClientProfile;
import com.marketplace.model.Conversation;
import com.marketplace.model.ProfessionalProfile;
import com.marketplace.model.User;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, Long> {
    
    // Find conversation by client profile and professional profile (for creating new conversations)
    Optional<Conversation> findByClientAndProfessional(ClientProfile client, ProfessionalProfile professional);
    
    // Find conversations for a logged-in client user
    @Query("SELECT c FROM Conversation c WHERE c.client.user = :user ORDER BY c.lastMessageSentAt DESC")
    List<Conversation> findConversationsForClient(@Param("user") User user);
    
    // Find conversations for a logged-in professional user
    List<Conversation> findByProfessionalUserOrderByLastMessageSentAtDesc(User professional);
    
    // Find active conversations for a logged-in client user
    @Query("SELECT c FROM Conversation c WHERE c.client.user = :user AND c.isActive = true ORDER BY c.lastMessageSentAt DESC")
    List<Conversation> findActiveConversationsForClient(@Param("user") User user);
    
    // Find active conversations for a logged-in professional user
    List<Conversation> findByProfessionalUserAndIsActiveTrueOrderByLastMessageSentAtDesc(User professional);
    
    // Find conversations with unread messages for a logged-in client user
    @Query("SELECT c FROM Conversation c WHERE c.client.user = :user AND c.unreadCountClient > 0")
    List<Conversation> findConversationsWithUnreadForClient(@Param("user") User user);
    
    // Find conversations with unread messages for a logged-in professional user
    @Query("SELECT c FROM Conversation c WHERE c.professional.user = :user AND c.unreadCountProfessional > 0")
    List<Conversation> findConversationsWithUnreadForProfessional(@Param("user") User user);
    
    // Find conversation by booking
    Optional<Conversation> findByBookingId(Long bookingId);
    
    // Count unread messages for a logged-in user in a conversation
    @Query("SELECT CASE WHEN c.client.user.id = :user.id THEN c.unreadCountProfessional ELSE c.unreadCountClient END FROM Conversation c WHERE c.id = :conversationId")
    Integer countUnreadForUser(@Param("conversationId") Long conversationId, @Param("user") User user);
    
    // Update unread count for client user
    @Query("UPDATE Conversation c SET c.unreadCountClient = 0 WHERE c.id = :conversationId AND c.client.user = :user")
    void resetUnreadCountForClientUser(@Param("conversationId") Long conversationId, @Param("user") User user);
    
    // Update unread count for professional user
    @Query("UPDATE Conversation c SET c.unreadCountProfessional = 0 WHERE c.id = :conversationId AND c.professional.user = :user")
    void resetUnreadCountForProfessionalUser(@Param("conversationId") Long conversationId, @Param("user") User user);
    
    // Find conversations by client profile (when we have the profile, not user)
    List<Conversation> findByClientOrderByLastMessageSentAtDesc(ClientProfile client);
    
    // Find conversations by professional profile (when we have the profile, not user)
    List<Conversation> findByProfessionalOrderByLastMessageSentAtDesc(ProfessionalProfile professional);
}