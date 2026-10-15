package com.nexainsights;

import com.nexainsights.dto.PreviewResponseDto;
import com.nexainsights.model.Dataset;
import com.nexainsights.model.Project;
import com.nexainsights.repository.DataRecordRepository;
import com.nexainsights.repository.DatasetRepository;
import com.nexainsights.repository.ProjectRepository;
import com.nexainsights.service.FileImportService;
import com.nexainsights.service.GroqAiAssistantService;
import com.nexainsights.dto.ChatRequestDto;
import com.nexainsights.dto.ChatResponseDto;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;

import java.io.File;
import java.io.FileInputStream;

@SpringBootTest
class NexaInsightsApplicationTests {

    @Autowired
    private FileImportService fileImportService;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private DatasetRepository datasetRepository;

    @Autowired
    private DataRecordRepository dataRecordRepository;

    @Autowired
    private GroqAiAssistantService groqAiAssistantService;

    @Autowired
    private com.nexainsights.service.DashboardGenerationService dashboardGenerationService;

    @Test
    void testEndToEndAuditExcelImportAndGroqQuery() throws Exception {
        // 1. Crear Proyecto
        Project project = new Project("Auditorías Corporativas 2026", "Demostración de auditorías internas");
        project = projectRepository.save(project);
        Assertions.assertNotNull(project.getId());

        // 2. Cargar archivo Excel real generado con la estructura de SharePoint
        File excelFile = new File("Programa_Auditorias.xlsx");
        Assertions.assertTrue(excelFile.exists(), "El archivo Programa_Auditorias.xlsx debe existir");

        try (FileInputStream fis = new FileInputStream(excelFile)) {
            MockMultipartFile multipartFile = new MockMultipartFile(
                    "file", "Programa_Auditorias.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", fis);

            // 3. Probar Vista Previa
            PreviewResponseDto preview = fileImportService.generatePreview(multipartFile, "Programa_Auditorias", 0);
            Assertions.assertNotNull(preview);
            Assertions.assertEquals("Programa_Auditorias", preview.getSelectedSheet());
            Assertions.assertTrue(preview.getOriginalHeaders().contains("AuditorResponsable"));
            Assertions.assertTrue(preview.getOriginalHeaders().contains("CodigoAuditoria"));

            // 4. Probar Importación definitiva y persistencia en MongoDB
            Dataset dataset = fileImportService.importFile(multipartFile, project.getId(), "Programa_Auditorias", "Programa_Auditorias", 0);
            Assertions.assertNotNull(dataset.getId());
            Assertions.assertEquals(24, dataset.getTotalRows());

            long recordsCount = dataRecordRepository.countByDatasetId(dataset.getId());
            Assertions.assertEquals(24, recordsCount);

            // 5. Probar Consulta de Inteligencia Artificial Groq con datos reales
            ChatRequestDto chatReq = new ChatRequestDto();
            chatReq.setProjectId(project.getId());
            chatReq.setDatasetId(dataset.getId());
            chatReq.setMessage("¿Quién es el auditor responsable de la auditoría con código AUD-2026-001 y qué resultado tuvo?");

            ChatResponseDto chatRes = groqAiAssistantService.processUserQuery(chatReq);
            Assertions.assertNotNull(chatRes);
            Assertions.assertNotNull(chatRes.getResponse());
            System.out.println("Respuesta verificable de Groq AI: \n" + chatRes.getResponse());
            System.out.println("Filas de evidencia: " + chatRes.getSupportingRows());
        }
    }

    @Test
    void testCsvImportWithDuplicateHeadersAndCustomChart() throws Exception {
        Project project = projectRepository.save(new Project("Proyecto Prueba CSV", "Test"));

        // CSV sintético con columnas duplicadas "Métrica" y "Métrica"
        String csvContent = "ID,Métrica,Métrica,Estado\n" +
                "101,500,20,Completado\n" +
                "102,750,35,Pendiente\n" +
                "103,1200,50,Completado\n";

        MockMultipartFile csvFile = new MockMultipartFile(
                "file", "metricas_duplicadas.csv", "text/csv", csvContent.getBytes(java.nio.charset.StandardCharsets.UTF_8));

        Dataset dataset = fileImportService.importFile(csvFile, project.getId(), "Metricas Duplicadas", null, 0);
        Assertions.assertNotNull(dataset);
        Assertions.assertEquals(3, dataset.getTotalRows());
        Assertions.assertEquals(4, dataset.getTotalColumns());

        // Verificar que la columna duplicada fue desduplicada a "Métrica (2)" sin colisiones
        boolean hasDuplicateResolved = dataset.getColumns().stream()
                .anyMatch(c -> c.getOriginalName().equalsIgnoreCase("Métrica (2)"));
        Assertions.assertTrue(hasDuplicateResolved, "La columna duplicada debe resolverse como 'Métrica (2)'");

        // Crear widget personalizado
        com.nexainsights.dto.CustomChartRequestDto chartDto = new com.nexainsights.dto.CustomChartRequestDto();
        chartDto.setDatasetId(dataset.getId());
        chartDto.setTitle("Distribución de Estados");
        chartDto.setDimensionKey(dataset.getColumns().get(3).getKey());
        chartDto.setType("doughnut");
        chartDto.setAggregation("COUNT");

        com.nexainsights.model.DashboardConfig.ChartWidget widget = dashboardGenerationService.addCustomChart(chartDto, dataset);
        Assertions.assertNotNull(widget);
        Assertions.assertEquals("doughnut", widget.getType());
        Assertions.assertEquals("Distribución de Estados", widget.getTitle());
    }
}
