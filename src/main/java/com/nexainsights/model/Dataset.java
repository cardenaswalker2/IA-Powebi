package com.nexainsights.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Document(collection = "datasets")
public class Dataset {

    @Id
    private String id;

    @Indexed
    private String projectId;

    private String name;                // Nombre visible del dataset (ej: Programa_Auditorias)
    private String originalFileName;    // Nombre del archivo de origen
    private String fileFormat;          // XLSX, XLS, CSV, JSON, etc.
    private String sheetName;           // Nombre de la hoja si aplica
    private long fileSize;              // Tamaño en bytes
    private long totalRows;             // Total de registros importados
    private int totalColumns;           // Total de columnas
    private int headerRowIndex;         // Fila del encabezado (0-based)
    private LocalDateTime importedAt = LocalDateTime.now();

    // Esquema de columnas
    private List<ColumnDefinition> columns = new ArrayList<>();

    // Mapeo inverso rápido: originalName -> key (ej: "AuditorResponsable" -> "col_3")
    private Map<String, String> nameToKeyMap = new HashMap<>();

    // Métricas de calidad de datos
    private double overallQualityScore; // 0.0 a 100.0%
    private long totalMissingValues;
    private long duplicateRowsCount;
    private List<String> qualityInsights = new ArrayList<>();

    public Dataset() {}

    public Dataset(String projectId, String name, String originalFileName, String fileFormat) {
        this.projectId = projectId;
        this.name = name;
        this.originalFileName = originalFileName;
        this.fileFormat = fileFormat;
    }

    public ColumnDefinition findColumnByKey(String key) {
        if (columns == null) return null;
        for (ColumnDefinition col : columns) {
            if (col.getKey().equalsIgnoreCase(key)) return col;
        }
        return null;
    }

    public ColumnDefinition findColumnByName(String name) {
        if (columns == null || name == null) return null;
        for (ColumnDefinition col : columns) {
            if (col.getOriginalName().equalsIgnoreCase(name.trim())) return col;
        }
        return null;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getOriginalFileName() { return originalFileName; }
    public void setOriginalFileName(String originalFileName) { this.originalFileName = originalFileName; }

    public String getFileFormat() { return fileFormat; }
    public void setFileFormat(String fileFormat) { this.fileFormat = fileFormat; }

    public String getSheetName() { return sheetName; }
    public void setSheetName(String sheetName) { this.sheetName = sheetName; }

    public long getFileSize() { return fileSize; }
    public void setFileSize(long fileSize) { this.fileSize = fileSize; }

    public long getTotalRows() { return totalRows; }
    public void setTotalRows(long totalRows) { this.totalRows = totalRows; }

    public int getTotalColumns() { return totalColumns; }
    public void setTotalColumns(int totalColumns) { this.totalColumns = totalColumns; }

    public int getHeaderRowIndex() { return headerRowIndex; }
    public void setHeaderRowIndex(int headerRowIndex) { this.headerRowIndex = headerRowIndex; }

    public LocalDateTime getImportedAt() { return importedAt; }
    public void setImportedAt(LocalDateTime importedAt) { this.importedAt = importedAt; }

    public List<ColumnDefinition> getColumns() { return columns; }
    public void setColumns(List<ColumnDefinition> columns) { this.columns = columns; }

    public Map<String, String> getNameToKeyMap() { return nameToKeyMap; }
    public void setNameToKeyMap(Map<String, String> nameToKeyMap) { this.nameToKeyMap = nameToKeyMap; }

    public double getOverallQualityScore() { return overallQualityScore; }
    public void setOverallQualityScore(double overallQualityScore) { this.overallQualityScore = overallQualityScore; }

    public long getTotalMissingValues() { return totalMissingValues; }
    public void setTotalMissingValues(long totalMissingValues) { this.totalMissingValues = totalMissingValues; }

    public long getDuplicateRowsCount() { return duplicateRowsCount; }
    public void setDuplicateRowsCount(long duplicateRowsCount) { this.duplicateRowsCount = duplicateRowsCount; }

    public List<String> getQualityInsights() { return qualityInsights; }
    public void setQualityInsights(List<String> qualityInsights) { this.qualityInsights = qualityInsights; }
}
