package com.nexainsights.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "dashboards")
public class DashboardConfig {

    @Id
    private String id;

    @Indexed
    private String datasetId;

    @Indexed
    private String projectId;

    private String title;
    private String description;
    private LocalDateTime generatedAt = LocalDateTime.now();

    // KPIs generados
    private List<KpiCard> kpis = new ArrayList<>();

    // Gráficos configurados
    private List<ChartWidget> charts = new ArrayList<>();

    public DashboardConfig() {}

    public DashboardConfig(String datasetId, String projectId, String title) {
        this.datasetId = datasetId;
        this.projectId = projectId;
        this.title = title;
    }

    // Clases internas para componentes visuales
    public static class KpiCard {
        private String id;
        private String title;
        private String value;
        private String subtitle;
        private String columnKey;
        private String icon; // Icono temático
        private String badge; // Ej: "100% integridad", "Máximo valor"
        private String iconBgColor; // Color de fondo del círculo del icono (#E7F3EC, #FFF4E5, etc.)
        private String iconColor;   // Color del icono
        private String subtextColor; // Color del texto secundario (verde positivo, naranja, etc.)

        public KpiCard() {}
        public KpiCard(String id, String title, String value, String subtitle, String icon) {
            this.id = id;
            this.title = title;
            this.value = value;
            this.subtitle = subtitle;
            this.icon = icon;
            this.iconBgColor = "#E7F3EC";
            this.iconColor = "#245C4F";
        }

        // Getters and Setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getValue() { return value; }
        public void setValue(String value) { this.value = value; }
        public String getSubtitle() { return subtitle; }
        public void setSubtitle(String subtitle) { this.subtitle = subtitle; }
        public String getColumnKey() { return columnKey; }
        public void setColumnKey(String columnKey) { this.columnKey = columnKey; }
        public String getIcon() { return icon; }
        public void setIcon(String icon) { this.icon = icon; }
        public String getBadge() { return badge; }
        public void setBadge(String badge) { this.badge = badge; }
        public String getIconBgColor() { return iconBgColor; }
        public void setIconBgColor(String iconBgColor) { this.iconBgColor = iconBgColor; }
        public String getIconColor() { return iconColor; }
        public void setIconColor(String iconColor) { this.iconColor = iconColor; }
        public String getSubtextColor() { return subtextColor; }
        public void setSubtextColor(String subtextColor) { this.subtextColor = subtextColor; }
    }

    public static class ChartWidget {
        private String id;
        private String title;
        private String type; // bar, pie, doughnut, line, area, ranking
        private String dimensionKey; // Columna de agrupamiento (e.g. Proceso, Auditor)
        private String dimensionName;
        private String metricKey;    // Columna métrica o "COUNT"
        private String metricName;
        private String aggregation;  // COUNT, SUM, AVG
        private String description;
        private List<String> categories = new ArrayList<>();
        private List<Number> seriesData = new ArrayList<>();

        public ChartWidget() {}

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public String getDimensionKey() { return dimensionKey; }
        public void setDimensionKey(String dimensionKey) { this.dimensionKey = dimensionKey; }
        public String getDimensionName() { return dimensionName; }
        public void setDimensionName(String dimensionName) { this.dimensionName = dimensionName; }
        public String getMetricKey() { return metricKey; }
        public void setMetricKey(String metricKey) { this.metricKey = metricKey; }
        public String getMetricName() { return metricName; }
        public void setMetricName(String metricName) { this.metricName = metricName; }
        public String getAggregation() { return aggregation; }
        public void setAggregation(String aggregation) { this.aggregation = aggregation; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public List<String> getCategories() { return categories; }
        public void setCategories(List<String> categories) { this.categories = categories; }
        public List<Number> getSeriesData() { return seriesData; }
        public void setSeriesData(List<Number> seriesData) { this.seriesData = seriesData; }
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getDatasetId() { return datasetId; }
    public void setDatasetId(String datasetId) { this.datasetId = datasetId; }
    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public LocalDateTime getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(LocalDateTime generatedAt) { this.generatedAt = generatedAt; }
    public List<KpiCard> getKpis() { return kpis; }
    public void setKpis(List<KpiCard> kpis) { this.kpis = kpis; }
    public List<ChartWidget> getCharts() { return charts; }
    public void setCharts(List<ChartWidget> charts) { this.charts = charts; }
}
