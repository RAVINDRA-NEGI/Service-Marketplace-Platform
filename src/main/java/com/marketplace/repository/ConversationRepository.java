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
    @Query("SELECT c FROM Conversation c WHERE c.client = :client ORDER BY c.updatedAt DESC")
    List<Conversation> findByClientOrderByUpdatedAtDesc(@Param("client") User client);
    
    // Find conversations for a professional
    @Query("SELECT c FROM Conversation c WHERE c.professional.user = :user ORDER BY c.updatedAt DESC")
    List<Conversation> findByProfessional_UserOrderByUpdatedAtDesc(@Param("user") User user);
    
    // Find active conversations for a client
    @Query("SELECT c FROM Conversation c WHERE c.client = :client AND c.isActive = true ORDER BY c.updatedAt DESC")
    List<Conversation> findActiveByClientOrderByUpdatedAtDesc(@Param("client") User client);
    
    // Find active conversations for a professional
    @Query("SELECT c FROM Conversation c WHERE c.professional.user = :user AND c.isActive = true ORDER BY c.updatedAt DESC")
    List<Conversation> findActiveByProfessional_UserOrderByUpdatedAtDesc(@Param("user") User user);
    
    // Count unread messages for client
    @Query("SELECT SUM(c.unreadCountClient) FROM Conversation c WHERE c.client = :client AND c.isActive = true")
    Integer countUnreadMessagesForClient(@Param("client") User client);
    
    // Count unread messages for professional
    @Query("SELECT SUM(c.unreadCountProfessional) FROM Conversation c WHERE c.professional.user = :user AND c.isActive = true")
    Integer countUnreadMessagesForProfessional(@Param("user") User user);
    
    // Check if conversation exists between client and professional
    boolean existsByClientAndProfessional(User client, ProfessionalProfile professional);
}