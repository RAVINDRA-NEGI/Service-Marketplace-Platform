package com.marketplace.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.marketplace.model.Message;
import com.marketplace.model.MessageFile;

@Repository
public interface MessageFileRepository extends JpaRepository<MessageFile, Long> {
    
    List<MessageFile> findByMessage(Message message);
    
    List<MessageFile> findByMessageIn(List<Message> messages);
    
    void deleteByMessage(Message message);
    
    void deleteByMessageIn(List<Message> messages);
}