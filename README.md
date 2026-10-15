# NEXA INSIGHTS — Intelligent Data Analytics Platform
Plataforma inteligente de analítica de datos, dashboards dinámicos y consultas fácticas con Inteligencia Artificial desarrollada con **Spring Boot 3**, **MongoDB**, **Groq AI (openai/gpt-oss-120b)** y **Apache ECharts**.

---

## 🌟 Principio Arquitectónico Fundamental
**El usuario proporciona los datos y la plataforma se adapta a ellos.**
No requiere esquemas predefinidos. Puedes subir cualquier archivo tabular (ej. el archivo de auditorías `Programa_Auditorias.xlsx`, registros de ventas, planillas de recursos humanos, estudiantes o incidentes).

---

## 🛠️ Tecnologías y Stack Implementado
- **Backend**: Java 17, Spring Boot 3.2.5, Spring Data MongoDB, Apache POI 5.2.5 (XLSX, XLS), OpenCSV 5.9, Jackson.
- **Base de Datos**: MongoDB (almacenamiento por documento independiente por registro con trazabilidad de fila original `rowNumber`).
- **Motor de IA**: **Groq AI** mediante llamadas REST directas desde el backend Java (modelo `openai/gpt-oss-120b`). Las consultas de la IA se sustentan en registros reales recuperados de MongoDB, citando el número de fila original.
- **Frontend & Visualizaciones**: Thymeleaf, HTML5, CSS3 Editorial Limpio (Paleta Blanca `#FAFAF8`, `#245C4F`, `#D98745`), **Apache ECharts 5.5** interactivo.

---

## 📁 Estructura del Proyecto
```
iapowerbi/
├── pom.xml                                   # Configuración de dependencias Maven
├── .env.example                              # Variables de entorno
├── Programa_Auditorias.xlsx                  # Dataset real de prueba generado
└── src/
    ├── main/
    │   ├── java/com/nexainsights/
    │   │   ├── NexaInsightsApplication.java  # Clase principal Spring Boot
    │   │   ├── config/MongoConfig.java       # Manejo de conversiones y dot replacement
    │   │   ├── model/                        # Project, Dataset, DataRecord, DashboardConfig, ChatSession
    │   │   ├── repository/                   # Repositorios Spring Data MongoDB
    │   │   ├── service/                      # FileImportService, DataProfilingService, DashboardGenerationService, GroqAiAssistantService
    │   │   ├── controller/                   # HomeController, ImportController, DashboardController, DataExplorerController, AssistantController, ReportExportController
    │   │   ├── dto/                          # PreviewResponseDto, ChatRequestDto, ChatResponseDto
    │   │   └── util/                         # Inferencia de tipos y parsing dinámico
    │   └── resources/
    │       ├── application.properties        # Configuración MongoDB y Groq
    │       ├── static/css/main.css           # Estilos editoriales sin azules genéricos
    │       └── templates/                    # Vistas Thymeleaf (dashboard, explorer, assistant, import, reports)
    └── test/
        └── java/com/nexainsights/NexaInsightsApplicationTests.java # Suite de tests end-to-end
```

---

## 🚀 Puesta en Marcha

### 1. Requisitos Previos
- **Java 17+** instalado (`java -version`).
- **Apache Maven 3.9+** instalado (`mvn -version`).
- **MongoDB** en ejecución en `localhost:27017`.

### 2. Variables de Entorno (Opcional)
Puedes definir las siguientes variables en tu sistema o en el archivo `.env`:
```properties
GROQ_API_KEY=gsk_your_groq_api_key_here
GROQ_API_MODEL=openai/gpt-oss-120b
MONGODB_URI=mongodb://localhost:27017/nexa_insights_db
PORT=8080
```

### 3. Ejecución de las Pruebas
Para validar la importación de Excel, la inferencia de columnas, la persistencia en MongoDB y la consulta fáctica con Groq:
```bash
mvn test
```

### 4. Iniciar el Servidor de la Aplicación
```bash
mvn spring-boot:run
```
Una vez iniciado, abre tu navegador en:
👉 **`http://localhost:8080`**

---

## 📊 Flujo de Demostración con el Excel de Auditorías
1. Entra a **`http://localhost:8080/import`**.
2. Arrastra o selecciona el archivo generado **`Programa_Auditorias.xlsx`**.
3. Observa la **Vista Previa** con las columnas detectadas (`Título`, `CodigoAuditoria`, `Proceso`, `AuditorResponsable`, `ResponsableProceso`, `FechaProgramada`, `FechaEjecutada`, `EstadoAuditoria`, `ResultadoGeneral`, `EvidenciaAuditoria`, `Observaciones`).
4. Haz clic en **Confirmar e Importar**.
5. Serás redirigido automáticamente al **Dashboard Inteligente**, donde verás:
   - Tarjetas KPI: Total de registros, índice de calidad, fechas y estados.
   - Gráfico de barras de auditorías por Proceso.
   - Gráfico Doughnut con la distribución de Resultados Generales (`Conforme`, `Observación Menor`, `No Conforme`, `Pendiente`).
   - Ranking por Auditor Responsable.
6. Ve a **Explorador Universal** para filtrar y buscar por cualquier término.
7. Abre el **Asistente Groq AI** y pregunta:
   - *«¿Quién es el auditor responsable de la auditoría con código AUD-2026-001 y qué resultado tuvo?»*
   - Groq consultará MongoDB, recuperará la fila #2 original de Excel y te responderá con evidencia demostrable.
8. En **Informes & Exportar**, descarga el dataset completo o transformado en `.xlsx` o `.csv`.
