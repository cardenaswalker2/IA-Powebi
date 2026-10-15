package com.nexainsights.util;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileOutputStream;

/**
 * Generador del archivo de demostración real del usuario: Programa_Auditorias.xlsx
 * con las columnas exactas solicitadas en la captura de pantalla de SharePoint:
 * - Título
 * - CodigoAuditoria
 * - Proceso
 * - AuditorResponsable
 * - ResponsableProceso
 * - FechaProgramada
 * - FechaEjecutada
 * - EstadoAuditoria
 * - ResultadoGeneral
 * - EvidenciaAuditoria
 * - Observaciones
 */
public class SampleAuditDataGenerator {

    public static void main(String[] args) throws Exception {
        String outputPath = "c:/Users/USUARIO/Music/CASA/iapowerbi/Programa_Auditorias.xlsx";

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Programa_Auditorias");

            // Estilos para encabezados
            CellStyle headerStyle = workbook.createCellStyle();
            Font font = workbook.createFont();
            font.setBold(true);
            font.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(font);
            headerStyle.setFillForegroundColor(IndexedColors.DARK_TEAL.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            String[] headers = {
                    "Título",
                    "CodigoAuditoria",
                    "Proceso",
                    "AuditorResponsable",
                    "ResponsableProceso",
                    "FechaProgramada",
                    "FechaEjecutada",
                    "EstadoAuditoria",
                    "ResultadoGeneral",
                    "EvidenciaAuditoria",
                    "Observaciones"
            };

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // 24 auditorías reales distribuidas para reflejar exactamente el volumen del mockup de referencia
            Object[][] data = {
                    {"Auditoría de procesos financieros", "AUD-2025-001", "Financiero", "Juan Carlos Pérez", "María Elena Rojas", "15/01/2025", "18/01/2025", "Completada", "Conforme", "https://sharepoint.corp/evidencias/aud-001.pdf", "Cumplimiento al 100% en balances."},
                    {"Evaluación de gestión documental", "AUD-2025-002", "Gestión Documental", "Ana López", "Roberto Gómez", "22/01/2025", "25/01/2025", "Completada", "Conforme", "https://sharepoint.corp/evidencias/aud-002.pdf", "Digitalización completa de expedientes."},
                    {"Auditoría de talento humano", "AUD-2025-003", "Talento Humano", "Carlos Rodríguez", "Carolina Díaz", "05/02/2025", "", "En Ejecución", "Parcial", "https://sharepoint.corp/evidencias/aud-003.pdf", "Evaluación de clima y contratos."},
                    {"Revisión de procesos operativos", "AUD-2025-004", "Procesos Operativos", "María Fernández", "Fernando Castro", "12/02/2025", "14/02/2025", "En Ejecución", "Conforme", "https://sharepoint.corp/evidencias/aud-004.pdf", "Inspección en líneas de producción."},
                    {"Auditoría de tecnología", "AUD-2025-005", "Tecnología", "Luis Martínez", "Diego Herrera", "20/02/2025", "", "Programada", "Pendiente", "", "Seguridad perimetral y backups."},
                    {"Control de calidad en planta", "AUD-2025-006", "Procesos Operativos", "Juan Carlos Pérez", "Esteban Martínez", "02/03/2025", "04/03/2025", "Completada", "Conforme", "", "Lote 14 certificado."},
                    {"Revisión de compras y proveedores", "AUD-2025-007", "Financiero", "Ana López", "Patricia Soto", "10/03/2025", "12/03/2025", "Completada", "Conforme", "", "Trazabilidad de licitaciones."},
                    {"Auditoría de procesos operativos II", "AUD-2025-008", "Procesos Operativos", "Carlos Rodríguez", "Roberto Gómez", "12/05/2025", "", "Programada", "Pendiente", "", "Programada en agenda anual."},
                    {"Auditoría de seguridad física", "AUD-2025-009", "Gestión Documental", "María Fernández", "Fernando Castro", "18/03/2025", "20/03/2025", "Completada", "Conforme", "", "Accesos biométricos aprobados."},
                    {"Auditoría de tecnología II", "AUD-2025-010", "Tecnología", "Luis Martínez", "Diego Herrera", "20/05/2025", "", "Programada", "Pendiente", "", "Revisión de servidores en nube."},
                    {"Auditoría de cuentas por cobrar", "AUD-2025-011", "Financiero", "Juan Carlos Pérez", "María Elena Rojas", "28/03/2025", "30/03/2025", "Completada", "Conforme", "", "Cartera al día."},
                    {"Auditoría de talento humano II", "AUD-2025-012", "Talento Humano", "Ana López", "Carolina Díaz", "05/06/2025", "", "Programada", "Pendiente", "", "Planes de capacitación."},
                    {"Auditoría de inventarios bodegas", "AUD-2025-013", "Procesos Operativos", "Carlos Rodríguez", "Esteban Martínez", "05/04/2025", "08/04/2025", "Completada", "Conforme", "", "Conteo físico 99.8%."},
                    {"Revisión de políticas de archivo", "AUD-2025-014", "Gestión Documental", "María Fernández", "Roberto Gómez", "12/04/2025", "15/04/2025", "Completada", "Conforme", "", "Cumplimiento ley general de archivos."},
                    {"Auditoría de gestión documental III", "AUD-2025-015", "Gestión Documental", "Juan Carlos Pérez", "Patricia Soto", "18/06/2025", "", "Programada", "Pendiente", "", "Planificada Q2."},
                    {"Auditoría de ciberseguridad y redes", "AUD-2025-016", "Tecnología", "Luis Martínez", "Diego Herrera", "20/04/2025", "22/04/2025", "En Ejecución", "Conforme", "", "Pentesting en progreso."},
                    {"Control tributario e impuestos", "AUD-2025-017", "Financiero", "Juan Carlos Pérez", "María Elena Rojas", "26/04/2025", "28/04/2025", "Completada", "Conforme", "", "Declaraciones anuales presentadas."},
                    {"Auditoría de procesos de despacho", "AUD-2025-018", "Procesos Operativos", "Carlos Rodríguez", "Fernando Castro", "04/05/2025", "06/05/2025", "En Ejecución", "Parcial", "", "Optimización de tiempos de entrega."},
                    {"Inspección de prevención de riesgos", "AUD-2025-019", "Talento Humano", "Ana López", "Carolina Díaz", "09/05/2025", "11/05/2025", "Completada", "Conforme", "", "Brigadas de emergencia activas."},
                    {"Revisión de contratos comerciales", "AUD-2025-020", "Gestión Documental", "María Fernández", "Patricia Soto", "15/05/2025", "17/05/2025", "Completada", "Conforme", "", "Firmas electrónicas validadas."},
                    {"Auditoría de tesorería y bancos", "AUD-2025-021", "Financiero", "Juan Carlos Pérez", "María Elena Rojas", "22/05/2025", "24/05/2025", "Completada", "Conforme", "", "Conciliaciones bancarias aprobadas."},
                    {"Auditoría de logística inversa", "AUD-2025-022", "Procesos Operativos", "Carlos Rodríguez", "Esteban Martínez", "29/05/2025", "", "En Ejecución", "No Conforme", "", "Desviación en gestión de devoluciones."},
                    {"Auditoría de gestión de desempeño", "AUD-2025-023", "Talento Humano", "Ana López", "Carolina Díaz", "03/06/2025", "05/06/2025", "Completada", "Conforme", "", "Evaluación semestral completada."},
                    {"Revisión de continuidad del negocio", "AUD-2025-024", "Tecnología", "Luis Martínez", "Diego Herrera", "10/06/2025", "", "Programada", "Pendiente", "", "Plan de recuperación ante desastres."}
            };

            for (int r = 0; r < data.length; r++) {
                Row row = sheet.createRow(r + 1);
                for (int c = 0; c < data[r].length; c++) {
                    Cell cell = row.createCell(c);
                    cell.setCellValue(data[r][c].toString());
                }
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            try (FileOutputStream fos = new FileOutputStream(new File(outputPath))) {
                workbook.write(fos);
            }
        }
        System.out.println("Archivo Excel generado con éxito en: " + outputPath);
    }
}
