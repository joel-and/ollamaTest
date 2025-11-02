package com.joelanderson.ollamatest.controller;

import com.joelanderson.ollamatest.service.OllamaService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.MediaType;

@RestController
@RequestMapping("/api/ai")
@CrossOrigin(origins = "http://localhost:5173")
public class OllamaController {

    private final OllamaService ollamaService;

    public OllamaController(OllamaService ollamaService) {
        this.ollamaService = ollamaService;
    }

    // existing GET /generate?topic=...
    @GetMapping("/generate")
    public String generateQuestions(@RequestParam String topic) {
        return ollamaService.generateQuestions(topic);
    }

    // NEW: POST /generateFromPdf
    @PostMapping(
            path = "/generateFromPdf",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public String generateQuestionsFromPdf(
            @RequestPart("file") MultipartFile file,
            @RequestPart("topic") String topic
    ) {
        return ollamaService.generateQuestionsFromPdf(topic, file);
    }
}
