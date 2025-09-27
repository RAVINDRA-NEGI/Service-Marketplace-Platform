package com.marketplace.model;

import java.time.LocalDateTime;

import com.marketplace.enums.MessageType;

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
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Table(name = "conversations",
       uniqueConstraints = @UniqueConstraint(columnNames = {"client_id", "professional_id"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Conversation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    @ToString.Exclude
    private ClientProfile client;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "professional_id", nullable = false)
    @ToString.Exclude
    private ProfessionalProfile professional;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id")
    @ToString.Exclude
    private Booking booking;

    @Column(name = "last_message_content", length = 1000)
    private String lastMessageContent;

    @Column(name = "last_message_type")
    @Enumerated(EnumType.STRING)
    private MessageType lastMessageType;

    @Column(name = "last_message_sent_at")
    private LocalDateTime lastMessageSentAt;

    @Column(name = "unread_count_client")
    private Integer unreadCountClient = 0;

    @Column(name = "unread_count_professional")
    private Integer unreadCountProfessional = 0;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "is_closed")
    private Boolean isClosed = false;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Constructor for creating conversation without booking
    public Conversation(ClientProfile client2, ProfessionalProfile professional) {
        this.client = client2;
        this.professional = professional;
    }

    // Constructor for creating conversation with booking
    public Conversation(ClientProfile client, ProfessionalProfile professional, Booking booking) {
        this(client, professional);
        this.booking = booking;
    }

    // Helper methods for unread counts
    public void incrementUnreadCount(User user) {
        if (user.getId().equals(client.getId())) {
            this.unreadCountProfessional++;
        } else if (user.getId().equals(professional.getUser().getId())) {
            this.unreadCountClient++;
        }
    }

    public void resetUnreadCount(User user) {
        if (user.getId().equals(client.getId())) {
            this.unreadCountClient = 0;
        } else if (user.getId().equals(professional.getUser().getId())) {
            this.unreadCountProfessional = 0;
        }
    }
}