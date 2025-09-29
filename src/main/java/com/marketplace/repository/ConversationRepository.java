package com.marketplace.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.marketplace.model.ClientProfile;
import com.marketplace.model.Conversation;
import com.marketplace.model.ProfessionalProfile;
import com.marketplace.model.User;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, Long> {
    
    /**
     * Find conversation between a specific client and professional.
     * Used to check if conversation already exists before creating a new one.
     */
    Optional<Conversation> findByClientAndProfessional(ClientProfile client, ProfessionalProfile professional);
    
    /**
     * Find all conversations for a client user with optimized fetching.
     * Uses JOIN FETCH to avoid N+1 query problems.
     */
    @Query("SELECT DISTINCT c FROM Conversation c " +
           "JOIN FETCH c.client cp " +
           "JOIN FETCH cp.user " +
           "JOIN FETCH c.professional pp " +
           "JOIN FETCH pp.user " +
           "WHERE cp = :clientProfile " +
           "ORDER BY c.lastMessageSentAt DESC")
    List<Conversation> findByClientOrderByLastMessageSentAtDesc(@Param("clientProfile") ClientProfile clientProfile);
    
    /**
     * Find all conversations for a professional user with optimized fetching.
     * Uses JOIN FETCH to avoid N+1 query problems.
     */
    @Query("SELECT DISTINCT c FROM Conversation c " +
           "JOIN FETCH c.client cp " +
           "JOIN FETCH cp.user " +
           "JOIN FETCH c.professional pp " +
           "JOIN FETCH pp.user " +
           "WHERE pp.user = :professionalUser " +
           "ORDER BY c.lastMessageSentAt DESC")
    List<Conversation> findByProfessionalUserOrderByLastMessageSentAtDesc(@Param("professionalUser") User professionalUser);
    
    /**
     * Find active conversations for a client user.
     */
    @Query("SELECT DISTINCT c FROM Conversation c " +
           "JOIN FETCH c.client cp " +
           "JOIN FETCH cp.user " +
           "JOIN FETCH c.professional pp " +
           "JOIN FETCH pp.user " +
           "WHERE cp = :clientProfile " +
           "AND c.isActive = true " +
           "ORDER BY c.lastMessageSentAt DESC")
    List<Conversation> findActiveByClientOrderByLastMessageSentAtDesc(@Param("clientProfile") ClientProfile clientProfile);
    
    /**
     * Find active conversations for a professional user.
     */
    @Query("SELECT DISTINCT c FROM Conversation c " +
           "JOIN FETCH c.client cp " +
           "JOIN FETCH cp.user " +
           "JOIN FETCH c.professional pp " +
           "JOIN FETCH pp.user " +
           "WHERE pp.user = :professionalUser " +
           "AND c.isActive = true " +
           "ORDER BY c.lastMessageSentAt DESC")
    List<Conversation> findActiveByProfessionalUserOrderByLastMessageSentAtDesc(@Param("professionalUser") User professionalUser);
    
    /**
     * Find conversations with unread messages for a client user.
     */
    @Query("SELECT DISTINCT c FROM Conversation c " +
           "JOIN FETCH c.client cp " +
           "JOIN FETCH cp.user " +
           "JOIN FETCH c.professional pp " +
           "JOIN FETCH pp.user " +
           "WHERE cp.user = :user " +
           "AND c.unreadCountClient > 0 " +
           "ORDER BY c.lastMessageSentAt DESC")
    List<Conversation> findConversationsWithUnreadForClient(@Param("user") User user);
    
    /**
     * Find conversations with unread messages for a professional user.
     */
    @Query("SELECT DISTINCT c FROM Conversation c " +
           "JOIN FETCH c.client cp " +
           "JOIN FETCH cp.user " +
           "JOIN FETCH c.professional pp " +
           "JOIN FETCH pp.user " +
           "WHERE pp.user = :user " +
           "AND c.unreadCountProfessional > 0 " +
           "ORDER BY c.lastMessageSentAt DESC")
    List<Conversation> findConversationsWithUnreadForProfessional(@Param("user") User user);
    
    /**
     * Find conversation by booking ID.
     */
    @Query("SELECT c FROM Conversation c " +
           "JOIN FETCH c.client cp " +
           "JOIN FETCH cp.user " +
           "JOIN FETCH c.professional pp " +
           "JOIN FETCH pp.user " +
           "WHERE c.booking.id = :bookingId")
    Optional<Conversation> findByBookingId(@Param("bookingId") Long bookingId);
    
    /**
     * Get unread count for a specific user in a conversation.
     * Returns the appropriate unread count based on user role.
     */
    @Query("SELECT CASE " +
           "WHEN c.client.user.id = :userId THEN c.unreadCountClient " +
           "ELSE c.unreadCountProfessional " +
           "END " +
           "FROM Conversation c " +
           "WHERE c.id = :conversationId")
    Integer countUnreadForUser(@Param("conversationId") Long conversationId, @Param("userId") Long userId);
    
    /**
     * Reset unread count for client in a specific conversation.
     */
    @Modifying
    @Query("UPDATE Conversation c " +
           "SET c.unreadCountClient = 0 " +
           "WHERE c.id = :conversationId " +
           "AND c.client.user.id = :userId")
    void resetUnreadCountForClient(@Param("conversationId") Long conversationId, @Param("userId") Long userId);
    
    /**
     * Reset unread count for professional in a specific conversation.
     */
    @Modifying
    @Query("UPDATE Conversation c " +
           "SET c.unreadCountProfessional = 0 " +
           "WHERE c.id = :conversationId " +
           "AND c.professional.user.id = :userId")
    void resetUnreadCountForProfessional(@Param("conversationId") Long conversationId, @Param("userId") Long userId);
    
    /**
     * Find all conversations for a user (works for both client and professional).
     * Useful for a unified inbox view.
     */
    @Query("SELECT DISTINCT c FROM Conversation c " +
           "JOIN FETCH c.client cp " +
           "JOIN FETCH cp.user cu " +
           "JOIN FETCH c.professional pp " +
           "JOIN FETCH pp.user pu " +
           "WHERE cu.id = :userId OR pu.id = :userId " +
           "ORDER BY c.lastMessageSentAt DESC")
    List<Conversation> findAllByUserId(@Param("userId") Long userId);
    
    /**
     * Count total unread messages for a user across all conversations.
     */
    @Query("SELECT COALESCE(SUM(CASE " +
           "WHEN c.client.user.id = :userId THEN c.unreadCountClient " +
           "WHEN c.professional.user.id = :userId THEN c.unreadCountProfessional " +
           "ELSE 0 END), 0) " +
           "FROM Conversation c " +
           "WHERE c.client.user.id = :userId OR c.professional.user.id = :userId")
    Long countTotalUnreadForUser(@Param("userId") Long userId);
    
    /**
     * Check if user has access to a conversation.
     */
    @Query("SELECT COUNT(c) > 0 FROM Conversation c " +
           "WHERE c.id = :conversationId " +
           "AND (c.client.user.id = :userId OR c.professional.user.id = :userId)")
    boolean userHasAccessToConversation(@Param("conversationId") Long conversationId, @Param("userId") Long userId);
}