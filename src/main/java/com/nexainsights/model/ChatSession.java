package com.nexainsights.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Document(collection = "chat_sessions")
public class ChatSession {

    @Id
    private String id;

    @Indexed
    private String projectId;

    @Indexed
    private String datasetId;

    private String title;
    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt = LocalDateTime.now();
    private List<ChatMessage> messages = new ArrayList<>();

    public ChatSession() {}

    public ChatSession(String projectId, String datasetId, String title) {
        this.projectId = projectId;
        this.datasetId = datasetId;
        this.title = title;
    }

    public static class ChatMessage {
        private String role; // "user", "assistant", "system"
        private String content;
        private LocalDateTime timestamp = LocalDateTime.now();

        // Trazabilidad de datos reales
        private List<Map<String, Object>> supportingRecords; // Registros reales que sustentan la respuesta
        private List<Integer> supportingRowNumbers;          // Filas originales de Excel encontradas
        private String executedQuery;                        // Explicación de la consulta ejecutada

        public ChatMessage() {}

        public ChatMessage(String role, String content) {
            this.role = role;
            this.content = content;
        }

        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        public LocalDateTime getTimestamp() { return timestamp; }
        public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
        public List<Map<String, Object>> getSupportingRecords() { return supportingRecords; }
        public void setSupportingRecords(List<Map<String, Object>> supportingRecords) { this.supportingRecords = supportingRecords; }
        public List<Integer> getSupportingRowNumbers() { return supportingRowNumbers; }
        public void setSupportingRowNumbers(List<Integer> supportingRowNumbers) { this.supportingRowNumbers = supportingRowNumbers; }
        public String getExecutedQuery() { return executedQuery; }
        public void setExecutedQuery(String executedQuery) { this.executedQuery = executedQuery; }
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }
    public String getDatasetId() { return datasetId; }
    public void setDatasetId(String datasetId) { this.datasetId = datasetId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public List<ChatMessage> getMessages() { return messages; }
    public void setMessages(List<ChatMessage> messages) { this.messages = messages; }
}
