package com.nexainsights.controller;

import com.nexainsights.model.Dataset;
import com.nexainsights.model.DatasetRelationship;
import com.nexainsights.model.Project;
import com.nexainsights.repository.DatasetRelationshipRepository;
import com.nexainsights.repository.DatasetRepository;
import com.nexainsights.repository.ProjectRepository;
import com.nexainsights.service.RelationshipDiscoveryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@RequestMapping("/relationships")
public class RelationshipController {

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private DatasetRepository datasetRepository;

    @Autowired
    private DatasetRelationshipRepository relationshipRepository;

    @Autowired
    private RelationshipDiscoveryService discoveryService;

    @GetMapping
    public String showRelationshipsView(@RequestParam(required = false) String projectId, Model model) {
        List<Project> projects = projectRepository.findAll();
        if (projects.isEmpty()) {
            return "redirect:/import";
        }

        Project selectedProject = (projectId != null && !projectId.isEmpty())
                ? projectRepository.findById(projectId).orElse(projects.get(0))
                : projects.get(0);

        List<Dataset> datasets = datasetRepository.findByProjectId(selectedProject.getId());
        List<DatasetRelationship> relationships = relationshipRepository.findByProjectId(selectedProject.getId());

        model.addAttribute("projects", projects);
        model.addAttribute("selectedProject", selectedProject);
        model.addAttribute("datasets", datasets);
        model.addAttribute("relationships", relationships);

        return "relationships/index";
    }

    @PostMapping("/discover")
    public String triggerDiscovery(@RequestParam String projectId) {
        discoveryService.discoverRelationships(projectId);
        return "redirect:/relationships?projectId=" + projectId;
    }
}
