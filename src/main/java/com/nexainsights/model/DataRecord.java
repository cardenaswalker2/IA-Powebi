package com.nexainsights.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.HashMap;
import java.util.Map;

@Document(collection = "dataset_records")
@CompoundIndex(name = "dataset_row_idx", def = "{'datasetId': 1, 'rowNumber': 1}")
public class DataRecord {

    @Id
    private String id;

    @Indexed
    private String datasetId;

    @Indexed
    private String projectId;

    private int rowNumber; // Número de fila original en el archivo (1-based)

    // Valores indexables por la clave interna de columna (col_0, col_1, etc.)
    private Map<String, Object> data = new HashMap<>();

    public DataRecord() {}

    public DataRecord(String datasetId, String projectId, int rowNumber, Map<String, Object> data) {
        this.datasetId = datasetId;
        this.projectId = projectId;
        this.rowNumber = rowNumber;
        this.data = data;
    }

    public Object getValue(String columnKey) {
        return data != null ? data.get(columnKey) : null;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getDatasetId() { return datasetId; }
    public void setDatasetId(String datasetId) { this.datasetId = datasetId; }

    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }

    public int getRowNumber() { return rowNumber; }
    public void setRowNumber(int rowNumber) { this.rowNumber = rowNumber; }

    public Map<String, Object> getData() { return data; }
    public void setData(Map<String, Object> data) { this.data = data; }
}
