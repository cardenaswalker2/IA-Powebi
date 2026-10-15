package com.nexainsights.dto;

import java.util.List;
import java.util.Map;

public class ChatRequestDto {
    private String projectId;
    private String datasetId;
    private String sessionId;
    private String message;

    public ChatRequestDto() {}

    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }

    public String getDatasetId() { return datasetId; }
    public void setDatasetId(String datasetId) { this.datasetId = datasetId; }

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
