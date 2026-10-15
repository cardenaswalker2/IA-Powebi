package com.nexainsights.controller;

import com.nexainsights.model.Dataset;
import com.nexainsights.model.Project;
import com.nexainsights.repository.DatasetRepository;
import com.nexainsights.repository.ProjectRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class HomeController {

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private DatasetRepository datasetRepository;

    @GetMapping("/")
    public String index(Model model) {
        List<Project> projects = projectRepository.findAll();
        List<Dataset> datasets = datasetRepository.findAll();

        long totalRowsAcrossAll = datasets.stream().mapToLong(Dataset::getTotalRows).sum();

        model.addAttribute("projects", projects);
        model.addAttribute("datasets", datasets);
        model.addAttribute("totalDatasets", datasets.size());
        model.addAttribute("totalRows", totalRowsAcrossAll);

        return "index";
    }
}
