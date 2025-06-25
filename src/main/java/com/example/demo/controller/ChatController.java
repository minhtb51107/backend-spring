package com.example.demo.controller;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = {
	    "http://localhost:5173", 
	    "https://frontend-vue-puce.vercel.app/"
	})

public class ChatController {

    @PostMapping("/chat")
    public ResponseEntity<Map<String, String>> chat(@RequestBody Map<String, String> request) {
        String message = request.get("message");
        String reply = callOpenAI(message);
        Map<String, String> response = new HashMap<>();
        response.put("reply", reply);
        return ResponseEntity.ok(response);
    }

    private String callOpenAI(String message) {
        try {
            HttpClient client = HttpClient.newHttpClient();

            // ✅ Dùng API Key của bạn ở đây (đừng commit lên GitHub nếu có)
           String apiKey = System.getenv("OPENAI_API_KEY");

            String body = """
            {
              "model": "gpt-4o",
              "messages": [
                { "role": "system", "content": "Bạn là một trợ lý AI chuyên dạy lập trình." },
                { "role": "user", "content": "%s" }
              ],
              "temperature": 0.7
            }
            """.formatted(message);

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.openai.com/v1/chat/completions"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            System.out.println("=== OPENAI RAW RESPONSE ===");
            System.out.println(response.body());

            ObjectMapper mapper = new ObjectMapper();
            JsonNode json = mapper.readTree(response.body());

            // Nếu có lỗi
            if (json.has("error")) {
                String errMsg = json.get("error").get("message").asText("Lỗi không xác định từ OpenAI.");
                System.err.println("OpenAI API Error: " + errMsg);
                return "Lỗi từ AI: " + errMsg;
            }

            // Trích xuất nội dung trả lời
            JsonNode contentNode = json
                .path("choices")
                .path(0)
                .path("message")
                .path("content");

            if (!contentNode.isMissingNode()) {
                return contentNode.asText();
            } else {
                return "Không nhận được phản hồi từ OpenAI.";
            }

        } catch (Exception e) {
            e.printStackTrace();
            return "Xin lỗi, tôi gặp lỗi khi phản hồi từ AI.";
        }
    }
}
