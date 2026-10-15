package com.nexainsights.service;

import com.nexainsights.dto.ChatRequestDto;
import com.nexainsights.dto.ChatResponseDto;
import com.nexainsights.model.ChatSession;
import com.nexainsights.model.ColumnDefinition;
import com.nexainsights.model.DataRecord;
import com.nexainsights.model.Dataset;
import com.nexainsights.repository.ChatSessionRepository;
import com.nexainsights.repository.DataRecordRepository;
import com.nexainsights.repository.DatasetRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.regex.Pattern;

@Service
public class GroqAiAssistantService {

    private static final Logger log = LoggerFactory.getLogger(GroqAiAssistantService.class);

    @Value("${groq.api.key}")
    private String groqApiKey;

    @Value("${groq.api.model:llama-3.3-70b-versatile}")
    private String groqModel;

    @Value("${groq.api.url:https://api.groq.com/openai/v1/chat/completions}")
    private String groqApiUrl;

    @Autowired
    private DatasetRepository datasetRepository;

    @Autowired
    private DataRecordRepository dataRecordRepository;

    @Autowired
    private ChatSessionRepository chatSessionRepository;

    @Autowired
    private MongoTemplate mongoTemplate;

    private final RestTemplate restTemplate = new RestTemplate();

    public ChatResponseDto processUserQuery(ChatRequestDto request) {
        String userQuery = request.getMessage();
        Dataset dataset = datasetRepository.findById(request.getDatasetId()).orElse(null);

        if (dataset == null) {
            return new ChatResponseDto("Dataset no encontrado o no especificado.", null);
        }

        // 1. Obtener o crear sesión de chat
        ChatSession session;
        if (request.getSessionId() != null && !request.getSessionId().isEmpty()) {
            session = chatSessionRepository.findById(request.getSessionId())
                    .orElseGet(() -> new ChatSession(request.getProjectId(), dataset.getId(), "Consulta " + dataset.getName()));
        } else {
            session = new ChatSession(request.getProjectId(), dataset.getId(), "Consulta " + dataset.getName());
        }

        // 2. Extraer metadatos y esquema para el contexto del asistente
        StringBuilder schemaSummary = new StringBuilder();
        schemaSummary.append("Dataset: ").append(dataset.getName()).append(" (Archivo: ").append(dataset.getOriginalFileName()).append(")\n");
        schemaSummary.append("Total registros: ").append(dataset.getTotalRows()).append("\n");
        schemaSummary.append("Columnas disponibles:\n");
        for (ColumnDefinition col : dataset.getColumns()) {
            schemaSummary.append("- ").append(col.getOriginalName())
                    .append(" [Clave interna: ").append(col.getKey())
                    .append(", Tipo: ").append(col.getEffectiveType())
                    .append(", Nulos: ").append(col.getNullCount())
                    .append(", Únicos: ").append(col.getDistinctCount());
            if (col.getTopCategories() != null && !col.getTopCategories().isEmpty()) {
                schemaSummary.append(", Valores frecuentes: ").append(col.getTopCategories());
            }
            if (col.getSumNumeric() != null) {
                schemaSummary.append(", Total: ").append(col.getSumNumeric()).append(", Promedio: ").append(col.getAvgNumeric());
            }
            if (col.getMinDate() != null) {
                schemaSummary.append(", Fechas: desde ").append(col.getMinDate()).append(" hasta ").append(col.getMaxDate());
            }
            schemaSummary.append("]\n");
        }

        // 3. Motor de Cálculo Real y Análisis Matemático en Backend (Separar interpretación de cálculo real)
        Map<String, Object> analyticalInsights = executeDeterministicAnalysis(dataset, userQuery);
        String analyticalContextText = (String) analyticalInsights.get("contextSummary");
        com.nexainsights.model.DashboardConfig.ChartWidget generatedChart = (com.nexainsights.model.DashboardConfig.ChartWidget) analyticalInsights.get("generatedChart");

        // 4. Ejecutar búsqueda controlada en MongoDB para extraer evidencia real según la consulta del usuario
        List<DataRecord> matchedRecords = searchRelevantRecords(dataset, userQuery);
        List<Integer> supportingRows = new ArrayList<>();
        List<Map<String, Object>> supportingRecordsData = new ArrayList<>();

        StringBuilder evidenceContext = new StringBuilder();
        if (analyticalContextText != null && !analyticalContextText.isEmpty()) {
            evidenceContext.append(analyticalContextText).append("\n\n");
        }

        if (!matchedRecords.isEmpty()) {
            evidenceContext.append("Se encontraron ").append(matchedRecords.size()).append(" registros coincidentes en la base de datos:\n");
            for (DataRecord rec : matchedRecords) {
                supportingRows.add(rec.getRowNumber());
                Map<String, Object> friendlyRow = new LinkedHashMap<>();
                friendlyRow.put("Fila_Excel", rec.getRowNumber());
                for (ColumnDefinition col : dataset.getColumns()) {
                    friendlyRow.put(col.getOriginalName(), rec.getValue(col.getKey()));
                }
                supportingRecordsData.add(friendlyRow);
                evidenceContext.append("- Fila ").append(rec.getRowNumber()).append(": ").append(friendlyRow).append("\n");
            }
        } else if (analyticalContextText == null || analyticalContextText.isEmpty()) {
            evidenceContext.append("No se encontraron registros individuales específicos con los términos de búsqueda, pero puedes responder analizando el esquema y las estadísticas consolidadas del dataset.");
        }

        // 5. Prompt para Groq
        String systemPrompt = "Eres NEXA Insights AI, el analista de inteligencia de datos empresarial de NEXA INSIGHTS.\n"
                + "Capacidades Operativas Reales del Sistema:\n"
                + "- Tienes acceso a un motor de cálculo real en el backend que computa conteos, sumas, promedios, máximos, mínimos, filtros de fechas y gráficos directamente sobre los registros.\n"
                + "- Si en el contexto se incluye un 'CÁLCULO EXACTO Y VERIFICADO POR EL SERVIDOR', USA ESOS NÚMEROS EXACTOS como la verdad matemática absoluta. Explica las conclusiones con claridad ejecutiva.\n"
                + "- Cuando el usuario pida un gráfico ('crear gráfico', 'grafica...', 'muéstrame un gráfico de...'), el sistema ya genera automáticamente el widget interactivo. Confírmale al usuario que el gráfico ha sido generado y preséntale un análisis breve de las tendencias y valores más altos observados.\n"
                + "- NEXA INSIGHTS cuenta con motores nativos en el servidor para generar y descargar informes PDF oficiales (ejecutivos, de auditoría de cambios y técnicos) y libros Excel (.xlsx).\n"
                + "- Cuando el usuario te pida generar, crear o descargar un PDF o un Excel, CONFÍRMALE cordialmente que el documento oficial ha sido generado y dile que use el botón verde 'Descargar Archivo Oficial' en la tarjeta inferior.\n"
                + "- REGLA CRÍTICA DE ENLACES: NUNCA escribas enlaces falsos ni enlaces Markdown como '[Descargar PDF](...)' o '[Descargar](...)' en el texto de tu respuesta. El sistema adjunta automáticamente el botón de descarga real y seguro en la tarjeta verde inferior.\n"
                + "- NUNCA digas que no puedes generar archivos o gráficos. NEXA INSIGHTS los crea de forma automática en el backend.\n"
                + "\n"
                + "Personalidad y Rol:\n"
                + "- Eres una Inteligencia Artificial analítica y ejecutiva. Responde en español con tono profesional, citas de filas reales y cifras claras.";

        String userPromptContent = "INFORMACIÓN DEL DATASET ACTIVO:\n" + schemaSummary.toString()
                + "\nEVIDENCIA Y CÁLCULOS REALES ENCONTRADOS EN BASE DE DATOS:\n" + evidenceContext.toString()
                + "\nMENSAJE DEL USUARIO:\n" + userQuery;

        // 6. Llamada a la API de Groq
        String aiTextResponse = callGroqApi(systemPrompt, userPromptContent);

        // 7. Detección de intenciones operativas reales (Generación de PDF, Excel, Gráficos, Navegación)
        String actionType = null;
        String actionTitle = null;
        String downloadUrl = null;
        String actionDescription = null;

        String lowerQuery = userQuery.toLowerCase();
        if (generatedChart != null) {
            actionType = "CHART_CREATED";
            actionTitle = "📈 " + generatedChart.getTitle();
            downloadUrl = "/dashboard/" + dataset.getId();
            actionDescription = "Gráfico generado dinámicamente con agregación " + generatedChart.getAggregation() + " sobre " + generatedChart.getDimensionName() + ".";
        } else if (lowerQuery.contains("pdf") || lowerQuery.contains("informe") || lowerQuery.contains("reporte")) {
            String reportType = "executive";
            if (lowerQuery.contains("cambio") || lowerQuery.contains("modificaci") || lowerQuery.contains("auditor") || lowerQuery.contains("trazabilidad")) {
                reportType = "audit";
            } else if (lowerQuery.contains("técnico") || lowerQuery.contains("tecnico") || lowerQuery.contains("esquema")) {
                reportType = "technical";
            }
            actionType = "DOWNLOAD_PDF";
            actionTitle = "📄 Descargar Informe PDF Oficial";
            downloadUrl = "/reports/export/pdf/" + dataset.getId() + "?type=" + reportType;
            actionDescription = "Informe generado con éxito con datos activos, KPIs y trazabilidad completa.";
        } else if (lowerQuery.contains("excel") || lowerQuery.contains("xlsx") || lowerQuery.contains("descargar datos")) {
            actionType = "DOWNLOAD_XLSX";
            actionTitle = "📊 Descargar Libro Excel (.XLSX)";
            downloadUrl = "/reports/export/xlsx/" + dataset.getId();
            actionDescription = "Libro de cálculo actualizado con todas las ediciones y filas conservadas.";
        } else if (lowerQuery.contains("editar") || lowerQuery.contains("tabla") || lowerQuery.contains("modificar") || lowerQuery.contains("corregir")) {
            actionType = "OPEN_EDITOR";
            actionTitle = "✏️ Abrir Editor de Datos";
            downloadUrl = "/explorer?datasetId=" + dataset.getId();
            actionDescription = "Acceso al editor tabular con guardado en vivo y registro de versiones.";
        } else if (lowerQuery.contains("gráfico") || lowerQuery.contains("grafico") || lowerQuery.contains("dashboard") || lowerQuery.contains("visualizar")) {
            actionType = "CHART_VIEW";
            actionTitle = "📈 Ver Gráficos y Dashboard Interactivo";
            downloadUrl = "/dashboard/" + dataset.getId();
            actionDescription = "Visualizaciones dinámicas interactivas adaptadas al esquema de este archivo.";
        } else if (lowerQuery.contains("comparar") || lowerQuery.contains("relacion") || lowerQuery.contains("diferencia")) {
            actionType = "RELATIONSHIPS";
            actionTitle = "🔗 Descubrimiento de Relaciones y Comparación";
            downloadUrl = "/relationships";
            actionDescription = "Explora coincidencias de claves y correlaciones entre conjuntos de datos.";
        }

        // 8. Guardar en el historial de sesión (incluyendo datos de acción)
        ChatSession.ChatMessage userMsg = new ChatSession.ChatMessage("user", userQuery);
        ChatSession.ChatMessage botMsg = new ChatSession.ChatMessage("assistant", aiTextResponse);
        botMsg.setSupportingRowNumbers(supportingRows);
        botMsg.setSupportingRecords(supportingRecordsData);
        botMsg.setExecutedQuery("Consulta sobre dataset: " + dataset.getName());
        botMsg.setActionType(actionType);
        botMsg.setActionTitle(actionTitle);
        botMsg.setDownloadUrl(downloadUrl);
        botMsg.setActionDescription(actionDescription);

        session.getMessages().add(userMsg);
        session.getMessages().add(botMsg);
        session.setUpdatedAt(java.time.LocalDateTime.now());
        session = chatSessionRepository.save(session);

        ChatResponseDto responseDto = new ChatResponseDto(aiTextResponse, session.getId());
        responseDto.setSupportingRecords(supportingRecordsData);
        responseDto.setSupportingRows(supportingRows);
        responseDto.setExecutedQuery(botMsg.getExecutedQuery());
        responseDto.setActionType(actionType);
        responseDto.setActionTitle(actionTitle);
        responseDto.setDownloadUrl(downloadUrl);
        responseDto.setActionDescription(actionDescription);
        responseDto.setChartWidget(generatedChart);
        responseDto.setActionPayload(analyticalInsights);

        return responseDto;
    }

