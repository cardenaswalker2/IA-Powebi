package com.nexainsights.controller;

import com.nexainsights.model.DashboardConfig;
import com.nexainsights.model.Dataset;
import com.nexainsights.model.Project;
import com.nexainsights.repository.DashboardConfigRepository;
import com.nexainsights.repository.DatasetRepository;
import com.nexainsights.repository.ProjectRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@RequestMapping("/dashboard")
public class DashboardController {

    @Autowired
    private DatasetRepository datasetRepository;

    @Autowired
    private DashboardConfigRepository dashboardRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private com.nexainsights.service.DashboardGenerationService dashboardService;

    @Autowired
    private com.nexainsights.repository.DataRecordRepository dataRecordRepository;

    @GetMapping
    public String defaultDashboard(@RequestParam(required = false) String datasetId, Model model) {
        List<Dataset> datasets = datasetRepository.findAll();
        if (datasets.isEmpty()) {
            return "redirect:/import";
        }

        Dataset selectedDataset = null;
        if (datasetId != null && !datasetId.isEmpty()) {
            selectedDataset = datasetRepository.findById(datasetId).orElse(datasets.get(0));
        } else {
            selectedDataset = datasets.get(0);
        }

        return "redirect:/dashboard/" + selectedDataset.getId();
    }

    @GetMapping("/{datasetId}")
    public String viewDashboard(@PathVariable String datasetId, Model model) {
        Dataset dataset = datasetRepository.findById(datasetId).orElse(null);
        if (dataset == null) {
            return "redirect:/import";
        }

        DashboardConfig dashboardConfig = dashboardRepository.findByDatasetId(datasetId)
                .orElse(null);

        if (dashboardConfig == null) {
            dashboardConfig = dashboardService.generateDefaultDashboard(dataset);
        }

        List<Dataset> allDatasets = datasetRepository.findAll();
        Project project = dataset.getProjectId() != null ? projectRepository.findById(dataset.getProjectId()).orElse(null) : null;

        // Obtener últimos registros para las tablas inferiores
        List<com.nexainsights.model.DataRecord> recentRecords = dataRecordRepository.findTop50ByDatasetId(datasetId);

        model.addAttribute("dataset", dataset);
        model.addAttribute("dashboard", dashboardConfig);
        model.addAttribute("allDatasets", allDatasets);
        model.addAttribute("project", project);
        model.addAttribute("recentRecords", recentRecords);

        return "dashboard/view";
    }

    @org.springframework.web.bind.annotation.PostMapping("/custom-chart")
    @org.springframework.web.bind.annotation.ResponseBody
    public org.springframework.http.ResponseEntity<?> createCustomChart(@org.springframework.web.bind.annotation.RequestBody com.nexainsights.dto.CustomChartRequestDto dto) {
        try {
            Dataset dataset = datasetRepository.findById(dto.getDatasetId()).orElseThrow();
            com.nexainsights.model.DashboardConfig.ChartWidget widget = dashboardService.addCustomChart(dto, dataset);
            return org.springframework.http.ResponseEntity.ok(widget);
        } catch (Exception e) {
            return org.springframework.http.ResponseEntity.badRequest().body("Error al crear gráfico: " + e.getMessage());
        }
    }

    @org.springframework.web.bind.annotation.PostMapping("/custom-chart/preview")
    @org.springframework.web.bind.annotation.ResponseBody
    public org.springframework.http.ResponseEntity<?> previewCustomChart(@org.springframework.web.bind.annotation.RequestBody com.nexainsights.dto.CustomChartRequestDto dto) {
        try {
            Dataset dataset = datasetRepository.findById(dto.getDatasetId()).orElseThrow();
            com.nexainsights.model.DashboardConfig.ChartWidget widget = dashboardService.previewCustomChart(dto, dataset);
            return org.springframework.http.ResponseEntity.ok(widget);
        } catch (Exception e) {
            return org.springframework.http.ResponseEntity.badRequest().body("Error en previsualización: " + e.getMessage());
        }
    }
}
