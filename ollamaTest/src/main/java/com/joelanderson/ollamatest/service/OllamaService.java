package com.joelanderson.ollamatest.service;

import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.Map;
import java.util.stream.Collectors;


@Service
public class OllamaService {

    private final RestTemplate restTemplate = new RestTemplate();
    private static final String OLLAMA_URL = "http://localhost:11434/api/generate";

    public String generateQuestions(String topic) {
        String prompt = "Generate 3 exam-style questions about " + topic;

        Map<String, Object> requestBody = Map.of(
                "model", "mistral",
                "prompt", prompt
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(OLLAMA_URL, entity, String.class);

        // Ollama returns one JSON object per line. Extract each "response" field.
        String raw = response.getBody();
        if (raw == null) return "{\"response\":\"No output from Ollama\"}";

        String cleaned = raw.lines()
                .filter(line -> line.contains("\"response\""))
                .map(line -> {
                    try {
                        int start = line.indexOf("\"response\":\"") + 12;
                        int end = line.indexOf("\"", start);
                        return line.substring(start, end);
                    } catch (Exception e) {
                        return "";
                    }
                })
                .filter(s -> !s.isBlank())
                .collect(Collectors.joining(" "));

        return "{\"response\": \"" + cleaned.replace("\"", "\\\"") + "\"}";
    }
}
