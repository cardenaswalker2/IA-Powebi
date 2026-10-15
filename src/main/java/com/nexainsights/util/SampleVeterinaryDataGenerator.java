package com.nexainsights.util;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileOutputStream;
import java.io.IOException;

public class SampleVeterinaryDataGenerator {

    public static void main(String[] args) {
        String filename = "Agendamiento_Clinicas_Veterinarias.xlsx";

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Agendamiento_Citas");

            // Estilo para el encabezado
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerFont.setFontHeightInPoints((short) 11);

            CellStyle headerCellStyle = workbook.createCellStyle();
            headerCellStyle.setFont(headerFont);
            headerCellStyle.setFillForegroundColor(IndexedColors.DARK_TEAL.getIndex());
            headerCellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerCellStyle.setAlignment(HorizontalAlignment.CENTER);
            headerCellStyle.setVerticalAlignment(VerticalAlignment.CENTER);

            // Estilos para celdas normales
            CellStyle dataCellStyle = workbook.createCellStyle();
            dataCellStyle.setVerticalAlignment(VerticalAlignment.CENTER);

            String[] columns = {
                    "CodigoCita",
                    "NombreMascota",
                    "Especie",
                    "Raza",
                    "EdadAnios",
                    "NombrePropietario",
                    "TelefonoContacto",
                    "ServicioSolicitado",
                    "VeterinarioAsignado",
                    "SedeClinica",
                    "FechaCita",
                    "HoraCita",
                    "EstadoCita",
                    "CostoEstimado",
                    "ObservacionesClinicas"
            };

            Row headerRow = sheet.createRow(0);
            headerRow.setHeightInPoints(26);
            for (int i = 0; i < columns.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(columns[i]);
                cell.setCellStyle(headerCellStyle);
            }

            // 28 Citas Veterinarias realistas y completas
            Object[][] data = {
                    {"CIT-2025-001", "Max", "Canino", "Golden Retriever", 4, "Carlos Mendoza", "3104567890", "Consulta General", "Dra. Valentina Ruiz", "Sede Norte", "15/01/2025", "08:30", "Atendida", 85000, "Chequeo preventivo anual. Vacunas al día."},
                    {"CIT-2025-002", "Luna", "Felino", "Siamés", 2, "Mariana Herrera", "3127891234", "Vacunación Triple Felina", "Dr. Alejandro Silva", "Sede Central", "16/01/2025", "09:15", "Atendida", 65000, "Aplicación de refuerzo sin reacciones adversas."},
                    {"CIT-2025-003", "Rocky", "Canino", "Bulldog Francés", 3, "Andrés Gómez", "3158902345", "Dermatología", "Dra. Valentina Ruiz", "Sede Norte", "18/01/2025", "10:00", "Atendida", 120000, "Cuadro de dermatitis alérgica alimentaria, se formula dieta hipoalergénica."},
                    {"CIT-2025-004", "Milo", "Canino", "Poodle", 6, "Patricia Morales", "3179013456", "Profilaxis Dental", "Dr. Sebastián Toro", "Sede Sur", "20/01/2025", "11:00", "Atendida", 160000, "Limpieza dental por ultrasonido exitosa."},
                    {"CIT-2025-005", "Simba", "Felino", "Persa", 5, "Fernando Castro", "3001234567", "Control Cardiológico", "Dra. Laura Cadavid", "Sede Central", "22/01/2025", "14:30", "Atendida", 145000, "Ecocardiograma normal, control en 6 meses."},
                    {"CIT-2025-006", "Bella", "Canino", "Pastor Alemán", 7, "Lucía Restrepo", "3142345678", "Ortopedia y Rayos X", "Dr. Sebastián Toro", "Sede Norte", "25/01/2025", "15:00", "Atendida", 190000, "Displasia leve cadera izquierda, fisioterapia recomendada."},
                    {"CIT-2025-007", "Toby", "Canino", "Beagle", 1, "Diego Valencia", "3183456789", "Desparasitación y Microchip", "Dr. Alejandro Silva", "Sede Sur", "28/01/2025", "16:15", "Atendida", 95000, "Implantación de microchip internacional exitosa."},
                    {"CIT-2025-008", "Kiwi", "Ave", "Ninfa", 2, "Camila Pineda", "3114567890", "Medicina de Aves Exóticas", "Dra. Sofía Rincón", "Sede Central", "02/02/2025", "09:00", "Atendida", 80000, "Revisión de plumaje y recorte de pico."},
                    {"CIT-2025-009", "Coco", "Canino", "Labrador", 5, "Gabriel Jaramillo", "3165678901", "Cirugía Esterilización", "Dr. Sebastián Toro", "Sede Norte", "05/02/2025", "08:00", "Atendida", 280000, "Orquiectomía sin complicaciones, postoperatorio favorable."},
                    {"CIT-2025-010", "Mia", "Felino", "Común Europeo", 3, "Daniela Ospina", "3196789012", "Ecografía Abdominal", "Dra. Laura Cadavid", "Sede Central", "07/02/2025", "10:30", "Atendida", 130000, "Sospecha de cistitis idiopática, examen normal."},
                    {"CIT-2025-011", "Zeus", "Canino", "Rottweiler", 4, "Jorge Benítez", "3207890123", "Fisioterapia y Rehabilitación", "Dra. Valentina Ruiz", "Sede Sur", "10/02/2025", "11:45", "Atendida", 110000, "Sesión de láser terapéutico en miembro posterior."},
                    {"CIT-2025-012", "Nala", "Felino", "Angora", 1, "Sara Quintero", "3218901234", "Vacunación Rabia", "Dr. Alejandro Silva", "Sede Norte", "12/02/2025", "14:00", "Atendida", 50000, "Dosis única administrada correctamente."},
                    {"CIT-2025-013", "Thor", "Canino", "Siberian Husky", 3, "Mateo Arango", "3229012345", "Oftalmología", "Dra. Sofía Rincón", "Sede Central", "15/02/2025", "15:30", "Confirmada", 115000, "Control de úlcera corneal superficial."},
                    {"CIT-2025-014", "Bruno", "Canino", "Bóxer", 6, "Esteban Cardona", "3130123456", "Oncología y Biopsia", "Dra. Laura Cadavid", "Sede Norte", "18/02/2025", "16:00", "Confirmada", 210000, "Toma de muestra de nódulo subcutáneo."},
                    {"CIT-2025-015", "Kira", "Canino", "Dálmata", 2, "Helena Cárdenas", "3141234567", "Nutrición y Control de Peso", "Dra. Valentina Ruiz", "Sede Sur", "21/02/2025", "08:30", "Confirmada", 75000, "Plan alimentario para mantenimiento corporal."},
                    {"CIT-2025-016", "Pancho", "Canino", "Chihuahua", 8, "Raúl Echeverri", "3152345678", "Geriatría Canina", "Dr. Alejandro Silva", "Sede Central", "24/02/2025", "09:30", "Confirmada", 125000, "Perfil geriátrico completo con química sanguínea."},
                    {"CIT-2025-017", "Pelusa", "Felino", "Maine Coon", 4, "Isabella Londoño", "3163456789", "Urgencia Respiratoria", "Dr. Sebastián Toro", "Sede Norte", "26/02/2025", "10:15", "Confirmada", 150000, "Bronquitis felina bajo nebulización."},
                    {"CIT-2025-018", "Lucas", "Canino", "Schnauzer", 3, "Guillermo Ríos", "3174567890", "Endoscopia Digestiva", "Dra. Laura Cadavid", "Sede Central", "01/03/2025", "11:00", "Confirmada", 240000, "Extracción de cuerpo extraño en estómago."},
                    {"CIT-2025-019", "Oreo", "Felino", "Ragdoll", 2, "Carolina Velez", "3185678901", "Grooming Clínico y Baño", "Dra. Sofía Rincón", "Sede Sur", "03/03/2025", "14:00", "Confirmada", 70000, "Baño medicado antipruriginoso."},
                    {"CIT-2025-020", "Apollo", "Canino", "Doberman", 5, "Manuel Beltrán", "3196789012", "Cardiología y Holter", "Dra. Laura Cadavid", "Sede Norte", "06/03/2025", "15:00", "Confirmada", 220000, "Instalación de monitor Holter de 24 horas."},
                    {"CIT-2025-021", "Fiona", "Canino", "Shih Tzu", 4, "Clara Guzmán", "3207890123", "Revisión Dermatológica", "Dra. Valentina Ruiz", "Sede Sur", "10/03/2025", "16:30", "Pendiente", 85000, "Cita agendada para control de otitis externa."},
                    {"CIT-2025-022", "Tom", "Felino", "Bengala", 1, "Óscar Salazar", "3218901234", "Primera Consulta Vacunas", "Dr. Alejandro Silva", "Sede Central", "13/03/2025", "08:45", "Pendiente", 60000, "Apertura de historial clínico y carnet."},
                    {"CIT-2025-023", "Sasha", "Canino", "Husky Siberiano", 2, "Viviana Henao", "3229012345", "Control Postquirúrgico", "Dr. Sebastián Toro", "Sede Norte", "15/03/2025", "09:30", "Pendiente", 45000, "Retiro de puntos quirúrgicos."},
                    {"CIT-2025-024", "Chester", "Roedor", "Conejo Belier", 2, "Tatiana Mejía", "3101234567", "Odontología Especies Menores", "Dra. Sofía Rincón", "Sede Sur", "18/03/2025", "10:30", "Pendiente", 90000, "Limado de molares por maloclusión."},
                    {"CIT-2025-025", "Chispa", "Canino", "Jack Russell", 3, "Felipe Marín", "3112345678", "Neurología Clínica", "Dra. Laura Cadavid", "Sede Central", "20/03/2025", "11:15", "Pendiente", 140000, "Evaluación de marcha claudicante."},
                    {"CIT-2025-026", "Loki", "Canino", "Border Collie", 2, "Diana Castaño", "3123456789", "Chequeo Preventivo", "Dra. Valentina Ruiz", "Sede Norte", "24/03/2025", "14:15", "Pendiente", 85000, "Revisión general de salud y asesoría deportiva."},
                    {"CIT-2025-027", "Mía", "Felino", "Siamés", 4, "Samuel Moncada", "3134567890", "Control Renal", "Dr. Alejandro Silva", "Sede Sur", "27/03/2025", "15:00", "Cancelada", 75000, "Cancelada por el propietario con reprogramación pendiente."},
                    {"CIT-2025-028", "Dante", "Canino", "Gran Danés", 5, "Rodrigo Tamayo", "3145678901", "Gastroenterología", "Dr. Sebastián Toro", "Sede Central", "30/03/2025", "16:30", "Cancelada", 130000, "Reprogramada para el siguiente mes por viaje."}
            };

            for (int r = 0; r < data.length; r++) {
                Row row = sheet.createRow(r + 1);
                row.setHeightInPoints(20);
                for (int c = 0; c < data[r].length; c++) {
                    Cell cell = row.createCell(c);
                    Object val = data[r][c];

                    if (val instanceof String) {
                        cell.setCellValue((String) val);
                    } else if (val instanceof Integer) {
                        cell.setCellValue((Integer) val);
                    } else if (val instanceof Double) {
                        cell.setCellValue((Double) val);
                    }
                    cell.setCellStyle(dataCellStyle);
                }
            }

            // Autoajustar ancho de columnas
            for (int i = 0; i < columns.length; i++) {
                sheet.autoSizeColumn(i);
                sheet.setColumnWidth(i, Math.max(sheet.getColumnWidth(i) + 1200, 3600));
            }

            try (FileOutputStream fileOut = new FileOutputStream(filename)) {
                workbook.write(fileOut);
            }

            System.out.println("Archivo Excel generado con éxito: " + filename + " con " + data.length + " citas veterinarias.");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
