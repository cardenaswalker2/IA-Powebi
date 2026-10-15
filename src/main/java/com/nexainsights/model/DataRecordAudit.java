package com.nexainsights.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "data_record_audits")
@CompoundIndex(name = "dataset_row_audit_idx", def = "{'datasetId': 1, 'rowNumber': 1, 'timestamp': -1}")
public class DataRecordAudit {

    @Id
    private String id;

    @Indexed
    private String datasetId;

    private int rowNumber;
    private String recordId;
    private String columnKey;
    private String columnName;
    private String operation; // "CELL_EDIT", "ROW_ADD", "ROW_DELETE", "COLUMN_RENAME"
    private Object oldValue;
    private Object newValue;
    private String modifiedBy;
    private LocalDateTime timestamp = LocalDateTime.now();
    private String reason;

    public DataRecordAudit() {}

    public DataRecordAudit(String datasetId, int rowNumber, String recordId, String columnKey, String columnName,
                           String operation, Object oldValue, Object newValue, String modifiedBy) {
        this.datasetId = datasetId;
        this.rowNumber = rowNumber;
        this.recordId = recordId;
        this.columnKey = columnKey;
        this.columnName = columnName;
        this.operation = operation;
        this.oldValue = oldValue;
        this.newValue = newValue;
        this.modifiedBy = modifiedBy;
        this.timestamp = LocalDateTime.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getDatasetId() { return datasetId; }
    public void setDatasetId(String datasetId) { this.datasetId = datasetId; }

    public int getRowNumber() { return rowNumber; }
    public void setRowNumber(int rowNumber) { this.rowNumber = rowNumber; }

    public String getRecordId() { return recordId; }
    public void setRecordId(String recordId) { this.recordId = recordId; }

    public String getColumnKey() { return columnKey; }
    public void setColumnKey(String columnKey) { this.columnKey = columnKey; }

    public String getColumnName() { return columnName; }
    public void setColumnName(String columnName) { this.columnName = columnName; }

    public String getOperation() { return operation; }
    public void setOperation(String operation) { this.operation = operation; }

    public Object getOldValue() { return oldValue; }
    public void setOldValue(Object oldValue) { this.oldValue = oldValue; }

    public Object getNewValue() { return newValue; }
    public void setNewValue(Object newValue) { this.newValue = newValue; }

    public String getModifiedBy() { return modifiedBy; }
    public void setModifiedBy(String modifiedBy) { this.modifiedBy = modifiedBy; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
