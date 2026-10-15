package com.nexainsights.controller;

import com.nexainsights.model.ColumnDefinition;
import com.nexainsights.model.DataRecord;
import com.nexainsights.model.Dataset;
import com.nexainsights.repository.DataRecordRepository;
import com.nexainsights.repository.DatasetRepository;
import com.opencsv.CSVWriter;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Controller
@RequestMapping("/reports")
public class ReportExportController {

    @Autowired
    private DatasetRepository datasetRepository;

    @Autowired
    private DataRecordRepository dataRecordRepository;

    @GetMapping
    public String showReportsView(@RequestParam(required = false) String datasetId, Model model) {
        List<Dataset> datasets = datasetRepository.findAll();
        if (datasets.isEmpty()) {
            return "redirect:/import";
        }

        Dataset selected = (datasetId != null && !datasetId.isEmpty())
                ? datasetRepository.findById(datasetId).orElse(datasets.get(0))
                : datasets.get(0);

        model.addAttribute("datasets", datasets);
        model.addAttribute("selectedDataset", selected);
        return "reports/index";
    }

    @GetMapping("/export/csv/{datasetId}")
    public ResponseEntity<byte[]> exportCsv(@PathVariable String datasetId) throws Exception {
        Dataset dataset = datasetRepository.findById(datasetId).orElseThrow();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try (CSVWriter writer = new CSVWriter(new OutputStreamWriter(baos, StandardCharsets.UTF_8))) {
            // Encabezados
            List<ColumnDefinition> cols = dataset.getColumns();
            String[] headers = new String[cols.size() + 1];
            headers[0] = "Fila_Origen";
            for (int i = 0; i < cols.size(); i++) {
                headers[i + 1] = cols.get(i).getOriginalName();
            }
            writer.writeNext(headers);

            int page = 0;
            int size = 1000;
            List<DataRecord> records;
            do {
                records = dataRecordRepository.findByDatasetId(datasetId, PageRequest.of(page, size)).getContent();
                for (DataRecord rec : records) {
                    String[] row = new String[cols.size() + 1];
                    row[0] = String.valueOf(rec.getRowNumber());
                    for (int i = 0; i < cols.size(); i++) {
                        Object val = rec.getValue(cols.get(i).getKey());
                        row[i + 1] = val != null ? val.toString() : "";
                    }
                    writer.writeNext(row);
                }
                page++;
            } while (!records.isEmpty());
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + dataset.getName() + "_export.csv\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(baos.toByteArray());
    }

    @GetMapping("/export/xlsx/{datasetId}")
    public ResponseEntity<byte[]> exportXlsx(@PathVariable String datasetId) throws Exception {
        Dataset dataset = datasetRepository.findById(datasetId).orElseThrow();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Datos");

            // Estilo encabezado
            CellStyle headerStyle = workbook.createCellStyle();
            Font font = workbook.createFont();
            font.setBold(true);
            headerStyle.setFont(font);

            Row headerRow = sheet.createRow(0);
            List<ColumnDefinition> cols = dataset.getColumns();
            Cell h0 = headerRow.createCell(0);
            h0.setCellValue("Fila Origen");
            h0.setCellStyle(headerStyle);

            for (int i = 0; i < cols.size(); i++) {
                Cell cell = headerRow.createCell(i + 1);
                cell.setCellValue(cols.get(i).getOriginalName());
                cell.setCellStyle(headerStyle);
            }

            int page = 0;
            int size = 1000;
            int currentRowIdx = 1;
            List<DataRecord> records;
            do {
                records = dataRecordRepository.findByDatasetId(datasetId, PageRequest.of(page, size)).getContent();
                for (DataRecord rec : records) {
                    Row row = sheet.createRow(currentRowIdx++);
                    row.createCell(0).setCellValue(rec.getRowNumber());
                    for (int i = 0; i < cols.size(); i++) {
                        Object val = rec.getValue(cols.get(i).getKey());
                        row.createCell(i + 1).setCellValue(val != null ? val.toString() : "");
                    }
                }
                page++;
            } while (!records.isEmpty() && currentRowIdx < 65000); // Límite seguro

            workbook.write(baos);
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + dataset.getName() + "_export.xlsx\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(baos.toByteArray());
    }
}
