package com.nexainsights.dto;

import java.util.List;
import java.util.Map;

public class PreviewResponseDto {
    private String fileName;
    private String format;
    private long fileSize;
    private List<String> availableSheets;
    private String selectedSheet;
    private int detectedHeaderRow;
    private List<String> originalHeaders;
    private List<String> detectedTypes;
    private List<Map<String, Object>> previewRows; // Primeras 10 filas de muestra
    private int estimatedTotalRows;
    private int totalColumns;

    public PreviewResponseDto() {}

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public String getFormat() { return format; }
    public void setFormat(String format) { this.format = format; }

    public long getFileSize() { return fileSize; }
    public void setFileSize(long fileSize) { this.fileSize = fileSize; }

    public List<String> getAvailableSheets() { return availableSheets; }
    public void setAvailableSheets(List<String> availableSheets) { this.availableSheets = availableSheets; }

    public String getSelectedSheet() { return selectedSheet; }
    public void setSelectedSheet(String selectedSheet) { this.selectedSheet = selectedSheet; }

    public int getDetectedHeaderRow() { return detectedHeaderRow; }
    public void setDetectedHeaderRow(int detectedHeaderRow) { this.detectedHeaderRow = detectedHeaderRow; }

    public List<String> getOriginalHeaders() { return originalHeaders; }
    public void setOriginalHeaders(List<String> originalHeaders) { this.originalHeaders = originalHeaders; }

    public List<String> getDetectedTypes() { return detectedTypes; }
    public void setDetectedTypes(List<String> detectedTypes) { this.detectedTypes = detectedTypes; }

    public List<Map<String, Object>> getPreviewRows() { return previewRows; }
    public void setPreviewRows(List<Map<String, Object>> previewRows) { this.previewRows = previewRows; }

    public int getEstimatedTotalRows() { return estimatedTotalRows; }
    public void setEstimatedTotalRows(int estimatedTotalRows) { this.estimatedTotalRows = estimatedTotalRows; }

    public int getTotalColumns() { return totalColumns; }
    public void setTotalColumns(int totalColumns) { this.totalColumns = totalColumns; }
}
