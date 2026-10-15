package com.nexainsights.dto;

import java.util.List;
import java.util.Map;

public class ChatResponseDto {
    private String response;
    private String sessionId;
    private List<Map<String, Object>> supportingRecords;
    private List<Integer> supportingRows;
    private String executedQuery;

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
}
