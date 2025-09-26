package com.marketplace.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.marketplace.model.Message;
import com.marketplace.model.MessageFile;

@Repository
public interface MessageFileRepository extends JpaRepository<MessageFile, Long> {
    
    // Find files by message
    List<MessageFile> findByMessage(Message message);
    
    // Find files by message ID
    List<MessageFile> findByMessageId(Long messageId);
    
    // Find files by file type
    List<MessageFile> findByFileType(String fileType);
    
    // Count files by message
    long countByMessage(Message message);
    
    // Delete files by message
    void deleteByMessage(Message message);
    
    // Delete files by message ID
    void deleteByMessageId(Long messageId);
}