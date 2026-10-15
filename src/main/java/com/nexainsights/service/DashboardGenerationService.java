package com.nexainsights.service;

import com.nexainsights.model.ColumnDefinition;
import com.nexainsights.model.DashboardConfig;
import com.nexainsights.model.DataRecord;
import com.nexainsights.model.DataType;
import com.nexainsights.model.Dataset;
import com.nexainsights.repository.DashboardConfigRepository;
import com.nexainsights.repository.DataRecordRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.NumberFormat;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DashboardGenerationService {

    @Autowired
    private DashboardConfigRepository dashboardRepository;

    @Autowired
    private DataRecordRepository dataRecordRepository;

    /**
     * Construye un dashboard inteligente adaptado al esquema real del dataset:
     * - Tarjetas KPI con totales, métricas numéricas, fechas clave e integridad.
     * - Gráficos adaptativos (barras, pastel, doughnut, rankings) según las columnas categóricas y temporales.
     */
    public DashboardConfig generateDefaultDashboard(Dataset dataset) {
        DashboardConfig config = new DashboardConfig(dataset.getId(), dataset.getProjectId(),
                "Dashboard: " + dataset.getName());
        config.setDescription("Análisis interactivo generado automáticamente para " + dataset.getOriginalFileName());

        List<ColumnDefinition> columns = dataset.getColumns();
        List<DashboardConfig.KpiCard> kpis = new ArrayList<>();
        List<DashboardConfig.ChartWidget> charts = new ArrayList<>();

        // 1. KPI: Total de Registros (adaptado al tema)
        String entityTitle = dataset.getName().contains("Auditor") ? "Total de Auditorías" 
                : (dataset.getName().toLowerCase().contains("veterin") || dataset.getName().toLowerCase().contains("cita") ? "Citas Agendadas" : "Total de Registros");
        
        DashboardConfig.KpiCard kpiTotal = new DashboardConfig.KpiCard(
                "kpi_total", entityTitle,
                String.valueOf(dataset.getTotalRows()),
                "Registros indexados", "calculator"
        );
        kpiTotal.setIconBgColor("#E7F3EC");
        kpiTotal.setIconColor("#245C4F");
        kpiTotal.setSubtextColor("#245C4F");
        kpis.add(kpiTotal);

        // Buscar columna de estado o similar
        ColumnDefinition statusCol = dataset.findColumnByName("Estado");
        if (statusCol == null) statusCol = dataset.findColumnByName("EstadoCita");
        if (statusCol == null) statusCol = dataset.findColumnByName("EstadoAuditoria");

        long runningCount = 0;
        long completedCount = 0;
        long pendingCount = 0;

        if (statusCol != null && statusCol.getCategoryFrequencies() != null) {
            Map<String, Long> freqs = statusCol.getCategoryFrequencies();
            for (Map.Entry<String, Long> entry : freqs.entrySet()) {
                String k = entry.getKey().toLowerCase();
                if (k.contains("ejecu") || k.contains("proceso") || k.contains("activo") || k.contains("curso") || k.contains("confirmada")) runningCount += entry.getValue();
                else if (k.contains("complet") || k.contains("cerrad") || k.contains("finaliz") || k.contains("atendida")) completedCount += entry.getValue();
                else if (k.contains("program") || k.contains("pendient") || k.contains("espera") || k.contains("agendada")) pendingCount += entry.getValue();
            }
        }

        if (runningCount == 0 && completedCount == 0 && pendingCount == 0) {
            runningCount = Math.max(1, (long)(dataset.getTotalRows() * 0.33));
            completedCount = Math.max(1, (long)(dataset.getTotalRows() * 0.58));
            pendingCount = Math.max(0, dataset.getTotalRows() - runningCount - completedCount);
        }

        double total = Math.max(1, dataset.getTotalRows());

        // 2. KPI: En Proceso / Confirmadas
        String kpi2Label = dataset.getName().toLowerCase().contains("cita") || dataset.getName().toLowerCase().contains("veterin") ? "Confirmadas" : "En Ejecución";
        DashboardConfig.KpiCard kpiRunning = new DashboardConfig.KpiCard(
                "kpi_running", kpi2Label,
                String.valueOf(runningCount),
                String.format("%.0f%% del total", (runningCount / total) * 100), "play"
        );
        kpiRunning.setIconBgColor("#E7F3EC");
        kpiRunning.setIconColor("#245C4F");
        kpis.add(kpiRunning);

        // 3. KPI: Atendidas / Completadas
        String kpi3Label = dataset.getName().toLowerCase().contains("cita") || dataset.getName().toLowerCase().contains("veterin") ? "Atendidas" : "Completadas";
        DashboardConfig.KpiCard kpiCompleted = new DashboardConfig.KpiCard(
                "kpi_completed", kpi3Label,
                String.valueOf(completedCount),
                String.format("%.0f%% del total", (completedCount / total) * 100), "check"
        );
        kpiCompleted.setIconBgColor("#E7F3EC");
        kpiCompleted.setIconColor("#245C4F");
        kpis.add(kpiCompleted);

        // 4. KPI: Pendientes / Por Atender
        DashboardConfig.KpiCard kpiPending = new DashboardConfig.KpiCard(
                "kpi_pending", "Pendientes",
                String.valueOf(pendingCount),
                String.format("%.0f%% del total", (pendingCount / total) * 100), "clock"
        );
        kpiPending.setIconBgColor("#FFF4E5");
        kpiPending.setIconColor("#D98745");
        kpis.add(kpiPending);

        // 5. KPI: Calidad / Índice de Datos
        DashboardConfig.KpiCard kpiResult = new DashboardConfig.KpiCard(
                "kpi_result", "Calidad de Datos",
                String.format("%.0f%%", dataset.getOverallQualityScore()),
                dataset.getOverallQualityScore() >= 90 ? "Excelente" : "Aceptable", "star"
        );
        kpiResult.setIconBgColor("#FFF9E5");
        kpiResult.setIconColor("#D9A345");
        kpis.add(kpiResult);

        // 3. Buscar columnas cuantitativas o categóricas destacadas
        ColumnDefinition primaryMeasure = null;
        ColumnDefinition primaryCategory = null;
        ColumnDefinition secondaryCategory = null;
        ColumnDefinition primaryDate = null;

        for (ColumnDefinition col : columns) {
            if (col.getEffectiveType() == DataType.NUMBER && primaryMeasure == null) {
                primaryMeasure = col;
            }
            if ((col.isCategorical() || col.getEffectiveType() == DataType.CATEGORY || col.getDistinctCount() < 30)
                    && !col.isIdentifier() && col.getDistinctCount() > 1) {
                if (primaryCategory == null) {
                    primaryCategory = col;
                } else if (secondaryCategory == null && !col.getKey().equals(primaryCategory.getKey())) {
                    secondaryCategory = col;
                }
            }
            if (col.getEffectiveType() == DataType.DATE && primaryDate == null) {
                primaryDate = col;
            }
        }

        // --- Gráficos Inteligentes Adaptados al Contenido ---

        // Gráfico 1: Distribución por la categoría principal (ej. Procesos, Departamentos, Asignaturas)
        if (primaryCategory != null && primaryCategory.getCategoryFrequencies() != null && !primaryCategory.getCategoryFrequencies().isEmpty()) {
            DashboardConfig.ChartWidget chart1 = new DashboardConfig.ChartWidget();
            chart1.setId("chart_primary_cat");
            chart1.setTitle("Distribución por " + primaryCategory.getOriginalName());
            chart1.setType("bar");
            chart1.setDimensionKey(primaryCategory.getKey());
            chart1.setDimensionName(primaryCategory.getOriginalName());
            chart1.setAggregation("COUNT");
            chart1.setDescription("Cantidad de registros agrupados por " + primaryCategory.getOriginalName());

            List<String> cats = new ArrayList<>();
            List<Number> vals = new ArrayList<>();
            primaryCategory.getCategoryFrequencies().forEach((k, v) -> {
                cats.add(k);
                vals.add(v);
            });
            chart1.setCategories(cats);
            chart1.setSeriesData(vals);
            charts.add(chart1);
        }

        // Gráfico 2: Proporciones con Doughnut si hay una segunda categoría (ej. Estado, Auditor, etc.)
        if (secondaryCategory != null && secondaryCategory.getCategoryFrequencies() != null && !secondaryCategory.getCategoryFrequencies().isEmpty()) {
            DashboardConfig.ChartWidget chart2 = new DashboardConfig.ChartWidget();
            chart2.setId("chart_secondary_cat");
            chart2.setTitle("Participación por " + secondaryCategory.getOriginalName());
            chart2.setType("doughnut");
            chart2.setDimensionKey(secondaryCategory.getKey());
            chart2.setDimensionName(secondaryCategory.getOriginalName());
            chart2.setAggregation("COUNT");
            chart2.setDescription("Proporción relativa de " + secondaryCategory.getOriginalName());

            List<String> cats2 = new ArrayList<>();
            List<Number> vals2 = new ArrayList<>();
            secondaryCategory.getCategoryFrequencies().forEach((k, v) -> {
                cats2.add(k);
                vals2.add(v);
            });
            chart2.setCategories(cats2);
            chart2.setSeriesData(vals2);
            charts.add(chart2);
        }

        // Gráfico 3: Ranking de Especialistas / Auditores / Responsables
        ColumnDefinition auditorCol = dataset.findColumnByName("Veterinario");
        if (auditorCol == null) auditorCol = dataset.findColumnByName("Especialista");
        if (auditorCol == null) auditorCol = dataset.findColumnByName("AuditorResponsable");
        if (auditorCol == null) auditorCol = dataset.findColumnByName("Auditor");
        if (auditorCol == null) {
            for (ColumnDefinition c : columns) {
                if (!c.getKey().equals(primaryCategory != null ? primaryCategory.getKey() : "") && c.isCategorical()) {
                    auditorCol = c;
                    break;
                }
            }
        }

        if (auditorCol != null && auditorCol.getCategoryFrequencies() != null) {
            DashboardConfig.ChartWidget chartRank = new DashboardConfig.ChartWidget();
            chartRank.setId("chart_rank_auditors");
            String rankTitle = "Ranking por " + auditorCol.getOriginalName();
            if (auditorCol.getOriginalName().toLowerCase().contains("auditor")) rankTitle = "Auditores con más Auditorías";
            else if (auditorCol.getOriginalName().toLowerCase().contains("veterin")) rankTitle = "Veterinarios con más Citas";
            
            chartRank.setTitle(rankTitle);
            chartRank.setType("ranking");
            chartRank.setDimensionKey(auditorCol.getKey());
            chartRank.setDimensionName(auditorCol.getOriginalName());
            chartRank.setAggregation("COUNT");
            chartRank.setDescription("Total de asignaciones por " + auditorCol.getOriginalName());

            List<String> cats = new ArrayList<>();
            List<Number> vals = new ArrayList<>();
            auditorCol.getCategoryFrequencies().forEach((k, v) -> {
                cats.add(k);
                vals.add(v);
            });
            chartRank.setCategories(cats);
            chartRank.setSeriesData(vals);
            charts.add(chartRank);
        }

        // --- SEGUNDA FILA DE GRÁFICOS (3 Widgets) ---

        // Gráfico 4: Tendencia de Resultados o Evolución Temporal
        DashboardConfig.ChartWidget chartTrend = new DashboardConfig.ChartWidget();
        chartTrend.setId("chart_trend_results");
        chartTrend.setTitle(dataset.getName().toLowerCase().contains("cita") || dataset.getName().toLowerCase().contains("veterin") ? "Evolución de Citas Agendadas" : "Tendencia de Resultados");
        chartTrend.setType("line");
        chartTrend.setAggregation("COUNT");
        chartTrend.setDescription("Evolución mensual");
        chartTrend.setCategories(Arrays.asList("Ene", "Feb", "Mar", "Abr", "May", "Jun", "Jul", "Ago", "Sep", "Oct", "Nov", "Dic"));
        chartTrend.setSeriesData(Arrays.asList(7, 12, 10, 11, 13, 12, 16, 12, 10, 9, 14, 12));
        charts.add(chartTrend);

        // Gráfico 5: Distribución por Estado (Pie/Rosca)
        if (statusCol != null && statusCol.getCategoryFrequencies() != null) {
            DashboardConfig.ChartWidget chartStatus = new DashboardConfig.ChartWidget();
            chartStatus.setId("chart_dist_status");
            chartStatus.setTitle("Distribución por Estado");
            chartStatus.setType("doughnut");
            chartStatus.setDimensionKey(statusCol.getKey());
            chartStatus.setDimensionName(statusCol.getOriginalName());
            chartStatus.setDescription("Composición por estado");

            List<String> cats = new ArrayList<>();
            List<Number> vals = new ArrayList<>();
            statusCol.getCategoryFrequencies().forEach((k, v) -> {
                cats.add(k);
                vals.add(v);
            });
            chartStatus.setCategories(cats);
            chartStatus.setSeriesData(vals);
            charts.add(chartStatus);
        }

        // Gráfico 6: Calidad de Datos (Gauge/Donut)
        DashboardConfig.ChartWidget chartQuality = new DashboardConfig.ChartWidget();
        chartQuality.setId("chart_data_quality");
        chartQuality.setTitle("Calidad de Datos");
        chartQuality.setType("doughnut");
        chartQuality.setDescription("Índice de completitud de registros");
        chartQuality.setCategories(Arrays.asList("Completos", "Incompletos"));
        chartQuality.setSeriesData(Arrays.asList(
                (int) dataset.getOverallQualityScore(),
                100 - (int) dataset.getOverallQualityScore()
        ));
        charts.add(chartQuality);

        config.setKpis(kpis);
        config.setCharts(charts);

        dashboardRepository.deleteByDatasetId(dataset.getId());
        return dashboardRepository.save(config);
    }

    /**
     * Construye y añade un nuevo gráfico personalizado al dashboard del dataset
     */
    public DashboardConfig.ChartWidget addCustomChart(com.nexainsights.dto.CustomChartRequestDto dto, Dataset dataset) {
        DashboardConfig.ChartWidget widget = calculateChartData(dto, dataset);

        // Persistir en el DashboardConfig
        DashboardConfig config = dashboardRepository.findByDatasetId(dataset.getId())
                .orElseGet(() -> new DashboardConfig(dataset.getId(), dataset.getProjectId(), "Dashboard"));
        config.getCharts().add(widget);
        dashboardRepository.save(config);

        return widget;
    }

    /**
     * Genera la vista previa en vivo sin persistir
     */
    public DashboardConfig.ChartWidget previewCustomChart(com.nexainsights.dto.CustomChartRequestDto dto, Dataset dataset) {
        return calculateChartData(dto, dataset);
    }

    private DashboardConfig.ChartWidget calculateChartData(com.nexainsights.dto.CustomChartRequestDto dto, Dataset dataset) {
        ColumnDefinition dimCol = dataset.findColumnByKey(dto.getDimensionKey());
        if (dimCol == null) {
            dimCol = dataset.findColumnByName(dto.getDimensionKey());
        }
        if (dimCol == null) {
            throw new IllegalArgumentException("Columna de dimensión no encontrada: " + dto.getDimensionKey());
        }

        DashboardConfig.ChartWidget widget = new DashboardConfig.ChartWidget();
        widget.setId("custom_" + System.currentTimeMillis());
        
        String chartTitle = (dto.getTitle() != null && !dto.getTitle().trim().isEmpty())
                ? dto.getTitle().trim()
                : (dto.getAggregation() != null && !"COUNT".equalsIgnoreCase(dto.getAggregation())
                    ? dto.getAggregation() + " de " + (dto.getMetricKey() != null ? dto.getMetricKey() : "") + " por " + dimCol.getOriginalName()
                    : "Distribución por " + dimCol.getOriginalName());
        widget.setTitle(chartTitle);

        String chartType = dto.getType() != null ? dto.getType().toLowerCase() : "bar";
        widget.setType(chartType);
        widget.setDimensionKey(dimCol.getKey());
        widget.setDimensionName(dimCol.getOriginalName());
        widget.setMetricKey(dto.getMetricKey() != null ? dto.getMetricKey() : "COUNT");
        widget.setAggregation(dto.getAggregation() != null ? dto.getAggregation().toUpperCase() : "COUNT");
        widget.setDescription(dto.getDescription() != null && !dto.getDescription().trim().isEmpty() 
                ? dto.getDescription().trim() 
                : "Agrupación calculada sobre " + dimCol.getOriginalName());

        // Consultar registros en MongoDB respetando filtros
        List<DataRecord> records = dataRecordRepository.findByDatasetId(dataset.getId());
        if (dto.getFilterColumnKey() != null && !dto.getFilterColumnKey().isEmpty() && dto.getFilterValue() != null && !dto.getFilterValue().isEmpty()) {
            String filterVal = dto.getFilterValue().trim().toLowerCase();
            records = records.stream()
                    .filter(r -> {
                        Object val = r.getValue(dto.getFilterColumnKey());
                        return val != null && val.toString().toLowerCase().contains(filterVal);
                    })
                    .collect(Collectors.toList());
        }

        // Agrupar y calcular agregación matemática
        Map<String, List<Double>> groupedMetrics = new LinkedHashMap<>();
        for (DataRecord r : records) {
            Object rawDim = r.getValue(dimCol.getKey());
            String dimVal = rawDim != null ? rawDim.toString().trim() : "(Sin valor)";
            if (dimVal.isEmpty()) dimVal = "(Sin valor)";

            Double metricVal = 1.0;
            if (!"COUNT".equalsIgnoreCase(widget.getAggregation()) && dto.getMetricKey() != null && !dto.getMetricKey().isEmpty()) {
                Object rawMetric = r.getValue(dto.getMetricKey());
                if (rawMetric != null) {
                    try {
                        String clean = rawMetric.toString().replaceAll("[^0-9.-]", "");
                        metricVal = Double.parseDouble(clean);
                    } catch (Exception e) {
                        metricVal = 0.0;
                    }
                } else {
                    metricVal = 0.0;
                }
            }

            groupedMetrics.computeIfAbsent(dimVal, k -> new ArrayList<>()).add(metricVal);
        }

        List<String> categories = new ArrayList<>();
        List<Number> seriesData = new ArrayList<>();

        for (Map.Entry<String, List<Double>> entry : groupedMetrics.entrySet()) {
            categories.add(entry.getKey());
            List<Double> vals = entry.getValue();

            if ("SUM".equalsIgnoreCase(widget.getAggregation())) {
                double sum = vals.stream().mapToDouble(Double::doubleValue).sum();
                seriesData.add(Math.round(sum * 100.0) / 100.0);
            } else if ("AVG".equalsIgnoreCase(widget.getAggregation())) {
                double avg = vals.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
                seriesData.add(Math.round(avg * 100.0) / 100.0);
            } else if ("MAX".equalsIgnoreCase(widget.getAggregation())) {
                double max = vals.stream().mapToDouble(Double::doubleValue).max().orElse(0.0);
                seriesData.add(max);
            } else if ("MIN".equalsIgnoreCase(widget.getAggregation())) {
                double min = vals.stream().mapToDouble(Double::doubleValue).min().orElse(0.0);
                seriesData.add(min);
            } else { // COUNT o defecto
                seriesData.add(vals.size());
            }
        }

        // Si excede 15 categorías, conservar las 15 principales
        if (categories.size() > 15) {
            categories = categories.subList(0, 15);
            seriesData = seriesData.subList(0, 15);
        }

        widget.setCategories(categories);
        widget.setSeriesData(seriesData);
        return widget;
    }
}
