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

import com.marketplace.enums.MessageStatus;
import com.marketplace.model.Conversation;
import com.marketplace.model.Message;
import com.marketplace.model.User;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {
    
    // Find messages by conversation
    List<Message> findByConversationOrderByCreatedAtAsc(Conversation conversation);
    
    // Find messages by conversation with pagination
    Page<Message> findByConversationOrderByCreatedAtAsc(Conversation conversation, Pageable pageable);
    
    // Find messages by conversation and sender user
    List<Message> findByConversationAndSenderOrderByCreatedAtAsc(Conversation conversation, User sender);
    
    // Find recent messages for a conversation
    @Query("SELECT m FROM Message m WHERE m.conversation = :conversation ORDER BY m.createdAt DESC LIMIT :limit")
    List<Message> findRecentMessages(@Param("conversation") Conversation conversation, @Param("limit") int limit);
    
    // Find messages by conversation and date range
    List<Message> findByConversationAndCreatedAtBetweenOrderByCreatedAtAsc(
        Conversation conversation, LocalDateTime start, LocalDateTime end);
    
    // Find messages by conversation and message status
    List<Message> findByConversationAndMessageStatusOrderByCreatedAtAsc(
        Conversation conversation, MessageStatus status);
    
    // Find messages sent by a user
    List<Message> findBySenderOrderByCreatedAtDesc(User sender);
    
    // Find unread messages for a user in a conversation
    @Query("SELECT m FROM Message m WHERE m.conversation = :conversation AND m.sender != :user AND m.messageStatus != 'READ'")
    List<Message> findUnreadMessagesForUser(@Param("conversation") Conversation conversation, 
                                          @Param("user") User user);
    
    // Mark messages as delivered for a user in a conversation
    @Query("UPDATE Message m SET m.messageStatus = 'DELIVERED', m.deliveredAt = CURRENT_TIMESTAMP WHERE m.conversation = :conversation AND m.sender != :user AND m.messageStatus = 'SENT'")
    void markMessagesAsDeliveredForUser(@Param("conversation") Conversation conversation, @Param("user") User user);
    
    // Mark messages as read for a user in a conversation
    @Query("UPDATE Message m SET m.messageStatus = 'READ', m.readAt = CURRENT_TIMESTAMP WHERE m.conversation = :conversation AND m.sender != :user AND m.messageStatus != 'READ'")
    void markMessagesAsReadForUser(@Param("conversation") Conversation conversation, @Param("user") User user);
    
    // Count messages for a conversation
    long countByConversation(Conversation conversation);
    
    // Count unread messages for a user in a conversation
    @Query("SELECT COUNT(m) FROM Message m WHERE m.conversation = :conversation AND m.sender != :user AND m.messageStatus != 'READ'")
    long countUnreadMessagesForUser(@Param("conversation") Conversation conversation, @Param("user") User user);
    
    // Find last message for a conversation
    @Query("SELECT m FROM Message m WHERE m.conversation = :conversation ORDER BY m.createdAt DESC LIMIT 1")
    Message findLastMessage(@Param("conversation") Conversation conversation);

    @Modifying
    @Query("DELETE FROM Message m WHERE m.conversation = :conversation")
    void deleteByConversation(@Param("conversation") Conversation conversation);
}