package com.nexainsights.dto;

import java.util.List;
import java.util.Map;

public class ChatResponseDto {
    private String response;
    private String sessionId;
    private List<Map<String, Object>> supportingRecords;
    private List<Integer> supportingRows;
    private String executedQuery;

    // Acción directa interactiva
    private String actionType;       // "DOWNLOAD_PDF", "DOWNLOAD_XLSX", "OPEN_EDITOR", "CHART_VIEW"
    private String actionTitle;      // Título visible de la acción
    private String downloadUrl;      // URL real y ejecutable
    private String actionDescription;// Descripción explicativa

    public ChatResponseDto() {}

    public ChatResponseDto(String response, String sessionId) {
        this.response = response;
        this.sessionId = sessionId;
    }

    public String getResponse() { return response; }
    public void setResponse(String response) { this.response = response; }

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public List<Map<String, Object>> getSupportingRecords() { return supportingRecords; }
    public void setSupportingRecords(List<Map<String, Object>> supportingRecords) { this.supportingRecords = supportingRecords; }

    public List<Integer> getSupportingRows() { return supportingRows; }
    public void setSupportingRows(List<Integer> supportingRows) { this.supportingRows = supportingRows; }

    public String getExecutedQuery() { return executedQuery; }
    public void setExecutedQuery(String executedQuery) { this.executedQuery = executedQuery; }

    public String getActionType() { return actionType; }
    public void setActionType(String actionType) { this.actionType = actionType; }

    public String getActionTitle() { return actionTitle; }
    public void setActionTitle(String actionTitle) { this.actionTitle = actionTitle; }

    public String getDownloadUrl() { return downloadUrl; }
    public void setDownloadUrl(String downloadUrl) { this.downloadUrl = downloadUrl; }

    public String getActionDescription() { return actionDescription; }
    public void setActionDescription(String actionDescription) { this.actionDescription = actionDescription; }
}
