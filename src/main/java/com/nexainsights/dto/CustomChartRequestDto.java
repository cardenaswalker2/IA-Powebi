package com.nexainsights.dto;

import java.util.List;

public class CustomChartRequestDto {
    private String datasetId;
    private String title;
    private String type;           // "bar", "line", "doughnut", "area", "ranking"
    private String dimensionKey;   // Columna a agrupar
    private String metricKey;      // "COUNT" o columna numérica
    private String aggregation;    // "COUNT", "SUM", "AVG", "MIN", "MAX"
    private String filterColumnKey;
    private String filterValue;

    public CustomChartRequestDto() {}

    public String getDatasetId() { return datasetId; }
    public void setDatasetId(String datasetId) { this.datasetId = datasetId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getDimensionKey() { return dimensionKey; }
    public void setDimensionKey(String dimensionKey) { this.dimensionKey = dimensionKey; }

    public String getMetricKey() { return metricKey; }
    public void setMetricKey(String metricKey) { this.metricKey = metricKey; }

    public String getAggregation() { return aggregation; }
    public void setAggregation(String aggregation) { this.aggregation = aggregation; }

    public String getFilterColumnKey() { return filterColumnKey; }
    public void setFilterColumnKey(String filterColumnKey) { this.filterColumnKey = filterColumnKey; }

    public String getFilterValue() { return filterValue; }
    public void setFilterValue(String filterValue) { this.filterValue = filterValue; }
}
