package com.nexainsights.service;

import com.nexainsights.dto.PreviewResponseDto;
import com.nexainsights.model.ColumnDefinition;
import com.nexainsights.model.DataRecord;
import com.nexainsights.model.DataType;
import com.nexainsights.model.Dataset;
import com.nexainsights.repository.DataRecordRepository;
import com.nexainsights.repository.DatasetRepository;
import com.nexainsights.util.TypeInferenceEngine;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import org.apache.commons.io.FilenameUtils;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
public class FileImportService {

    private static final Logger log = LoggerFactory.getLogger(FileImportService.class);

    @Autowired
    private DatasetRepository datasetRepository;

    @Autowired
    private DataRecordRepository dataRecordRepository;

    @Autowired
    private DataProfilingService profilingService;

    @Autowired
    private DashboardGenerationService dashboardService;

    /**
     * Genera la vista previa de un archivo antes de confirmar la importación
     */
    public PreviewResponseDto generatePreview(MultipartFile file, String requestedSheet, Integer headerRowIndex) throws Exception {
        String fileName = file.getOriginalFilename();
        String extension = FilenameUtils.getExtension(fileName).toLowerCase();

        PreviewResponseDto preview = new PreviewResponseDto();
        preview.setFileName(fileName);
        preview.setFileSize(file.getSize());
        preview.setFormat(extension.toUpperCase());

        int headerIdx = headerRowIndex != null ? headerRowIndex : 0;
        preview.setDetectedHeaderRow(headerIdx);

        if (extension.equals("xlsx") || extension.equals("xls")) {
            previewExcel(file.getInputStream(), preview, requestedSheet, headerIdx);
        } else if (extension.equals("csv") || extension.equals("tsv") || extension.equals("txt")) {
            previewCsv(file.getInputStream(), preview, headerIdx, extension.equals("tsv") ? '\t' : ',');
        } else {
            throw new IllegalArgumentException("Formato no soportado directamente: ." + extension + ". Utiliza XLSX, XLS o CSV.");
        }

        return preview;
    }

