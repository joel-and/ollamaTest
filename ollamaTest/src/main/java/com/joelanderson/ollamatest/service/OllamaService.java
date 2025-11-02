package com.joelanderson.ollamatest.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.InputStream;
import java.util.Map;

@Service
public class OllamaService {

    private static final String OLLAMA_URL = "http://localhost:11434/api/generate";

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper mapper = new ObjectMapper();

    // --- existing text-only generation ---
    public String generateQuestions(String topic) {
        String prompt = basePrompt(topic, null);
        return callOllamaAndReturnQuestionsJson(prompt);
    }

    // --- NEW: PDF-based generation ---
    public String generateQuestionsFromPdf(String topic, MultipartFile file) {
        try {
            // 1. Extract text from the uploaded PDF
            String pdfText = extractTextFromPdf(file);

            // 2. (very important) Safety: trim to avoid sending a whole textbook
            // You can tune this. We keep first ~4000 chars to stay prompt-friendly.
            String limitedNotes = pdfText.length() > 4000
                    ? pdfText.substring(0, 4000)
                    : pdfText;

            // 3. Build prompt using both topic AND the student's notes
            String prompt = basePrompt(topic, limitedNotes);

            // 4. Ask Ollama to generate questions using those notes
            return callOllamaAndReturnQuestionsJson(prompt);

        } catch (Exception e) {
            // if PDF parsing fails or file was empty
            return "{\"questions\":[], \"error\":\"Failed to read PDF\"}";
        }
    }

    // helper: extract text from pdf
    private String extractTextFromPdf(MultipartFile file) throws Exception {
        try (InputStream is = file.getInputStream();
             PDDocument doc = PDDocument.load(is)) {

            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            return stripper.getText(doc).replaceAll("\\s+", " ").trim();
        }
    }

    // helper: build the instruction prompt
    private String basePrompt(String topic, String notesOrNull) {
        // We instruct the model to return STRICT JSON. This matches what already works for you.
        // If notesOrNull is provided, we tell the model: "base questions ONLY on these notes".
        String notesSection = (notesOrNull == null || notesOrNull.isBlank())
                ? "You do NOT have any source notes, only the topic."
                : "Use ONLY the following student revision notes as source material:\n\"\"\"\n"
                + notesOrNull +
                "\n\"\"\"\nIf something is not in these notes, do not invent it.";

        return """
            You generate exam-style questions.

            %s

            Return ONLY a valid JSON object matching this schema (no backticks, no extra text before or after the JSON):
            {
              "questions": [
                {"number": 1, "topic": "string", "text": "string"},
                {"number": 2, "topic": "string", "text": "string"},
                {"number": 3, "topic": "string", "text": "string"}
              ]
            }

            Requirements:
            - Exactly 3 items in "questions".
            - Each "text" is a single clear question, not an answer.
            - Use normal spacing and punctuation.
            - Don't include answers or marking schemes.
            - Stay appropriate for an exam.

            The exam topic / subject area is: %s
            """.formatted(notesSection, topic);
    }

    // helper: actually call Ollama and cleanly return { "questions": [...] }
    private String callOllamaAndReturnQuestionsJson(String prompt) {
        Map<String, Object> requestBody = Map.of(
                "model", "mistral",
                "prompt", prompt,
                "format", "json",
                "stream", false,
                "options", Map.of("temperature", 0.2)
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        ResponseEntity<String> resp = restTemplate.postForEntity(OLLAMA_URL, entity, String.class);
        String body = resp.getBody();
        if (body == null || body.isBlank()) return "{\"questions\":[]}";

        try {
            // Ollama response has shape { ... "response": "<json string from model>" ... }
            JsonNode root = mapper.readTree(body);
            String innerJson = root.path("response").asText();

            // (models sometimes wrap in ```json ... ``` fences, strip them if present)
            innerJson = innerJson
                    .replaceAll("^```(?:json)?\\s*", "")
                    .replaceAll("\\s*```\\s*$", "");

            JsonNode questionsObj = mapper.readTree(innerJson);

            if (!questionsObj.has("questions") || !questionsObj.get("questions").isArray()) {
                return "{\"questions\":[]}";
            }

            // Return re-serialized clean json
            return mapper.writeValueAsString(questionsObj);

        } catch (Exception e) {
            return "{\"questions\":[]}";
        }
    }
}
