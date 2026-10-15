package com.nexainsights.controller;

import com.nexainsights.dto.CellEditRequestDto;
import com.nexainsights.model.DataRecordAudit;
import com.nexainsights.repository.DataRecordAuditRepository;
import com.nexainsights.service.DataEditorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/editor")
public class DataEditorController {

    @Autowired
    private DataEditorService dataEditorService;

    @Autowired
    private DataRecordAuditRepository auditRepository;

    @PutMapping("/datasets/{datasetId}/cell")
    public ResponseEntity<Map<String, Object>> updateCell(
            @PathVariable String datasetId,
            @RequestBody CellEditRequestDto editDto,
            @RequestParam(required = false, defaultValue = "Luis Cárdenas") String user) {
        try {
            Map<String, Object> result = dataEditorService.updateCell(datasetId, editDto, user);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    @PostMapping("/datasets/{datasetId}/rows")
    public ResponseEntity<Map<String, Object>> addRow(
            @PathVariable String datasetId,
            @RequestBody(required = false) Map<String, Object> initialData,
            @RequestParam(required = false, defaultValue = "Luis Cárdenas") String user) {
        try {
            Map<String, Object> result = dataEditorService.addRow(datasetId, initialData, user);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    @DeleteMapping("/datasets/{datasetId}/records/{recordId}")
    public ResponseEntity<Map<String, Object>> deleteRow(
            @PathVariable String datasetId,
            @PathVariable String recordId,
            @RequestParam(required = false, defaultValue = "Luis Cárdenas") String user) {
        try {
            Map<String, Object> result = dataEditorService.deleteRow(datasetId, recordId, user);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    @GetMapping("/datasets/{datasetId}/audits")
    public ResponseEntity<List<DataRecordAudit>> getAudits(@PathVariable String datasetId) {
        List<DataRecordAudit> audits = auditRepository.findByDatasetIdOrderByTimestampDesc(datasetId);
        return ResponseEntity.ok(audits);
    }

    @PostMapping("/audits/{auditId}/rollback")
    public ResponseEntity<Map<String, Object>> rollbackAudit(@PathVariable String auditId) {
        try {
            Map<String, Object> result = dataEditorService.rollbackAudit(auditId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }
}
