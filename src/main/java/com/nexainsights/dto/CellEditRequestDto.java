package com.nexainsights.dto;

import java.util.Map;

public class CellEditRequestDto {
    private String recordId;
    private String columnKey;
    private Object newValue;
    private String reason;

    public CellEditRequestDto() {}

    public String getRecordId() { return recordId; }
    public void setRecordId(String recordId) { this.recordId = recordId; }

    public String getColumnKey() { return columnKey; }
    public void setColumnKey(String columnKey) { this.columnKey = columnKey; }

    public Object getNewValue() { return newValue; }
    public void setNewValue(Object newValue) { this.newValue = newValue; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
