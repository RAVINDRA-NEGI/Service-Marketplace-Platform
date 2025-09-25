package com.marketplace.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.marketplace.model.Conversation;
import com.marketplace.model.Message;
import com.marketplace.model.User;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {
    
    // Find messages by conversation
    @Query("SELECT m FROM Message m WHERE m.conversation = :conversation ORDER BY m.createdAt ASC")
    List<Message> findByConversationOrderByCreatedAtAsc(@Param("conversation") Conversation conversation);
    
    // Find paginated messages by conversation
    Page<Message> findByConversationOrderByCreatedAtAsc(Conversation conversation, Pageable pageable);
    
    // Find recent messages (for initial load)
    @Query("SELECT m FROM Message m WHERE m.conversation = :conversation AND m.createdAt > :since ORDER BY m.createdAt ASC")
    List<Message> findRecentMessages(@Param("conversation") Conversation conversation, @Param("since") LocalDateTime since);
    
    // Count unread messages for a user in a conversation
    @Query("SELECT COUNT(m) FROM Message m WHERE m.conversation = :conversation AND m.isRead = false AND m.sender != :user")
    Long countUnreadMessages(@Param("conversation") Conversation conversation, @Param("user") User user);
    
    // Mark messages as read
    @Modifying
    @Query("UPDATE Message m SET m.isRead = true, m.readAt = CURRENT_TIMESTAMP WHERE m.conversation = :conversation AND m.isRead = false AND m.sender != :user")
    void markAsRead(@Param("conversation") Conversation conversation, @Param("user") User user);
    
    // Find unread messages for a user
    @Query("SELECT m FROM Message m WHERE m.conversation = :conversation AND m.isRead = false AND m.sender != :user ORDER BY m.createdAt ASC")
    List<Message> findUnreadMessages(@Param("conversation") Conversation conversation, @Param("user") User user);
    
    // Find messages by sender
    List<Message> findBySenderOrderByCreatedAtAsc(User sender);
    
    // Find messages by date range
    @Query("SELECT m FROM Message m WHERE m.conversation = :conversation AND m.createdAt BETWEEN :startDate AND :endDate ORDER BY m.createdAt ASC")
    List<Message> findByConversationAndDateRange(
        @Param("conversation") Conversation conversation,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );
    
    // Get last message in conversation
    @Query("SELECT m FROM Message m WHERE m.conversation = :conversation ORDER BY m.createdAt DESC LIMIT 1")
    Message findLastMessage(@Param("conversation") Conversation conversation);
}