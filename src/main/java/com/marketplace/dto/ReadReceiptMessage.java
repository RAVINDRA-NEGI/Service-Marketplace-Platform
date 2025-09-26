package com.marketplace.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReadReceiptMessage {
    private Long messageId;
    private String username;
    private LocalDateTime timestamp;
}