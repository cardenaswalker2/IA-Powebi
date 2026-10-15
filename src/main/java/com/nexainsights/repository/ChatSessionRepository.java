package com.nexainsights.repository;

import com.nexainsights.model.ChatSession;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatSessionRepository extends MongoRepository<ChatSession, String> {
    List<ChatSession> findByProjectIdOrderByUpdatedAtDesc(String projectId);
    List<ChatSession> findByDatasetIdOrderByUpdatedAtDesc(String datasetId);
}
