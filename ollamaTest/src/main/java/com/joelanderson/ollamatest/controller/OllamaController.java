package com.joelanderson.ollamatest.controller;

import com.joelanderson.ollamatest.service.OllamaService;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(origins = "http://localhost:5173")
@RestController
@RequestMapping("/api/ai")
public class OllamaController {

    private final OllamaService ollamaService;

    public OllamaController(OllamaService ollamaService) {
        this.ollamaService = ollamaService;
    }

    @GetMapping("/generate")
    public String generateQuestions(@RequestParam String topic) {
        return ollamaService.generateQuestions(topic);
    }
}
