package com.joelanderson.ollamatest.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class OllamaService {

    private static final String OLLAMA_URL = "http://localhost:11434/api/generate";

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper mapper = new ObjectMapper();

    public String generateQuestions(String topic) {
        // 1) Tell the model EXACTLY what JSON to return (no prose, no Markdown).
        String prompt = """
            You generate exam-style questions.

            Return ONLY a valid JSON object matching this schema (no backticks, no extra text):
            {
              "questions": [
                {"number": 1, "topic": "string", "text": "string"},
                {"number": 2, "topic": "string", "text": "string"},
                {"number": 3, "topic": "string", "text": "string"}
              ]
            }

            Requirements:
            - Exactly 3 items in "questions".
            - Each "text" should be a clear, single question (no answers).
            - Use normal spacing and punctuation.
            - Do not include code fences or explanations outside the JSON.

            Topic: %s
            """.formatted(topic);

        // 2) Ask Ollama for JSON and disable streaming.
        Map<String, Object> requestBody = Map.of(
                "model", "mistral",
                "prompt", prompt,
                "format", "json",         // ← ask for JSON
                "stream", false,          // ← get a single response, not a stream
                "options", Map.of("temperature", 0.2)
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        ResponseEntity<String> resp = restTemplate.postForEntity(OLLAMA_URL, entity, String.class);
        String body = resp.getBody();
        if (body == null || body.isBlank()) return "{\"questions\":[]}";

        try {
            // 3) Ollama wraps the model output in a JSON object with a "response" field.
            //    When format=json, that field itself is a JSON string we need to parse.
            JsonNode root = mapper.readTree(body);
            String innerJson = root.path("response").asText(); // the JSON string produced by the model

            // 4) Validate / normalize and return as proper JSON.
            JsonNode questionsObj = mapper.readTree(innerJson); // should be {"questions":[...]}
            // (Optional) light validation:
            if (!questionsObj.has("questions") || !questionsObj.get("questions").isArray()) {
                return "{\"questions\":[]}";
            }
            // Re-serialize to ensure valid JSON is returned to the frontend
            return mapper.writeValueAsString(questionsObj);

        } catch (Exception e) {
            // Fallback: empty list if the model didn't follow instructions
            return "{\"questions\":[]}";
        }
    }
}