    @Autowired
    private DashboardGenerationService dashboardGenerationService;

    /**
     * Motor de Cálculo Analítico Determinista: realiza cálculos matemáticos reales,
     * agrega, filtra por fechas y genera gráficos con datos 100% reales.
     */
    private Map<String, Object> executeDeterministicAnalysis(Dataset dataset, String userQuery) {
        Map<String, Object> result = new HashMap<>();
        StringBuilder summary = new StringBuilder();
        String lq = userQuery.toLowerCase();

        List<DataRecord> records = dataRecordRepository.findByDatasetId(dataset.getId());
        if (records == null || records.isEmpty()) {
            return result;
        }

        // 1. Detección de creación de Gráficos desde el chat
        boolean isChartRequest = lq.contains("gráfico") || lq.contains("grafico") || lq.contains("grafica")
                || lq.contains("diagrama") || lq.contains("visualiza") || lq.contains("plot");

        if (isChartRequest) {
            // Encontrar la mejor columna de dimensión
            ColumnDefinition targetDim = null;
            for (ColumnDefinition c : dataset.getColumns()) {
                if (lq.contains(c.getOriginalName().toLowerCase()) || lq.contains(c.getKey().toLowerCase())) {
                    targetDim = c;
                    break;
                }
            }
            if (targetDim == null) {
                // Fallback a primera columna categórica o de texto
                for (ColumnDefinition c : dataset.getColumns()) {
                    if (c.isCategorical() || c.getEffectiveType() == com.nexainsights.model.DataType.TEXT) {
                        targetDim = c;
                        break;
                    }
                }
            }
            if (targetDim == null && !dataset.getColumns().isEmpty()) {
                targetDim = dataset.getColumns().get(0);
            }

            if (targetDim != null) {
                // Determinar tipo de gráfico sugerido
                String chartType = "bar";
                if (lq.contains("torta") || lq.contains("circular") || lq.contains("pastel") || lq.contains("proporci")) {
                    chartType = "pie";
                } else if (lq.contains("rosca") || lq.contains("donut") || lq.contains("doughnut")) {
                    chartType = "doughnut";
                } else if (lq.contains("línea") || lq.contains("linea") || lq.contains("tendencia") || lq.contains("evolución") || lq.contains("evolucion") || lq.contains("mes")) {
                    chartType = "line";
                } else if (lq.contains("ranking") || lq.contains("top") || lq.contains("horizontal")) {
                    chartType = "ranking";
                }

                // Determinar agregación
                String aggregation = "COUNT";
                String metricKey = null;
                if (lq.contains("suma") || lq.contains("total")) aggregation = "SUM";
                else if (lq.contains("promedio") || lq.contains("media")) aggregation = "AVG";
                else if (lq.contains("máximo") || lq.contains("maximo") || lq.contains("mayor")) aggregation = "MAX";
                else if (lq.contains("mínimo") || lq.contains("minimo") || lq.contains("menor")) aggregation = "MIN";

                // Si no es COUNT, buscar columna numérica
                if (!"COUNT".equals(aggregation)) {
                    for (ColumnDefinition c : dataset.getColumns()) {
                        if (c.isNumeric() && (lq.contains(c.getOriginalName().toLowerCase()) || lq.contains(c.getKey().toLowerCase()))) {
                            metricKey = c.getKey();
                            break;
                        }
                    }
                    if (metricKey == null) {
                        for (ColumnDefinition c : dataset.getColumns()) {
                            if (c.isNumeric()) {
                                metricKey = c.getKey();
                                break;
                            }
                        }
                    }
                }

                com.nexainsights.dto.CustomChartRequestDto chartDto = new com.nexainsights.dto.CustomChartRequestDto();
                chartDto.setDatasetId(dataset.getId());
                chartDto.setTitle("Distribución de " + targetDim.getOriginalName() + (metricKey != null ? " (" + aggregation + ")" : ""));
                chartDto.setDimensionKey(targetDim.getKey());
                chartDto.setType(chartType);
                chartDto.setAggregation(aggregation);
                chartDto.setMetricKey(metricKey);

                try {
                    com.nexainsights.model.DashboardConfig.ChartWidget widget = dashboardGenerationService.addCustomChart(chartDto, dataset);
                    result.put("generatedChart", widget);
                    summary.append("CÁLCULO EXACTO Y VERIFICADO POR EL SERVIDOR (GRÁFICO GENERADO):\n")
                           .append("- Gráfico creado: '").append(widget.getTitle()).append("'\n")
                           .append("- Dimensión: ").append(targetDim.getOriginalName()).append("\n")
                           .append("- Tipo: ").append(chartType).append("\n")
                           .append("- Categorías calculadas: ").append(widget.getCategories()).append("\n")
                           .append("- Valores reales computados: ").append(widget.getSeriesData()).append("\n");
                } catch (Exception e) {
                    log.warn("No se pudo autogenerar el gráfico: {}", e.getMessage());
                }
            }
        }

        // 2. Cálculos Numéricos Explícitos (Suma, Promedio, Máximo, Mínimo)
        for (ColumnDefinition col : dataset.getColumns()) {
            if (col.isNumeric() && (lq.contains(col.getOriginalName().toLowerCase()) || lq.contains(col.getKey().toLowerCase()))) {
                List<Double> numVals = new ArrayList<>();
                for (DataRecord r : records) {
                    Object val = r.getValue(col.getKey());
                    if (val != null) {
                        try {
                            String clean = val.toString().replaceAll("[^0-9.-]", "");
                            if (!clean.isEmpty()) numVals.add(Double.parseDouble(clean));
                        } catch (Exception ignored) {}
                    }
                }
                if (!numVals.isEmpty()) {
                    double sum = numVals.stream().mapToDouble(Double::doubleValue).sum();
                    double avg = numVals.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
                    double max = numVals.stream().mapToDouble(Double::doubleValue).max().orElse(0.0);
                    double min = numVals.stream().mapToDouble(Double::doubleValue).min().orElse(0.0);

                    summary.append("CÁLCULO EXACTO Y VERIFICADO POR EL SERVIDOR (COLUMNA NUMÉRICA '").append(col.getOriginalName()).append("'):\n")
                           .append("- Total registros con valor numérico: ").append(numVals.size()).append("\n")
                           .append("- Suma total calculada: ").append(Math.round(sum * 100.0) / 100.0).append("\n")
                           .append("- Promedio calculado: ").append(Math.round(avg * 100.0) / 100.0).append("\n")
                           .append("- Valor máximo: ").append(max).append("\n")
                           .append("- Valor mínimo: ").append(min).append("\n");
                }
            }
        }

        // 3. Conteo de Estados o Categorías más repetidas
        if (lq.contains("más se repite") || lq.contains("mas se repite") || lq.contains("frecuente") || lq.contains("ranking") || lq.contains("mayor cantidad")) {
            for (ColumnDefinition col : dataset.getColumns()) {
                if (col.isCategorical() && col.getCategoryFrequencies() != null && !col.getCategoryFrequencies().isEmpty()) {
                    summary.append("FRECUENCIAS REALES VERIFICADAS ('").append(col.getOriginalName()).append("'):\n");
                    col.getCategoryFrequencies().entrySet().stream()
                            .sorted((e1, e2) -> Long.compare(e2.getValue(), e1.getValue()))
                            .limit(5)
                            .forEach(e -> summary.append("- ").append(e.getKey()).append(": ").append(e.getValue()).append(" registros\n"));
                }
            }
        }

        // 4. Detección de duplicados o registros vacíos
        if (lq.contains("duplicad") || lq.contains("incomplet") || lq.contains("vacío") || lq.contains("vacio")) {
            long emptyRows = records.stream().filter(r -> {
                for (ColumnDefinition col : dataset.getColumns()) {
                    Object v = r.getValue(col.getKey());
                    if (v == null || v.toString().trim().isEmpty()) return true;
                }
                return false;
            }).count();
            summary.append("AUDITORÍA DE INTEGRIDAD REAL:\n")
                   .append("- Total de filas con al menos un campo vacío: ").append(emptyRows).append(" de ").append(records.size()).append(" registros.\n");
        }

        result.put("contextSummary", summary.toString());
        return result;
    }

