package com.nexainsights.model;

import java.util.List;
import java.util.Map;

public class ColumnDefinition {

    private String key;              // Clave interna segura (ej. col_1, col_fecha)
    private String originalName;     // Nombre original en el archivo (ej. "Auditor Responsable")
    private DataType detectedType;   // Tipo detectado automáticamente
    private DataType userOverrideType; // Tipo sobreescrito si el usuario lo cambia
    private double confidenceScore;  // Nivel de confianza (0.0 a 1.0)
    private long nullCount;          // Registros vacíos
    private long distinctCount;      // Valores únicos
    private Double minNumeric;       // Para numéricos
    private Double maxNumeric;
    private Double avgNumeric;
    private Double sumNumeric;
    private String minDate;          // Para fechas
    private String maxDate;
    private List<String> topCategories; // Categorías más comunes
    private Map<String, Long> categoryFrequencies; // Frecuencias
    private boolean isIdentifier;    // Posible clave primaria o código único
    private boolean isCategorical;   // Pocos valores distintos, ideal para gráficos
    private boolean isTemporal;      // Fecha o fecha/hora
    private boolean isMeasure;       // Numérico cuantitativo

    public ColumnDefinition() {}

    public ColumnDefinition(String key, String originalName, DataType detectedType) {
        this.key = key;
        this.originalName = originalName;
        this.detectedType = detectedType;
        this.confidenceScore = 1.0;
    }

    public DataType getEffectiveType() {
        return userOverrideType != null ? userOverrideType : detectedType;
    }

    // Getters and Setters
    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }

    public String getOriginalName() { return originalName; }
    public void setOriginalName(String originalName) { this.originalName = originalName; }

    public DataType getDetectedType() { return detectedType; }
    public void setDetectedType(DataType detectedType) { this.detectedType = detectedType; }

    public DataType getUserOverrideType() { return userOverrideType; }
    public void setUserOverrideType(DataType userOverrideType) { this.userOverrideType = userOverrideType; }

    public double getConfidenceScore() { return confidenceScore; }
    public void setConfidenceScore(double confidenceScore) { this.confidenceScore = confidenceScore; }

    public long getNullCount() { return nullCount; }
    public void setNullCount(long nullCount) { this.nullCount = nullCount; }

    public long getDistinctCount() { return distinctCount; }
    public void setDistinctCount(long distinctCount) { this.distinctCount = distinctCount; }

    public Double getMinNumeric() { return minNumeric; }
    public void setMinNumeric(Double minNumeric) { this.minNumeric = minNumeric; }

    public Double getMaxNumeric() { return maxNumeric; }
    public void setMaxNumeric(Double maxNumeric) { this.maxNumeric = maxNumeric; }

    public Double getAvgNumeric() { return avgNumeric; }
    public void setAvgNumeric(Double avgNumeric) { this.avgNumeric = avgNumeric; }

    public Double getSumNumeric() { return sumNumeric; }
    public void setSumNumeric(Double sumNumeric) { this.sumNumeric = sumNumeric; }

    public String getMinDate() { return minDate; }
    public void setMinDate(String minDate) { this.minDate = minDate; }

    public String getMaxDate() { return maxDate; }
    public void setMaxDate(String maxDate) { this.maxDate = maxDate; }

    public List<String> getTopCategories() { return topCategories; }
    public void setTopCategories(List<String> topCategories) { this.topCategories = topCategories; }

    public Map<String, Long> getCategoryFrequencies() { return categoryFrequencies; }
    public void setCategoryFrequencies(Map<String, Long> categoryFrequencies) { this.categoryFrequencies = categoryFrequencies; }

    public boolean isIdentifier() { return isIdentifier; }
    public void setIdentifier(boolean identifier) { isIdentifier = identifier; }

    public boolean isCategorical() { return isCategorical; }
    public void setCategorical(boolean categorical) { isCategorical = categorical; }

    public boolean isTemporal() { return isTemporal; }
    public void setTemporal(boolean temporal) { isTemporal = temporal; }

    public boolean isMeasure() { return isMeasure; }
    public void setMeasure(boolean measure) { isMeasure = measure; }
}
