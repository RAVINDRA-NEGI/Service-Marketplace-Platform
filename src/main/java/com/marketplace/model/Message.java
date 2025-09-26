package com.marketplace.model;

import java.time.LocalDateTime;
import java.util.List;

import com.marketplace.enums.MessageStatus;
import com.marketplace.enums.MessageType;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Table(name = "messages")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id", nullable = false)
    @ToString.Exclude
    private Conversation conversation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    @ToString.Exclude
    private User sender;

    @Column(name = "content", length = 2000)
    private String content;

    @Column(name = "message_type")
    @Enumerated(EnumType.STRING)
    private MessageType messageType = MessageType.TEXT;

    @Column(name = "message_status")
    @Enumerated(EnumType.STRING)
    private MessageStatus messageStatus = MessageStatus.SENT;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @OneToMany(mappedBy = "message", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @ToString.Exclude
    private List<MessageFile> files;

    // Constructor
    public Message(Conversation conversation, User sender, String content, MessageType messageType) {
        this.conversation = conversation;
        this.sender = sender;
        this.content = content;
        this.messageType = messageType;
    }

    // Helper methods for status updates
    public void markAsDelivered() {
        this.deliveredAt = LocalDateTime.now();
        this.messageStatus = MessageStatus.DELIVERED;
    }

    public void markAsRead() {
        this.readAt = LocalDateTime.now();
        this.messageStatus = MessageStatus.READ;
    }

    public boolean isRead() {
        return messageStatus == MessageStatus.READ;
    }

    public boolean isDelivered() {
        return messageStatus == MessageStatus.DELIVERED || messageStatus == MessageStatus.READ;
    }

    public boolean isFromUser(User user) {
        return sender.getId().equals(user.getId());
    }
}