package com.nexainsights.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "dataset_relationships")
public class DatasetRelationship {

    @Id
    private String id;

    @Indexed
    private String projectId;

    private String sourceDatasetId;
    private String sourceDatasetName;
    private String sourceColumnKey;
    private String sourceColumnName;

    private String targetDatasetId;
    private String targetDatasetName;
    private String targetColumnKey;
    private String targetColumnName;

    private String relationshipType; // ONE_TO_ONE, ONE_TO_MANY, MANY_TO_MANY
    private double matchConfidence;  // 0.0 a 1.0
    private long matchedRecordsCount;
    private long unmatchedRecordsCount;
    private boolean isConfirmedByUser;
    private LocalDateTime detectedAt = LocalDateTime.now();

    public DatasetRelationship() {}

    public DatasetRelationship(String projectId, String sourceDatasetId, String sourceDatasetName, String sourceColumnKey, String sourceColumnName,
                               String targetDatasetId, String targetDatasetName, String targetColumnKey, String targetColumnName) {
        this.projectId = projectId;
        this.sourceDatasetId = sourceDatasetId;
        this.sourceDatasetName = sourceDatasetName;
        this.sourceColumnKey = sourceColumnKey;
        this.sourceColumnName = sourceColumnName;
        this.targetDatasetId = targetDatasetId;
        this.targetDatasetName = targetDatasetName;
        this.targetColumnKey = targetColumnKey;
        this.targetColumnName = targetColumnName;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }
    public String getSourceDatasetId() { return sourceDatasetId; }
    public void setSourceDatasetId(String sourceDatasetId) { this.sourceDatasetId = sourceDatasetId; }
    public String getSourceDatasetName() { return sourceDatasetName; }
    public void setSourceDatasetName(String sourceDatasetName) { this.sourceDatasetName = sourceDatasetName; }
    public String getSourceColumnKey() { return sourceColumnKey; }
    public void setSourceColumnKey(String sourceColumnKey) { this.sourceColumnKey = sourceColumnKey; }
    public String getSourceColumnName() { return sourceColumnName; }
    public void setSourceColumnName(String sourceColumnName) { this.sourceColumnName = sourceColumnName; }
    public String getTargetDatasetId() { return targetDatasetId; }
    public void setTargetDatasetId(String targetDatasetId) { this.targetDatasetId = targetDatasetId; }
    public String getTargetDatasetName() { return targetDatasetName; }
    public void setTargetDatasetName(String targetDatasetName) { this.targetDatasetName = targetDatasetName; }
    public String getTargetColumnKey() { return targetColumnKey; }
    public void setTargetColumnKey(String targetColumnKey) { this.targetColumnKey = targetColumnKey; }
    public String getTargetColumnName() { return targetColumnName; }
    public void setTargetColumnName(String targetColumnName) { this.targetColumnName = targetColumnName; }
    public String getRelationshipType() { return relationshipType; }
    public void setRelationshipType(String relationshipType) { this.relationshipType = relationshipType; }
    public double getMatchConfidence() { return matchConfidence; }
    public void setMatchConfidence(double matchConfidence) { this.matchConfidence = matchConfidence; }
    public long getMatchedRecordsCount() { return matchedRecordsCount; }
    public void setMatchedRecordsCount(long matchedRecordsCount) { this.matchedRecordsCount = matchedRecordsCount; }
    public long getUnmatchedRecordsCount() { return unmatchedRecordsCount; }
    public void setUnmatchedRecordsCount(long unmatchedRecordsCount) { this.unmatchedRecordsCount = unmatchedRecordsCount; }
    public boolean isConfirmedByUser() { return isConfirmedByUser; }
    public void setConfirmedByUser(boolean confirmedByUser) { isConfirmedByUser = confirmedByUser; }
    public LocalDateTime getDetectedAt() { return detectedAt; }
    public void setDetectedAt(LocalDateTime detectedAt) { this.detectedAt = detectedAt; }
}