    /**
     * Búsqueda segura en MongoDB para recuperar registros que sustentan la pregunta
     */
    private List<DataRecord> searchRelevantRecords(Dataset dataset, String queryText) {
        if (queryText == null || queryText.trim().isEmpty()) {
            return Collections.emptyList();
        }

        // Extraer palabras clave relevantes (filtrar palabras comunes)
        String[] words = queryText.replaceAll("[^a-zA-Z0-9áéíóúÁÉÍÓÚñÑ\\s]", "").split("\\s+");
        List<String> keywords = new ArrayList<>();
        Set<String> stopWords = new HashSet<>(Arrays.asList("el", "la", "los", "las", "un", "una", "de", "del", "en", "que", "para", "por", "con", "a", "mostrar", "muestrame", "busca", "cuales", "quien", "cuando"));
        for (String w : words) {
            if (w.length() > 2 && !stopWords.contains(w.toLowerCase())) {
                keywords.add(w);
            }
        }

        if (keywords.isEmpty()) {
            return Collections.emptyList();
        }

        boolean wantsMissingOrEmpty = queryText.toLowerCase().contains("vacio") || queryText.toLowerCase().contains("incomplet") || queryText.toLowerCase().contains("faltan");

        Criteria mainCriteria = Criteria.where("datasetId").is(dataset.getId());
        List<Criteria> orConditions = new ArrayList<>();

        if (wantsMissingOrEmpty) {
            for (ColumnDefinition col : dataset.getColumns()) {
                orConditions.add(Criteria.where("data." + col.getKey()).is(""));
                orConditions.add(Criteria.where("data." + col.getKey()).is(null));
            }
        }

        for (String kw : keywords) {
            Pattern regex = Pattern.compile(Pattern.quote(kw), Pattern.CASE_INSENSITIVE);
            List<Criteria> columnMatches = new ArrayList<>();
            for (ColumnDefinition col : dataset.getColumns()) {
                columnMatches.add(Criteria.where("data." + col.getKey()).regex(regex));
            }
            if (!columnMatches.isEmpty()) {
                orConditions.add(new Criteria().orOperator(columnMatches.toArray(new Criteria[0])));
            }
        }

        if (!orConditions.isEmpty()) {
            mainCriteria = new Criteria().andOperator(mainCriteria, new Criteria().orOperator(orConditions.toArray(new Criteria[0])));
        }

        Query mongoQuery = new Query(mainCriteria).with(PageRequest.of(0, 25)); // Capacidad ampliada a 25 registros
        return mongoTemplate.find(mongoQuery, DataRecord.class);
    }

