package com.nexainsights;

import com.nexainsights.dto.CellEditRequestDto;
import com.nexainsights.model.ColumnDefinition;
import com.nexainsights.model.DataRecord;
import com.nexainsights.model.DataRecordAudit;
import com.nexainsights.model.Dataset;
import com.nexainsights.repository.DataRecordAuditRepository;
import com.nexainsights.repository.DataRecordRepository;
import com.nexainsights.repository.DatasetRepository;
import com.nexainsights.service.DataEditorService;
import com.nexainsights.service.PdfReportGenerationService;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.ByteArrayInputStream;
import java.util.*;

@SpringBootTest
public class FunctionalAuditAndTrazabilidadTest {

    @Autowired
    private DatasetRepository datasetRepository;

    @Autowired
    private DataRecordRepository dataRecordRepository;

    @Autowired
    private DataRecordAuditRepository auditRepository;

    @Autowired
    private DataEditorService dataEditorService;

    @Autowired
    private PdfReportGenerationService pdfReportService;

    @Autowired
    private com.nexainsights.controller.ReportExportController reportExportController;

    @Test
    public void testCompleteEditorAuditRollbackPdfAndExcelWorkflow() throws Exception {
        // 1. Crear un Dataset aislado de auditoría para pruebas funcionales reales
        Dataset testDataset = new Dataset("proj_audit_test", "Dataset_Auditoria_Real", "test_auditoria.xlsx", "XLSX");
        testDataset.setTotalRows(5);
        testDataset.setTotalColumns(3);
        testDataset.setOverallQualityScore(98.5);

        ColumnDefinition col1 = new ColumnDefinition("col_0", "Codigo", com.nexainsights.model.DataType.TEXT);
        ColumnDefinition col2 = new ColumnDefinition("col_1", "HorasTrabajadas", com.nexainsights.model.DataType.NUMBER);
        ColumnDefinition col3 = new ColumnDefinition("col_2", "Responsable", com.nexainsights.model.DataType.TEXT);
        testDataset.getColumns().addAll(Arrays.asList(col1, col2, col3));
        testDataset = datasetRepository.save(testDataset);
        Assertions.assertNotNull(testDataset.getId());

        // 2. Insertar registros iniciales en MongoDB
        List<DataRecord> records = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            Map<String, Object> data = new HashMap<>();
            data.put("col_0", "REG-00" + i);
            data.put("col_1", 40.0 + (i * 2.5));
            data.put("col_2", "Auditor " + i);
            records.add(new DataRecord(testDataset.getId(), testDataset.getProjectId(), i, data));
        }
        dataRecordRepository.saveAll(records);
        DataRecord targetRecord = records.get(1); // Fila 2

        // 3. EDITAR CELDA: Cambiar HorasTrabajadas de Fila 2 de 45.0 a 60.5
        CellEditRequestDto editDto = new CellEditRequestDto();
        editDto.setRecordId(targetRecord.getId());
        editDto.setColumnKey("col_1");
        editDto.setNewValue(60.5);
        editDto.setReason("Ajuste horas extraordinarias aprobadas");

        Map<String, Object> editResult = dataEditorService.updateCell(testDataset.getId(), editDto, "Luis Cárdenas");
        Assertions.assertTrue((Boolean) editResult.get("success"));
        Assertions.assertEquals(60.5, editResult.get("newValue"));

        // 4. VERIFICAR PERSISTENCIA REAL EN MONGODB
        DataRecord updatedRecord = dataRecordRepository.findById(targetRecord.getId()).orElseThrow();
        Assertions.assertEquals(60.5, updatedRecord.getValue("col_1"));

        // 5. VERIFICAR REGISTRO DE AUDITORÍA
        List<DataRecordAudit> audits = auditRepository.findByDatasetIdOrderByTimestampDesc(testDataset.getId());
        Assertions.assertFalse(audits.isEmpty());
        DataRecordAudit firstAudit = audits.get(0);
        Assertions.assertEquals("CELL_EDIT", firstAudit.getOperation());
        Assertions.assertEquals(45.0, firstAudit.getOldValue());
        Assertions.assertEquals(60.5, firstAudit.getNewValue());
        Assertions.assertEquals("Luis Cárdenas", firstAudit.getModifiedBy());