    private void previewExcel(InputStream is, PreviewResponseDto preview, String requestedSheet, int headerRowIndex) throws Exception {
        try (Workbook workbook = WorkbookFactory.create(is)) {
            List<String> sheets = new ArrayList<>();
            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                sheets.add(workbook.getSheetName(i));
            }
            preview.setAvailableSheets(sheets);

            Sheet sheet = (requestedSheet != null && !requestedSheet.isEmpty())
                    ? workbook.getSheet(requestedSheet)
                    : workbook.getSheetAt(0);

            if (sheet == null) sheet = workbook.getSheetAt(0);
            preview.setSelectedSheet(sheet.getSheetName());

            DataFormatter formatter = new DataFormatter();
            Row headerRow = sheet.getRow(headerRowIndex);
            if (headerRow == null) {
                // Intentar buscar la primera fila con datos si la indicada está vacía
                for (int r = 0; r <= 10; r++) {
                    Row rRow = sheet.getRow(r);
                    if (rRow != null && rRow.getLastCellNum() > 0) {
                        headerRow = rRow;
                        preview.setDetectedHeaderRow(r);
                        break;
                    }
                }
            }

            if (headerRow == null) {
                throw new IllegalStateException("No se encontraron filas con encabezados en la hoja " + sheet.getSheetName());
            }

            int lastCellNum = headerRow.getLastCellNum();
            List<String> headers = new ArrayList<>();
            for (int c = 0; c < lastCellNum; c++) {
                Cell cell = headerRow.getCell(c);
                String val = cell != null ? formatter.formatCellValue(cell).trim() : "";
                if (val.isEmpty()) {
                    val = "Columna_" + (c + 1);
                }
                headers.add(val);
            }
            preview.setOriginalHeaders(headers);
            preview.setTotalColumns(headers.size());

            // Extraer primeras 10 filas para preview
            List<Map<String, Object>> previewRows = new ArrayList<>();
            int totalPhysicalRows = sheet.getPhysicalNumberOfRows();
            preview.setEstimatedTotalRows(Math.max(0, totalPhysicalRows - 1));

            int startRow = preview.getDetectedHeaderRow() + 1;
            int rowsCollected = 0;

            List<DataType> inferredColumnTypes = new ArrayList<>();
            for (int i = 0; i < headers.size(); i++) inferredColumnTypes.add(null);

            for (int r = startRow; r <= sheet.getLastRowNum() && rowsCollected < 10; r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;

                Map<String, Object> rowMap = new LinkedHashMap<>();
                boolean hasContent = false;

                for (int c = 0; c < headers.size(); c++) {
                    Cell cell = row.getCell(c);
                    String cellVal = cell != null ? formatter.formatCellValue(cell) : "";
                    if (!cellVal.isEmpty()) hasContent = true;
                    rowMap.put(headers.get(c), cellVal);

                    if (inferredColumnTypes.get(c) == null && !cellVal.isEmpty()) {
                        inferredColumnTypes.set(c, TypeInferenceEngine.inferSingleValueType(cellVal));
                    }
                }

                if (hasContent) {
                    previewRows.add(rowMap);
                    rowsCollected++;
                }
            }

            List<String> typesStr = new ArrayList<>();
            for (DataType dt : inferredColumnTypes) {
                typesStr.add(dt != null ? dt.name() : DataType.TEXT.name());
            }
            preview.setDetectedTypes(typesStr);
            preview.setPreviewRows(previewRows);
        }
    }

    private void previewCsv(InputStream is, PreviewResponseDto preview, int headerRowIndex, char separator) throws Exception {
        preview.setAvailableSheets(Collections.singletonList("Principal"));
        preview.setSelectedSheet("Principal");

        try (CSVReader reader = new CSVReaderBuilder(new InputStreamReader(is, StandardCharsets.UTF_8))
                .withCSVParser(new CSVParserBuilder().withSeparator(separator).build())
                .build()) {

            List<String[]> allRows = new ArrayList<>();
            String[] line;
            int counter = 0;
            while ((line = reader.readNext()) != null && counter < 15) {
                allRows.add(line);
                counter++;
            }

            if (allRows.isEmpty() || headerRowIndex >= allRows.size()) {
                throw new IllegalStateException("El archivo delimitado está vacío o no tiene encabezados válidos.");
            }

            String[] headerArr = allRows.get(headerRowIndex);
            List<String> headers = new ArrayList<>();
            for (int i = 0; i < headerArr.length; i++) {
                String h = headerArr[i].trim();
                headers.add(h.isEmpty() ? "Columna_" + (i + 1) : h);
            }
            preview.setOriginalHeaders(headers);
            preview.setTotalColumns(headers.size());

            List<Map<String, Object>> previewRows = new ArrayList<>();
            List<DataType> inferredColumnTypes = new ArrayList<>();
            for (int i = 0; i < headers.size(); i++) inferredColumnTypes.add(null);

            for (int r = headerRowIndex + 1; r < allRows.size() && previewRows.size() < 10; r++) {
                String[] row = allRows.get(r);
                Map<String, Object> map = new LinkedHashMap<>();
                for (int c = 0; c < headers.size(); c++) {
                    String val = c < row.length ? row[c].trim() : "";
                    map.put(headers.get(c), val);
                    if (inferredColumnTypes.get(c) == null && !val.isEmpty()) {
                        inferredColumnTypes.set(c, TypeInferenceEngine.inferSingleValueType(val));
                    }
                }
                previewRows.add(map);
            }

            List<String> typesStr = new ArrayList<>();
            for (DataType dt : inferredColumnTypes) {
                typesStr.add(dt != null ? dt.name() : DataType.TEXT.name());
            }
            preview.setDetectedTypes(typesStr);
            preview.setPreviewRows(previewRows);
            preview.setEstimatedTotalRows(allRows.size());
        }
    }

    /**
     * Importación definitiva: Guarda metadatos, perfila columnas y persiste en MongoDB por lotes
     */
    public Dataset importFile(MultipartFile file, String projectId, String datasetName,
                              String requestedSheet, Integer headerRowIndex) throws Exception {

        String fileName = file.getOriginalFilename();
        String extension = FilenameUtils.getExtension(fileName).toLowerCase();
        int headerIdx = headerRowIndex != null ? headerRowIndex : 0;

        String name = (datasetName != null && !datasetName.trim().isEmpty())
                ? datasetName.trim()
                : FilenameUtils.getBaseName(fileName);

        Dataset dataset = new Dataset(projectId, name, fileName, extension.toUpperCase());
        dataset.setFileSize(file.getSize());
        dataset.setHeaderRowIndex(headerIdx);
        dataset = datasetRepository.save(dataset);

        List<ColumnDefinition> columns = new ArrayList<>();
        List<DataRecord> recordsBatch = new ArrayList<>();
        int batchSize = 1000;
        int totalSavedRows = 0;

        if (extension.equals("xlsx") || extension.equals("xls")) {
            try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
                Sheet sheet = (requestedSheet != null && !requestedSheet.isEmpty())
                        ? workbook.getSheet(requestedSheet)
                        : workbook.getSheetAt(0);
                if (sheet == null) sheet = workbook.getSheetAt(0);
                dataset.setSheetName(sheet.getSheetName());

                DataFormatter formatter = new DataFormatter();
                FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
                Row headerRow = sheet.getRow(headerIdx);
                if (headerRow == null) throw new IllegalStateException("Fila de encabezado inválida.");

                int lastCol = headerRow.getLastCellNum();
                Set<String> seenHeaders = new HashSet<>();
                for (int c = 0; c < lastCol; c++) {
                    Cell cell = headerRow.getCell(c);
                    String origName = "";
                    if (cell != null) {
                        try {
                            origName = formatter.formatCellValue(cell, evaluator).trim();
                        } catch (Exception e) {
                            origName = formatter.formatCellValue(cell).trim();
                        }
                    }
                    // Limpieza de fórmulas y encabezados basura como SUM(...) o números solos
                    if (origName.startsWith("=") || origName.toUpperCase().startsWith("SUM(") || origName.toUpperCase().startsWith("PROM(") || origName.toUpperCase().startsWith("COUNT(")) {
                        origName = "";
                    }
                    if (origName.isEmpty()) origName = "Campo_" + (c + 1);

                    // Si hay nombres repetidos en el archivo, sufijar con número único
                    String uniqueHeader = origName;
                    int dupIdx = 2;
                    while (seenHeaders.contains(uniqueHeader.toLowerCase())) {
                        uniqueHeader = origName + " (" + dupIdx + ")";
                        dupIdx++;
                    }
                    seenHeaders.add(uniqueHeader.toLowerCase());

                    String colKey = "col_" + c;
                    ColumnDefinition colDef = new ColumnDefinition(colKey, uniqueHeader, DataType.TEXT);
                    columns.add(colDef);
                    dataset.getNameToKeyMap().put(uniqueHeader, colKey);
                }

                // Recorrer filas de datos
                for (int r = headerIdx + 1; r <= sheet.getLastRowNum(); r++) {
                    Row row = sheet.getRow(r);
                    if (row == null) continue;

                    Map<String, Object> data = new HashMap<>();
                    boolean rowHasData = false;

                    for (int c = 0; c < columns.size(); c++) {
                        Cell cell = row.getCell(c);
                        String rawVal = "";
                        if (cell != null) {
                            try {
                                if (cell.getCellType() == CellType.FORMULA) {
                                    CellValue cellValue = evaluator.evaluate(cell);
                                    if (cellValue != null) {
                                        switch (cellValue.getCellType()) {
                                            case BOOLEAN:
                                                rawVal = String.valueOf(cellValue.getBooleanValue());
                                                break;
                                            case NUMERIC:
                                                if (DateUtil.isCellDateFormatted(cell)) {
                                                    Date d = DateUtil.getJavaDate(cellValue.getNumberValue());
                                                    rawVal = sanitizeDateValue(d);
                                                } else {
                                                    double num = cellValue.getNumberValue();
                                                    rawVal = (num == (long) num) ? String.format("%d", (long) num) : String.valueOf(num);
                                                }
                                                break;
                                            case STRING:
                                                rawVal = cellValue.getStringValue();
                                                break;
                                            default:
                                                rawVal = "";
                                        }
                                    }
                                } else if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
                                    Date d = cell.getDateCellValue();
                                    rawVal = sanitizeDateValue(d);
                                } else {
                                    rawVal = formatter.formatCellValue(cell, evaluator);
                                }
                            } catch (Exception ex) {
                                rawVal = formatter.formatCellValue(cell);
                            }
                        }

                        // Limpieza de valores nulos o fórmulas no resueltas de Excel (#N/A, #VALUE!, #REF!)
                        if (rawVal != null) {
                            rawVal = rawVal.trim();
                            if (rawVal.startsWith("#") || rawVal.equalsIgnoreCase("1899-12-31") || rawVal.equalsIgnoreCase("1900-01-00") || rawVal.equalsIgnoreCase("00/00/0000")) {
                                rawVal = "";
                            }
                        } else {
                            rawVal = "";
                        }

                        if (!rawVal.isEmpty()) rowHasData = true;
                        data.put(columns.get(c).getKey(), rawVal);
                    }

                    if (rowHasData) {
                        // Conservamos el número de fila original de Excel (1-based: r + 1)
                        DataRecord record = new DataRecord(dataset.getId(), projectId, r + 1, data);
                        recordsBatch.add(record);
                        totalSavedRows++;

                        if (recordsBatch.size() >= batchSize) {
                            dataRecordRepository.saveAll(recordsBatch);
                            recordsBatch.clear();
                        }
                    }
                }
            }
        } else if (extension.equals("csv") || extension.equals("tsv") || extension.equals("txt")) {
            char sep = extension.equals("tsv") ? '\t' : ',';
            dataset.setSheetName("Principal");

            try (CSVReader reader = new CSVReaderBuilder(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))
                    .withCSVParser(new CSVParserBuilder().withSeparator(sep).build())
                    .build()) {

                String[] line;
                int currentFileRow = 0;

                while ((line = reader.readNext()) != null) {
                    currentFileRow++;
                    if (currentFileRow - 1 < headerIdx) continue;

                    if (currentFileRow - 1 == headerIdx) {
                        Set<String> seenHeaders = new HashSet<>();
                        for (int c = 0; c < line.length; c++) {
                            String origName = line[c].trim();
                            if (origName.isEmpty()) origName = "Columna_" + (c + 1);

                            String uniqueHeader = origName;
                            int dupIdx = 2;
                            while (seenHeaders.contains(uniqueHeader.toLowerCase())) {
                                uniqueHeader = origName + " (" + dupIdx + ")";
                                dupIdx++;
                            }
                            seenHeaders.add(uniqueHeader.toLowerCase());

                            String colKey = "col_" + c;
                            ColumnDefinition colDef = new ColumnDefinition(colKey, uniqueHeader, DataType.TEXT);
                            columns.add(colDef);
                            dataset.getNameToKeyMap().put(uniqueHeader, colKey);
                        }
                        continue;
                    }

                    // Filas de datos
                    Map<String, Object> data = new HashMap<>();
                    boolean hasContent = false;
                    for (int c = 0; c < columns.size(); c++) {
                        String val = c < line.length ? line[c].trim() : "";
                        if (!val.isEmpty()) hasContent = true;
                        data.put(columns.get(c).getKey(), val);
                    }

                    if (hasContent) {
                        DataRecord record = new DataRecord(dataset.getId(), projectId, currentFileRow, data);
                        recordsBatch.add(record);
                        totalSavedRows++;

                        if (recordsBatch.size() >= batchSize) {
                            dataRecordRepository.saveAll(recordsBatch);
                            recordsBatch.clear();
                        }
                    }
                }
            }
        }

        if (!recordsBatch.isEmpty()) {
            dataRecordRepository.saveAll(recordsBatch);
            recordsBatch.clear();
        }

        dataset.setColumns(columns);
        dataset.setTotalColumns(columns.size());
        dataset.setTotalRows(totalSavedRows);
        dataset = datasetRepository.save(dataset);

        // 3. Perfilado de datos inteligente y métricas estadísticas
        profilingService.profileDataset(dataset);

        // 4. Generación automática del dashboard inicial adaptado a las columnas detectadas
        dashboardService.generateDefaultDashboard(dataset);

        return dataset;
    }

    private String sanitizeDateValue(Date date) {
        if (date == null) return "";
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        int year = cal.get(Calendar.YEAR);
        if (year < 1920 || year > 2100) {
            return ""; // Evita fechas anómalas de Excel como 1899-12-31 o 1900-01-00
        }
        return new SimpleDateFormat("yyyy-MM-dd").format(date);
    }
}
