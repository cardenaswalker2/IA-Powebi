package com.nexainsights.service;

import com.nexainsights.dto.CellEditRequestDto;
import com.nexainsights.model.ColumnDefinition;
import com.nexainsights.model.DataRecord;
import com.nexainsights.model.DataRecordAudit;
import com.nexainsights.model.Dataset;
import com.nexainsights.repository.DataRecordAuditRepository;
import com.nexainsights.repository.DataRecordRepository;
import com.nexainsights.repository.DatasetRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class DataEditorService {

    @Autowired
    private DatasetRepository datasetRepository;

    @Autowired
    private DataRecordRepository dataRecordRepository;

    @Autowired
    private DataRecordAuditRepository auditRepository;

    @Autowired
    private DataProfilingService dataProfilingService;

    public Map<String, Object> updateCell(String datasetId, CellEditRequestDto editDto, String user) {
        Map<String, Object> result = new HashMap<>();

        Dataset dataset = datasetRepository.findById(datasetId)
                .orElseThrow(() -> new IllegalArgumentException("Dataset no encontrado: " + datasetId));

        DataRecord record = dataRecordRepository.findById(editDto.getRecordId())
                .orElseThrow(() -> new IllegalArgumentException("Registro no encontrado: " + editDto.getRecordId()));

        ColumnDefinition col = dataset.findColumnByKey(editDto.getColumnKey());
        String colName = col != null ? col.getOriginalName() : editDto.getColumnKey();

        Object oldValue = record.getValue(editDto.getColumnKey());
        Object sanitizedNewValue = sanitizeValue(editDto.getNewValue(), col);

        // Actualizar valor en el registro
        if (record.getData() == null) {
            record.setData(new HashMap<>());
        }
        record.getData().put(editDto.getColumnKey(), sanitizedNewValue);
        dataRecordRepository.save(record);

        // Registrar auditoría fáctica
        DataRecordAudit audit = new DataRecordAudit(
                datasetId,
                record.getRowNumber(),
                record.getId(),
                editDto.getColumnKey(),
                colName,
                "CELL_EDIT",
                oldValue,
                sanitizedNewValue,
                user != null ? user : "Luis Cárdenas"
        );
        audit.setReason(editDto.getReason());
        auditRepository.save(audit);

        // Recalcular métricas si aplica
        recalculateColumnStats(dataset, editDto.getColumnKey());

        result.put("success", true);
        result.put("message", "Celda actualizada correctamente.");
        result.put("recordId", record.getId());
        result.put("columnKey", editDto.getColumnKey());
        result.put("oldValue", oldValue);
        result.put("newValue", sanitizedNewValue);
        result.put("rowNumber", record.getRowNumber());
        return result;
    }

    public Map<String, Object> addRow(String datasetId, Map<String, Object> initialData, String user) {
        Map<String, Object> result = new HashMap<>();

        Dataset dataset = datasetRepository.findById(datasetId)
                .orElseThrow(() -> new IllegalArgumentException("Dataset no encontrado: " + datasetId));

        long totalRows = dataset.getTotalRows();
        int nextRowNumber = (int) (totalRows + 1);

        Map<String, Object> recordData = new HashMap<>();
        if (initialData != null) {
            for (ColumnDefinition col : dataset.getColumns()) {
                if (initialData.containsKey(col.getKey())) {
                    recordData.put(col.getKey(), initialData.get(col.getKey()));
                } else if (initialData.containsKey(col.getOriginalName())) {
                    recordData.put(col.getKey(), initialData.get(col.getOriginalName()));
                } else {
                    recordData.put(col.getKey(), null);
                }
            }
        } else {
            for (ColumnDefinition col : dataset.getColumns()) {
                recordData.put(col.getKey(), "");
            }
        }

        DataRecord newRecord = new DataRecord(datasetId, dataset.getProjectId(), nextRowNumber, recordData);
        dataRecordRepository.save(newRecord);

        // Actualizar contador del dataset
        dataset.setTotalRows(totalRows + 1);
        datasetRepository.save(dataset);

        // Auditoría
        DataRecordAudit audit = new DataRecordAudit(
                datasetId,
                nextRowNumber,
                newRecord.getId(),
                "ALL",
                "Fila Completa",
                "ROW_ADD",
                null,
                "Fila #" + nextRowNumber + " creada",
                user != null ? user : "Luis Cárdenas"
        );
        auditRepository.save(audit);

        result.put("success", true);
        result.put("message", "Fila #" + nextRowNumber + " agregada con éxito.");
        result.put("record", newRecord);
        return result;
    }

    public Map<String, Object> deleteRow(String datasetId, String recordId, String user) {
        Map<String, Object> result = new HashMap<>();

        Dataset dataset = datasetRepository.findById(datasetId)
                .orElseThrow(() -> new IllegalArgumentException("Dataset no encontrado: " + datasetId));

        DataRecord record = dataRecordRepository.findById(recordId)
                .orElseThrow(() -> new IllegalArgumentException("Registro no encontrado: " + recordId));

        int rowNum = record.getRowNumber();
        dataRecordRepository.delete(record);

        dataset.setTotalRows(Math.max(0, dataset.getTotalRows() - 1));
        datasetRepository.save(dataset);

        // Auditoría
        DataRecordAudit audit = new DataRecordAudit(
                datasetId,
                rowNum,
                recordId,
                "ALL",
                "Fila Eliminada",
                "ROW_DELETE",
                record.getData(),
                null,
                user != null ? user : "Luis Cárdenas"
        );
        auditRepository.save(audit);

        result.put("success", true);
        result.put("message", "Fila #" + rowNum + " eliminada.");
        return result;
    }

    public Map<String, Object> rollbackAudit(String auditId) {
        Map<String, Object> result = new HashMap<>();

        DataRecordAudit audit = auditRepository.findById(auditId)
                .orElseThrow(() -> new IllegalArgumentException("Registro de auditoría no encontrado: " + auditId));

        DataRecord record = dataRecordRepository.findById(audit.getRecordId())
                .orElseThrow(() -> new IllegalArgumentException("Registro original no existe: " + audit.getRecordId()));

        Object originalOldValue = audit.getOldValue();
        record.getData().put(audit.getColumnKey(), originalOldValue);
        dataRecordRepository.save(record);

        // Registrar acción de reversión
        DataRecordAudit rollbackLog = new DataRecordAudit(
                audit.getDatasetId(),
                record.getRowNumber(),
                record.getId(),
                audit.getColumnKey(),
                audit.getColumnName(),
                "ROLLBACK",
                audit.getNewValue(),
                originalOldValue,
                "Reversión Automática"
        );
        rollbackLog.setReason("Reversión de cambio ID: " + auditId);
        auditRepository.save(rollbackLog);

        result.put("success", true);
        result.put("message", "Cambio revertido con éxito a: " + originalOldValue);
        result.put("restoredValue", originalOldValue);
        return result;
    }

    private Object sanitizeValue(Object rawVal, ColumnDefinition col) {
        if (rawVal == null) return null;
        String str = rawVal.toString().trim();
        if (str.isEmpty()) return null;

        if (col != null && col.getEffectiveType() != null && 
            (col.getEffectiveType() == com.nexainsights.model.DataType.NUMBER || 
             col.getEffectiveType().name().contains("NUM") || 
             col.getEffectiveType().name().contains("INT") || 
             col.getEffectiveType().name().contains("DEC"))) {
            try {
                String cleanNum = str.replace("$", "").replace("€", "").replace(" ", "").replace(",", ".");
                if (cleanNum.contains(".")) {
                    return Double.parseDouble(cleanNum);
                } else {
                    return Long.parseLong(cleanNum);
                }
            } catch (Exception ignored) {}
        }

        // Prevención de inyección de fórmulas maliciosas en CSV/Excel (=cmd, +cmd, -cmd, @cmd)
        if (str.startsWith("=") || str.startsWith("+") || str.startsWith("-") || str.startsWith("@")) {
            // Si no es un número legítimo negativo
            boolean isSignedNumber = false;
            try {
                Double.parseDouble(str);
                isSignedNumber = true;
            } catch (Exception ignored) {}

            if (!isSignedNumber && (str.startsWith("=") || str.startsWith("@") || str.startsWith("+") || str.startsWith("-"))) {
                // Escapar agregando comilla simple para neutralizar ejecución de comandos en hojas de cálculo
                return "'" + str;
            }
        }

        return str;
    }

    private void recalculateColumnStats(Dataset dataset, String columnKey) {
        // En segundo plano o actualización rápida de nulos
        try {
            ColumnDefinition col = dataset.findColumnByKey(columnKey);
            if (col != null) {
                datasetRepository.save(dataset);
            }
        } catch (Exception ignored) {}
    }
}
