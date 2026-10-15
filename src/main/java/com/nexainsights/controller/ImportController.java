package com.nexainsights.controller;

import com.nexainsights.dto.PreviewResponseDto;
import com.nexainsights.model.Dataset;
import com.nexainsights.model.Project;
import com.nexainsights.repository.DatasetRepository;
import com.nexainsights.repository.ProjectRepository;
import com.nexainsights.service.FileImportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Controller
@RequestMapping("/import")
public class ImportController {

    @Autowired
    private FileImportService fileImportService;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private DatasetRepository datasetRepository;

    @GetMapping
    public String showImportView(@RequestParam(required = false) String projectId, Model model) {
        List<Project> projects = projectRepository.findAll();
        if (projects.isEmpty()) {
            Project defaultProject = new Project("Proyecto General de Auditorías y Operaciones", "Proyecto inicial para analítica");
            defaultProject = projectRepository.save(defaultProject);
            projects.add(defaultProject);
        }

        model.addAttribute("projects", projects);
        model.addAttribute("selectedProjectId", (projectId != null && !projectId.isEmpty()) ? projectId : projects.get(0).getId());
        return "import/index";
    }

    @PostMapping("/preview")
    @ResponseBody
    public ResponseEntity<?> previewFile(@RequestParam("file") MultipartFile file,
                                         @RequestParam(value = "sheet", required = false) String sheet,
                                         @RequestParam(value = "headerRow", required = false, defaultValue = "0") Integer headerRow) {
        try {
            PreviewResponseDto preview = fileImportService.generatePreview(file, sheet, headerRow);
            return ResponseEntity.ok(preview);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error al generar vista previa: " + e.getMessage());
        }
    }

    @PostMapping("/confirm")
    @ResponseBody
    public ResponseEntity<?> confirmImport(@RequestParam("file") MultipartFile file,
                                          @RequestParam("projectId") String projectId,
                                          @RequestParam(value = "datasetName", required = false) String datasetName,
                                          @RequestParam(value = "sheet", required = false) String sheet,
                                          @RequestParam(value = "headerRow", required = false, defaultValue = "0") Integer headerRow) {
        try {
            Dataset dataset = fileImportService.importFile(file, projectId, datasetName, sheet, headerRow);

            // Asociar al proyecto si no estaba
            Project project = projectRepository.findById(projectId).orElse(null);
            if (project != null && !project.getDatasetIds().contains(dataset.getId())) {
                project.getDatasetIds().add(dataset.getId());
                projectRepository.save(project);
            }

            return ResponseEntity.ok(dataset);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error al importar el archivo: " + e.getMessage());
        }
    }
}
