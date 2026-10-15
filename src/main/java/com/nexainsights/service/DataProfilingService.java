package com.nexainsights.service;

import com.nexainsights.model.ColumnDefinition;
import com.nexainsights.model.DataRecord;
import com.nexainsights.model.DataType;
import com.nexainsights.model.Dataset;
import com.nexainsights.repository.DataRecordRepository;
import com.nexainsights.repository.DatasetRepository;
import com.nexainsights.util.TypeInferenceEngine;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class DataProfilingService {

    @Autowired
    private DataRecordRepository dataRecordRepository;

    @Autowired
    private DatasetRepository datasetRepository;

    /**
     * Examina los registros del dataset para deducir tipos, calcular frecuencias,
     * detectar valores nulos, métricas numéricas y evaluar la calidad.
     */
    public void profileDataset(Dataset dataset) {
        List<ColumnDefinition> columns = dataset.getColumns();
        if (columns == null || columns.isEmpty() || dataset.getTotalRows() == 0) {
            return;
        }

        // Muestra de hasta 2000 registros para perfilado estadístico de alta velocidad
        int sampleSize = (int) Math.min(2000, dataset.getTotalRows());
        List<DataRecord> sampleRecords = dataRecordRepository.findByDatasetId(
                dataset.getId(), PageRequest.of(0, sampleSize)).getContent();

        long totalMissingAcrossAll = 0;
        List<String> qualityInsights = new ArrayList<>();

        for (ColumnDefinition col : columns) {
            String colKey = col.getKey();
            Map<DataType, Integer> typeVotes = new EnumMap<>(DataType.class);
            Map<String, Long> valueFrequencies = new HashMap<>();
            long nullCount = 0;

            Double minNum = null;
            Double maxNum = null;
            double sumNum = 0.0;
            int validNumCount = 0;

            String minDate = null;
            String maxDate = null;

            for (DataRecord record : sampleRecords) {
                Object valObj = record.getValue(colKey);
                String val = valObj != null ? valObj.toString().trim() : "";

                if (val.isEmpty()) {
                    nullCount++;
                    continue;
                }

                // Frecuencia de categorías
                valueFrequencies.put(val, valueFrequencies.getOrDefault(val, 0L) + 1);

                // Inferencia del tipo del valor
                DataType inferred = TypeInferenceEngine.inferSingleValueType(val);
                typeVotes.put(inferred, typeVotes.getOrDefault(inferred, 0) + 1);

                if (inferred == DataType.NUMBER) {
                    try {
                        Object parsed = TypeInferenceEngine.parseValueToType(val, DataType.NUMBER);
                        if (parsed instanceof Number) {
                            double d = ((Number) parsed).doubleValue();
                            if (minNum == null || d < minNum) minNum = d;
                            if (maxNum == null || d > maxNum) maxNum = d;
                            sumNum += d;
                            validNumCount++;
                        }
                    } catch (Exception ignored) {}
                } else if (inferred == DataType.DATE) {
                    if (minDate == null || val.compareTo(minDate) < 0) minDate = val;
                    if (maxDate == null || val.compareTo(maxDate) > 0) maxDate = val;
                }
            }

            // Proyectar recuento de nulos al total
            long estimatedNulls = (long) ((double) nullCount / sampleRecords.size() * dataset.getTotalRows());
            col.setNullCount(estimatedNulls);
            col.setDistinctCount(valueFrequencies.size());
            totalMissingAcrossAll += estimatedNulls;

            // Determinar tipo dominante
            DataType dominantType = DataType.TEXT;
            int maxVotes = 0;
            for (Map.Entry<DataType, Integer> entry : typeVotes.entrySet()) {
                if (entry.getValue() > maxVotes) {
                    maxVotes = entry.getValue();
                    dominantType = entry.getKey();
                }
            }

            double confidence = sampleRecords.size() > 0 ? (double) maxVotes / (sampleRecords.size() - nullCount) : 1.0;
            col.setDetectedType(dominantType);
            col.setConfidenceScore(Double.isNaN(confidence) ? 1.0 : confidence);

            // Métricas numéricas
            if (dominantType == DataType.NUMBER && validNumCount > 0) {
                col.setMinNumeric(minNum);
                col.setMaxNumeric(maxNum);
                col.setSumNumeric(sumNum);
                col.setAvgNumeric(sumNum / validNumCount);
                col.setMeasure(true);
            }

            if (dominantType == DataType.DATE) {
                col.setMinDate(minDate);
                col.setMaxDate(maxDate);
                col.setTemporal(true);
            }

            // Clasificación como identificador o categoría
            int distinctValues = valueFrequencies.size();
            int nonNullSample = sampleRecords.size() - (int) nullCount;

            if (distinctValues == nonNullSample && nonNullSample > 5) {
                col.setIdentifier(true);
            } else if (distinctValues >= 1 && distinctValues <= 50) {
                col.setCategorical(true);
                // Top 10 categorías más frecuentes
                List<Map.Entry<String, Long>> sorted = new ArrayList<>(valueFrequencies.entrySet());
                sorted.sort((a, b) -> Long.compare(b.getValue(), a.getValue()));

                List<String> topCat = new ArrayList<>();
                Map<String, Long> topMap = new LinkedHashMap<>();
                for (int i = 0; i < Math.min(10, sorted.size()); i++) {
                    topCat.add(sorted.get(i).getKey());
                    topMap.put(sorted.get(i).getKey(), sorted.get(i).getValue());
                }
                col.setTopCategories(topCat);
                col.setCategoryFrequencies(topMap);
            }

            // Observaciones de calidad por columna
            if (estimatedNulls > 0) {
                double pct = ((double) estimatedNulls / dataset.getTotalRows()) * 100.0;
                if (pct > 25.0) {
                    qualityInsights.add(String.format("La columna '%s' tiene %.1f%% de valores vacíos.", col.getOriginalName(), pct));
                }
            }
        }

        // Calidad global del dataset
        long totalCells = dataset.getTotalRows() * dataset.getTotalColumns();
        double completeness = totalCells > 0 ? (1.0 - ((double) totalMissingAcrossAll / totalCells)) * 100.0 : 100.0;
        dataset.setOverallQualityScore(Math.max(0.0, Math.min(100.0, completeness)));
        dataset.setTotalMissingValues(totalMissingAcrossAll);

        if (qualityInsights.isEmpty()) {
            qualityInsights.add("Excelente integridad general: no se detectaron valores vacíos significativos.");
        }
        dataset.setQualityInsights(qualityInsights);

        datasetRepository.save(dataset);
    }
}
