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

        // 3. Ejecutar búsqueda controlada en MongoDB para extraer evidencia real según la consulta del usuario
        List<DataRecord> matchedRecords = searchRelevantRecords(dataset, userQuery);
        List<Integer> supportingRows = new ArrayList<>();
        List<Map<String, Object>> supportingRecordsData = new ArrayList<>();

        StringBuilder evidenceContext = new StringBuilder();
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
        } else {
            evidenceContext.append("No se encontraron registros individuales específicos con los términos de búsqueda, pero puedes responder analizando el esquema y las estadísticas consolidadas del dataset.");
        }

        // 4. Prompt para Groq
        String systemPrompt = "Eres NEXA Insights AI, el analista de inteligencia de datos empresarial de NEXA INSIGHTS.\n"
                + "Capacidades Operativas Reales del Sistema:\n"
                + "- NEXA INSIGHTS cuenta con motores nativos en el servidor para generar y descargar informes PDF oficiales (ejecutivos, de auditoría de cambios y técnicos) y libros Excel (.xlsx).\n"
                + "- Cuando el usuario te pida generar, crear o descargar un PDF o un Excel, o te pida un informe sobre los cambios, las horas o los costos, CONFÍRMALE cordialmente que el documento oficial ha sido generado y procesado con éxito con los datos actuales y que puede descargarlo directamente desde el botón adjunto a continuación.\n"
                + "- NUNCA digas que no puedes generar archivos, que solo manejas texto o que el usuario debe copiar y pegar a Word o Google Docs para hacer un PDF. NEXA INSIGHTS genera el PDF y el Excel reales de forma automática en el backend.\n"
                + "\n"
                + "Personalidad y Rol:\n"
                + "- Eres una Inteligencia Artificial avanzada creada para analizar datos, responder consultas analíticas, generar resúmenes ejecutivos y ayudar a los usuarios a tomar decisiones con su información.\n"
                + "- Si el usuario te saluda, te pregunta quién eres, si eres una IA, o te hace preguntas generales o sobre tus capacidades, responde de forma natural, cordial, elocuente y profesional como la IA de NEXA INSIGHTS.\n"
                + "\n"
                + "Reglas para Consultas sobre los Datos del Dataset:\n"
                + "1. Cuando te pregunten sobre los datos (personas, horas, costos, auditorías, ventas, estados, fechas, números, etc.), básate en los datos reales provistos.\n"
                + "2. Cita los números de fila originales de Excel donde encontraste la información.\n"
                + "3. Si piden un resumen ejecutivo o análisis de cambios, presenta los hallazgos principales de forma concisa y profesional en español.";

        String userPromptContent = "INFORMACIÓN DEL DATASET ACTIVO:\n" + schemaSummary.toString()
                + "\nEVIDENCIA Y REGISTROS ENCONTRADOS EN BASE DE DATOS:\n" + evidenceContext.toString()
                + "\nMENSAJE DEL USUARIO:\n" + userQuery;

        // 5. Llamada a la API de Groq
        String aiTextResponse = callGroqApi(systemPrompt, userPromptContent);

        // 6. Detección de intenciones operativas reales (Generación de PDF, Excel, Navegación)
        String actionType = null;
        String actionTitle = null;
        String downloadUrl = null;
        String actionDescription = null;

        String lowerQuery = userQuery.toLowerCase();
        if (lowerQuery.contains("pdf") || lowerQuery.contains("informe") || lowerQuery.contains("reporte")) {
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

        // 7. Guardar en el historial de sesión (incluyendo datos de acción)
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

        return responseDto;
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