    private String callGroqApi(String systemPrompt, String userContent) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(groqApiKey);

            Map<String, Object> body = new HashMap<>();
            body.put("model", groqModel);
            body.put("temperature", 0.2); // Respuestas fácticas y precisas

            List<Map<String, String>> messages = new ArrayList<>();
            Map<String, String> sysMsg = new HashMap<>();
            sysMsg.put("role", "system");
            sysMsg.put("content", systemPrompt);
            messages.add(sysMsg);

            Map<String, String> usrMsg = new HashMap<>();
            usrMsg.put("role", "user");
            usrMsg.put("content", userContent);
            messages.add(usrMsg);

            body.put("messages", messages);

            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(body, headers);
            ResponseEntity<Map> response = restTemplate.exchange(groqApiUrl, HttpMethod.POST, requestEntity, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                List<Map<String, Object>> choices = (List<Map<String, Object>>) response.getBody().get("choices");
                if (choices != null && !choices.isEmpty()) {
                    Map<String, Object> firstChoice = choices.get(0);
                    Map<String, Object> message = (Map<String, Object>) firstChoice.get("message");
                    if (message != null) {
                        return (String) message.get("content");
                    }
                }
            }
            return "No se pudo obtener respuesta del modelo de Groq.";
        } catch (Exception e) {
            log.error("Error al consultar Groq AI: {}", e.getMessage());
            return "Error al comunicar con Groq AI: " + e.getMessage() + ". Verifica tu conexión y clave API.";
        }
    }
}
