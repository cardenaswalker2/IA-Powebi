package com.nexainsights.controller;

import com.nexainsights.dto.ChatRequestDto;
import com.nexainsights.dto.ChatResponseDto;
import com.nexainsights.model.ChatSession;
import com.nexainsights.model.Dataset;
import com.nexainsights.repository.ChatSessionRepository;
import com.nexainsights.repository.DatasetRepository;
import com.nexainsights.service.GroqAiAssistantService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/assistant")
public class AssistantController {

    @Autowired
    private DatasetRepository datasetRepository;

    @Autowired
    private ChatSessionRepository chatSessionRepository;

    @Autowired
    private GroqAiAssistantService groqAssistantService;

    @GetMapping
    public String showAssistantView(@RequestParam(required = false) String datasetId,
                                    @RequestParam(required = false) String sessionId,
                                    Model model) {
        List<Dataset> datasets = datasetRepository.findAll();
        if (datasets.isEmpty()) {
            return "redirect:/import";
        }

        Dataset selectedDataset;
        if (datasetId != null && !datasetId.isEmpty()) {
            selectedDataset = datasetRepository.findById(datasetId).orElse(datasets.get(0));
        } else {
            selectedDataset = datasets.get(0);
        }

        ChatSession activeSession = null;
        if (sessionId != null && !sessionId.isEmpty()) {
            activeSession = chatSessionRepository.findById(sessionId).orElse(null);
        }

        List<ChatSession> recentSessions = chatSessionRepository.findByDatasetIdOrderByUpdatedAtDesc(selectedDataset.getId());

        model.addAttribute("datasets", datasets);
        model.addAttribute("selectedDataset", selectedDataset);
        model.addAttribute("activeSession", activeSession);
        model.addAttribute("recentSessions", recentSessions);

        return "assistant/index";
    }

    @PostMapping("/query")
    @ResponseBody
    public ResponseEntity<ChatResponseDto> askAssistant(@RequestBody ChatRequestDto request) {
        try {
            ChatResponseDto response = groqAssistantService.processUserQuery(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ChatResponseDto("Error: " + e.getMessage(), null));
        }
    }
}
