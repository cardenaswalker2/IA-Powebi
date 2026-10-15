package com.nexainsights.service;

import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.pdf.*;
import com.nexainsights.model.ColumnDefinition;
import com.nexainsights.model.DataRecord;
import com.nexainsights.model.DataRecordAudit;
import com.nexainsights.model.Dataset;
import com.nexainsights.repository.DataRecordAuditRepository;
import com.nexainsights.repository.DataRecordRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class PdfReportGenerationService {

    @Autowired
    private DataRecordRepository dataRecordRepository;

    @Autowired
    private DataRecordAuditRepository auditRepository;

    public byte[] generateExecutivePdf(Dataset dataset, String reportType, String specificScope) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 36, 36, 54, 36);
        PdfWriter writer = PdfWriter.getInstance(document, baos);

        // Header / Footer de página
        HeaderFooterPageEvent event = new HeaderFooterPageEvent(dataset.getName());
        writer.setPageEvent(event);

        document.open();

        // Fuentes de marca NEXA
        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 22, new Color(36, 92, 79));
        Font subtitleFont = FontFactory.getFont(FontFactory.HELVETICA, 12, new Color(90, 100, 95));
        Font sectionHeaderFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, new Color(32, 43, 42));
        Font boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.BLACK);
        Font regularFont = FontFactory.getFont(FontFactory.HELVETICA, 10, new Color(40, 40, 40));
        Font tableHeaderFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.WHITE);
        Font tableCellFont = FontFactory.getFont(FontFactory.HELVETICA, 8, Color.BLACK);
        Font smallMutedFont = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 8, new Color(120, 120, 120));

        // 1. Encabezado / Portada del Informe
        PdfPTable headerTable = new PdfPTable(2);
        headerTable.setWidthPercentage(100);
        headerTable.setWidths(new float[]{70, 30});

        PdfPCell titleCell = new PdfPCell();
        titleCell.setBorder(Rectangle.NO_BORDER);
        titleCell.addElement(new Paragraph("NEXA INSIGHTS", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, new Color(36, 92, 79))));
        
        String reportTitle = "Informe Ejecutivo de Inteligencia de Datos";
        if ("audit".equalsIgnoreCase(reportType)) {
            reportTitle = "Informe de Auditoría y Trazabilidad de Cambios";
        } else if ("technical".equalsIgnoreCase(reportType)) {
            reportTitle = "Informe Técnico de Calidad de Datos y Esquema";
        } else if (specificScope != null && !specificScope.trim().isEmpty()) {
            reportTitle = "Informe Analítico: " + specificScope;
        }

        Paragraph pTitle = new Paragraph(reportTitle, titleFont);
        pTitle.setSpacingBefore(4);
        titleCell.addElement(pTitle);

        Paragraph pSub = new Paragraph("Archivo analizado: " + dataset.getName() + " (" + dataset.getOriginalFileName() + ")", subtitleFont);
        pSub.setSpacingBefore(4);
        titleCell.addElement(pSub);

        headerTable.addCell(titleCell);

        PdfPCell metaCell = new PdfPCell();
        metaCell.setBorder(Rectangle.NO_BORDER);
        metaCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        
        String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
        Paragraph pMeta = new Paragraph();
        pMeta.setAlignment(Element.ALIGN_RIGHT);
        pMeta.add(new Chunk("Fecha Emisión:\n", boldFont));
        pMeta.add(new Chunk(dateStr + "\n", regularFont));
        pMeta.add(new Chunk("Registros: ", boldFont));
        pMeta.add(new Chunk(String.valueOf(dataset.getTotalRows()) + "\n", regularFont));
        pMeta.add(new Chunk("Columnas: ", boldFont));
        pMeta.add(new Chunk(String.valueOf(dataset.getTotalColumns()) + "\n", regularFont));
        metaCell.addElement(pMeta);

        headerTable.addCell(metaCell);
        document.add(headerTable);

        // Línea divisoria verde de marca
        Paragraph separator = new Paragraph(" ");
        separator.setSpacingAfter(10);
        document.add(separator);

        // 2. Resumen Ejecutivo y Alcance
        Paragraph sec1 = new Paragraph("1. Resumen Ejecutivo y Objetivos", sectionHeaderFont);
        sec1.setSpacingBefore(12);
        sec1.setSpacingAfter(6);
        document.add(sec1);

        String scopeText = (specificScope != null && !specificScope.isEmpty())
                ? "Este informe ha sido generado de manera focalizada para responder a la solicitud: \"" + specificScope + "\". "
                : "Este informe ejecutivo consolida el análisis fáctico, métricas clave, control de cambios y validación de calidad sobre los datos activos. ";
        
        scopeText += "NEXA INSIGHTS garantiza la trazabilidad matemática directa a las filas del archivo de origen sin estimaciones subjetivas.";
        
        Paragraph pScope = new Paragraph(scopeText, regularFont);
        pScope.setSpacingAfter(14);
        document.add(pScope);

        // 3. Indicadores Clave de Rendimiento (KPIs)
        Paragraph sec2 = new Paragraph("2. Indicadores Clave del Archivo (KPIs)", sectionHeaderFont);
        sec2.setSpacingBefore(10);
        sec2.setSpacingAfter(8);
        document.add(sec2);

        PdfPTable kpiTable = new PdfPTable(4);
        kpiTable.setWidthPercentage(100);
        kpiTable.setWidths(new float[]{25, 25, 25, 25});

        addKpiBox(kpiTable, "Total Registros", String.valueOf(dataset.getTotalRows()), "Filas de datos procesadas");
        addKpiBox(kpiTable, "Total Columnas", String.valueOf(dataset.getTotalColumns()), "Campos en esquema");
        addKpiBox(kpiTable, "Valores Nulos", String.valueOf(dataset.getTotalMissingValues()), "Celdas vacías detectadas");
        addKpiBox(kpiTable, "Calidad de Datos", String.format("%.1f%%", dataset.getOverallQualityScore()), "Índice de completitud");

        document.add(kpiTable);

        // 4. Muestra Representativa de Registros Actuales
        Paragraph sec3 = new Paragraph("3. Muestra de Registros Activos (con Referencia a Fila Original)", sectionHeaderFont);
        sec3.setSpacingBefore(16);
        sec3.setSpacingAfter(8);
        document.add(sec3);

        List<ColumnDefinition> cols = dataset.getColumns();
        int maxColsToShow = Math.min(cols.size(), 6); // Mostrar hasta 6 columnas principales para legibilidad A4

        PdfPTable dataTable = new PdfPTable(maxColsToShow + 1);
        dataTable.setWidthPercentage(100);

        // Encabezados
        PdfPCell h0 = new PdfPCell(new Phrase("Fila", tableHeaderFont));
        h0.setBackgroundColor(new Color(36, 92, 79));
        h0.setPadding(6);
        dataTable.addCell(h0);

        for (int i = 0; i < maxColsToShow; i++) {
            PdfPCell h = new PdfPCell(new Phrase(cols.get(i).getOriginalName(), tableHeaderFont));
            h.setBackgroundColor(new Color(36, 92, 79));
            h.setPadding(6);
            dataTable.addCell(h);
        }

        // Obtener primeras 8 filas para muestra
        List<DataRecord> sampleRecords = dataRecordRepository.findByDatasetId(dataset.getId(), PageRequest.of(0, 8)).getContent();
        for (DataRecord rec : sampleRecords) {
            PdfPCell c0 = new PdfPCell(new Phrase("#" + rec.getRowNumber(), boldFont));
            c0.setBackgroundColor(new Color(245, 247, 245));
            c0.setPadding(5);
            dataTable.addCell(c0);

            for (int i = 0; i < maxColsToShow; i++) {
                Object val = rec.getValue(cols.get(i).getKey());
                String strVal = val != null ? val.toString() : "";
                PdfPCell cell = new PdfPCell(new Phrase(strVal, tableCellFont));
                cell.setPadding(5);
                dataTable.addCell(cell);
            }
        }
        document.add(dataTable);

        // 5. Historial de Cambios y Trazabilidad Real
        List<DataRecordAudit> audits = auditRepository.findByDatasetIdOrderByTimestampDesc(dataset.getId());
        Paragraph sec4 = new Paragraph("4. Registro de Cambios y Trazabilidad (" + audits.size() + " operaciones registradas)", sectionHeaderFont);
        sec4.setSpacingBefore(16);
        sec4.setSpacingAfter(8);
        document.add(sec4);

        if (audits.isEmpty()) {
            Paragraph noChanges = new Paragraph("No se registran modificaciones manuales en esta versión de trabajo. Los datos coinciden íntegramente con el archivo original importado.", smallMutedFont);
            noChanges.setSpacingAfter(12);
            document.add(noChanges);
        } else {
            PdfPTable auditTable = new PdfPTable(5);
            auditTable.setWidthPercentage(100);
            auditTable.setWidths(new float[]{15, 20, 20, 25, 20});

            addAuditHeaderCell(auditTable, "Fila");
            addAuditHeaderCell(auditTable, "Columna");
            addAuditHeaderCell(auditTable, "Operación");
            addAuditHeaderCell(auditTable, "Valor Previo -> Nuevo");
            addAuditHeaderCell(auditTable, "Fecha / Usuario");

            int auditLimit = Math.min(audits.size(), 10);
            for (int i = 0; i < auditLimit; i++) {
                DataRecordAudit a = audits.get(i);
                auditTable.addCell(new Phrase("#" + a.getRowNumber(), tableCellFont));
                auditTable.addCell(new Phrase(a.getColumnName() != null ? a.getColumnName() : a.getColumnKey(), tableCellFont));
                auditTable.addCell(new Phrase(a.getOperation(), tableCellFont));
                
                String diff = (a.getOldValue() != null ? a.getOldValue().toString() : "(vacío)") + " → " + 
                              (a.getNewValue() != null ? a.getNewValue().toString() : "(vacío)");
                auditTable.addCell(new Phrase(diff, tableCellFont));

                String userTime = a.getTimestamp().format(DateTimeFormatter.ofPattern("dd/MM HH:mm")) + " (" + a.getModifiedBy() + ")";
                auditTable.addCell(new Phrase(userTime, tableCellFont));
            }
            document.add(auditTable);
        }

        // 6. Conclusiones y Recomendaciones
        Paragraph sec5 = new Paragraph("5. Conclusiones y Recomendaciones Analíticas", sectionHeaderFont);
        sec5.setSpacingBefore(16);
        sec5.setSpacingAfter(8);
        document.add(sec5);

        Paragraph concl = new Paragraph();
        concl.setFont(regularFont);
        concl.add(new Chunk("• Consistencia General: ", boldFont));
        concl.add("El archivo presenta un nivel de completitud del " + String.format("%.1f%%", dataset.getOverallQualityScore()) + ". ");
        if (dataset.getTotalMissingValues() > 0) {
            concl.add("Se recomienda revisar las " + dataset.getTotalMissingValues() + " celdas con valores faltantes en el editor de datos integrado.\n");
        } else {
            concl.add("No se detectaron valores vacíos en las columnas estructurales evaluadas.\n");
        }
        concl.add(new Chunk("• Control de Versiones: ", boldFont));
        concl.add("Cualquier ajuste realizado en la plataforma se encuentra registrado con trazabilidad por celda y autor (" + audits.size() + " cambios auditados).\n");
        concl.setSpacingAfter(14);
        document.add(concl);

        document.close();
        return baos.toByteArray();
    }

    private void addKpiBox(PdfPTable table, String label, String value, String desc) {
        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(new Color(248, 249, 246));
        cell.setBorderColor(new Color(220, 224, 218));
        cell.setPadding(8);

        Font valFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, new Color(36, 92, 79));
        Font lblFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, new Color(100, 100, 100));
        Font descFont = FontFactory.getFont(FontFactory.HELVETICA, 7, new Color(130, 130, 130));

        Paragraph p = new Paragraph();
        p.add(new Chunk(label.toUpperCase() + "\n", lblFont));
        p.add(new Chunk(value + "\n", valFont));
        p.add(new Chunk(desc, descFont));
        cell.addElement(p);

        table.addCell(cell);
    }

    private void addAuditHeaderCell(PdfPTable table, String text) {
        PdfPCell c = new PdfPCell(new Phrase(text, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, Color.WHITE)));
        c.setBackgroundColor(new Color(60, 75, 72));
        c.setPadding(5);
        table.addCell(c);
    }

    // Evento de encabezado/pie de página
    private static class HeaderFooterPageEvent extends PdfPageEventHelper {
        private final String datasetName;

        public HeaderFooterPageEvent(String datasetName) {
            this.datasetName = datasetName;
        }

        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            PdfContentByte cb = writer.getDirectContent();
            Font footerFont = FontFactory.getFont(FontFactory.HELVETICA, 8, new Color(130, 130, 130));

            // Encabezado
            Phrase header = new Phrase("NEXA INSIGHTS — Plataforma de Inteligencia Empresarial | Dataset: " + datasetName, footerFont);
            ColumnText.showTextAligned(cb, Element.ALIGN_LEFT, header, document.left(), document.top() + 10, 0);

            // Pie de página con numeración
            Phrase footer = new Phrase("Página " + writer.getPageNumber() + " | Generado automáticamente con datos reales", footerFont);
            ColumnText.showTextAligned(cb, Element.ALIGN_RIGHT, footer, document.right(), document.bottom() - 10, 0);
        }
    }
}
