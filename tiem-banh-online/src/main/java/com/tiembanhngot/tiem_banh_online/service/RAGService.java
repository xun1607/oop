package com.tiembanhngot.tiem_banh_online.service; 

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

@Service
public class RAGService {

    @Autowired
    private VectorDBService vectorDBService;

    private final String HUGGING_FACE_API = "https://api-inference.huggingface.co/models/tiiuae/falcon-7b-instruct";
    
    private final String HF_TOKEN = "Bearer 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"; 
    private final ObjectMapper objectMapper = new ObjectMapper();

    public String getAnswer(String question) {
        String context = vectorDBService.searchRelevantContext(question);
        if (context == null || context.trim().isEmpty()) {
            context = "Không tìm thấy thông tin liên quan trong cơ sở dữ liệu.";
        }

        String ragPrompt = "Dựa trên thông tin này: \"" + context.trim().replace("\n", " ") + "\". Trả lời câu hỏi sau: " + question;
        try {
            HttpClient client = HttpClient.newHttpClient();

            String requestBody = objectMapper.writeValueAsString(Map.of("inputs", ragPrompt, "parameters", Map.of("max_new_tokens", 100, "return_full_text", false)));
            URI apiUri = URI.create(HUGGING_FACE_API);
            System.out.println("Đang gọi đến API URI: " + apiUri.toString());
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(HUGGING_FACE_API))
                    .header("Authorization", HF_TOKEN) // HF_TOKEN should include "Bearer "
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JsonNode rootNode = objectMapper.readTree(response.body());
                if (rootNode.isArray() && rootNode.size() > 0) {
                    JsonNode generatedTextNode = rootNode.get(0).get("generated_text");
                    if (generatedTextNode != null) {
                        String fullGeneratedText = generatedTextNode.asText();
                        return fullGeneratedText;
                    }
                }
                System.err.println("Unexpected JSON structure from Hugging Face: " + response.body());
                return "Trợ lý đã phản hồi, nhưng có lỗi khi xử lý câu trả lời. Nội dung thô: " + response.body();
            } else {
                System.err.println("Error from Hugging Face API: " + response.statusCode() + " - " + response.body());
                return "Xin lỗi, có lỗi khi kết nối đến trợ lý AI: " + response.statusCode();
            }

        } catch (Exception e) {
            e.printStackTrace();
            return "Có lỗi xảy ra trong quá trình xử lý yêu cầu của bạn.";
        }
    }
}