        // 6. PROBAR REVERSIÓN (ROLLBACK) DEL CAMBIO
        Map<String, Object> rollbackResult = dataEditorService.rollbackAudit(firstAudit.getId());
        Assertions.assertTrue((Boolean) rollbackResult.get("success"));

        // Confirmar que el valor original (45.0) fue restaurado en la BD
        DataRecord restoredRecord = dataRecordRepository.findById(targetRecord.getId()).orElseThrow();
        Assertions.assertEquals(45.0, restoredRecord.getValue("col_1"));

        // Confirmar que la acción de reversión también quedó auditada
        List<DataRecordAudit> postRollbackAudits = auditRepository.findByDatasetIdOrderByTimestampDesc(testDataset.getId());
        Assertions.assertEquals(2, postRollbackAudits.size());
        Assertions.assertEquals("ROLLBACK", postRollbackAudits.get(0).getOperation());

        // 7. AGREGAR NUEVA FILA Y ELIMINAR FILA
        Map<String, Object> addRowResult = dataEditorService.addRow(testDataset.getId(), Map.of("col_0", "REG-006", "col_1", 38.0, "col_2", "Nuevo Miembro"), "Luis Cárdenas");
        Assertions.assertTrue((Boolean) addRowResult.get("success"));
        DataRecord newRecord = (DataRecord) addRowResult.get("record");
        Assertions.assertEquals(6, newRecord.getRowNumber());

        // 8. GENERAR Y VALIDAR INFORME PDF EJECUTIVO
        byte[] execPdfBytes = pdfReportService.generateExecutivePdf(testDataset, "executive", "Control de horas y costos");
        Assertions.assertNotNull(execPdfBytes);
        Assertions.assertTrue(execPdfBytes.length > 1000, "El PDF ejecutivo debe contener estructura válida");
        // Verificar firma mágica de archivo PDF (%PDF)
        String pdfHeader = new String(Arrays.copyOfRange(execPdfBytes, 0, 5));
        Assertions.assertEquals("%PDF-", pdfHeader);

        // 9. GENERAR Y VALIDAR INFORME PDF DE AUDITORÍA
        byte[] auditPdfBytes = pdfReportService.generateExecutivePdf(testDataset, "audit", null);
        Assertions.assertNotNull(auditPdfBytes);
        Assertions.assertTrue(auditPdfBytes.length > 1000);
        Assertions.assertEquals("%PDF-", new String(Arrays.copyOfRange(auditPdfBytes, 0, 5)));

        // 10. GENERAR Y VALIDAR EXPORTACIÓN A EXCEL (.XLSX)
        var xlsxResponse = reportExportController.exportXlsx(testDataset.getId());
        Assertions.assertEquals(200, xlsxResponse.getStatusCode().value());
        byte[] xlsxBytes = xlsxResponse.getBody();
        Assertions.assertNotNull(xlsxBytes);

        try (Workbook wb = new XSSFWorkbook(new ByteArrayInputStream(xlsxBytes))) {
            Assertions.assertTrue(wb.getNumberOfSheets() > 0);
            var sheet = wb.getSheetAt(0);
            Assertions.assertNotNull(sheet.getRow(0)); // Encabezados
            Assertions.assertEquals("Fila Origen", sheet.getRow(0).getCell(0).getStringCellValue());
        }

        // 11. VISTA PREVIA EXCEL
        var previewResponse = reportExportController.previewXlsx(testDataset.getId());
        Assertions.assertEquals(200, previewResponse.getStatusCode().value());
        Assertions.assertNotNull(previewResponse.getBody());
        Assertions.assertEquals(testDataset.getName(), previewResponse.getBody().get("datasetName"));
    }
}
