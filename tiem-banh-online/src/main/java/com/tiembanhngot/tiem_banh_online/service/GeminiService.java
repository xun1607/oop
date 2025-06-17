// package com.tiembanhngot.tiem_banh_online.service; // HOẶC PACKAGE PHÙ HỢP

// import com.google.cloud.vertexai.VertexAI;
// import com.google.cloud.vertexai.api.GenerateContentResponse;
// // import com.google.cloud.vertexai.api.Content; // Không dùng trực tiếp trong code này
// // import com.google.cloud.vertexai.api.Part; // Không dùng trực tiếp trong code này
// import com.google.cloud.vertexai.api.HarmCategory;
// import com.google.cloud.vertexai.api.SafetySetting;
// import com.google.cloud.vertexai.generativeai.*;
// import org.springframework.beans.factory.annotation.Value;
// import org.springframework.stereotype.Service;

// import java.io.IOException;
// import java.util.ArrayList; // Import ArrayList
// import java.util.Collections;
// import java.util.List;
// import java.util.stream.Collectors;
// import com.google.auth.oauth2.GoogleCredentials;
// import java.io.FileInputStream;
// import java.net.URI;
// import java.net.http.HttpClient;
// import java.net.http.HttpRequest;
// import java.net.http.HttpResponse;
// import com.fasterxml.jackson.databind.JsonNode;
// import com.fasterxml.jackson.databind.ObjectMapper;
// import org.springframework.core.io.ClassPathResource; // Thêm import này
// import java.io.InputStream; // Thêm import này

// @Service
// public class GeminiService {

//     @Value("${google.api.key}")
//     private String apiKey;

//     @Value("${google.project.id}")
//     private String projectId;

//     @Value("${google.location}")
//     private String location;

//     @Value("${google.credentials.path}")
//     private String credentialsPath; // Đường dẫn tới file service account

//     // Chọn model phù hợp, text-embedding-004 thường tốt hơn
//     private static final String EMBEDDING_MODEL_NAME = "text-embedding-004";
//     // Hoặc "gemini-1.5-flash-latest", "gemini-1.5-pro-latest", "gemini-1.0-pro"
//     private static final String GENERATIVE_MODEL_NAME = "gemini-1.5-flash-latest";

//     /**
//      * Tạo embeddings cho một đoạn văn bản.
//      */
//     public List<Double> getEmbeddings(String text) throws IOException {
//         // Lấy access token từ service account
//         GoogleCredentials credentials;
//         try (InputStream credentialsStream = new ClassPathResource(credentialsPath.replace("classpath:", "")).getInputStream()) {
//         // credentialsPath ở đây sẽ là "classpath:service-account-key.json"
//         // ClassPathResource cần tên file không có "classpath:"
//         // nên ta sẽ bỏ "classpath:" đi khi truyền vào ClassPathResource
//         credentials = GoogleCredentials.fromStream(credentialsStream)
//             .createScoped("https://www.googleapis.com/auth/cloud-platform");
//         }
//         // Bây giờ bạn có thể dùng 'credentials'
//         credentials.refreshIfExpired();
//         String accessToken = credentials.getAccessToken().getTokenValue();
//         // Endpoint của Vertex AI Embedding
//         String endpoint = String.format(
//             "https://%s-aiplatform.googleapis.com/v1/projects/%s/locations/%s/publishers/google/models/%s:predict",
//             location, projectId, location, EMBEDDING_MODEL_NAME
//         );

//         // Body request
//         String jsonBody = String.format("""
//         {
//           \"instances\": [
//             {\"content\": \"%s\"}
//           ]
//         }
//         """, text.replace("\"", "\\\""));

//         // Gửi request
//         HttpRequest request = HttpRequest.newBuilder()
//                 .uri(URI.create(endpoint))
//                 .header("Authorization", "Bearer " + accessToken)
//                 .header("Content-Type", "application/json")
//                 .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
//                 .build();

//         HttpClient client = HttpClient.newHttpClient();
//         HttpResponse<String> response;
//         try {
//             response = client.send(request, HttpResponse.BodyHandlers.ofString());
//             System.out.println("VERTEX AI EMBEDDING API - Status Code: " + response.statusCode()); // LOG QUAN TRỌNG
//             System.out.println("VERTEX AI EMBEDDING API - Response Body: " + response.body());
//         } catch (InterruptedException e) {
//             Thread.currentThread().interrupt();
//             throw new IOException("Request bị gián đoạn", e);
//         }

//         // Parse kết quả
//         ObjectMapper mapper = new ObjectMapper();
//         JsonNode root = mapper.readTree(response.body());
//         JsonNode embeddingNode = root.at("/predictions/0/embeddings/values");
//         List<Double> embedding = new ArrayList<>();
//         if (embeddingNode.isArray()) {
//             for (JsonNode value : embeddingNode) {
//                 embedding.add(value.asDouble());
//             }
//         }
//         return embedding;
//     }

//     /**
//      * Tạo phản hồi từ LLM dựa trên prompt.
//      */
//     public String generateResponse(String promptText) throws IOException {
//         try (VertexAI vertexAi = new VertexAI(projectId, location)) {
//             // Khởi tạo GenerativeModel với tên model sinh văn bản và cấu hình an toàn
//             GenerativeModel model = new GenerativeModel(GENERATIVE_MODEL_NAME, vertexAi)
//                 .withSafetySettings(Collections.singletonList(
//                     SafetySetting.newBuilder()
//                         .setCategory(HarmCategory.HARM_CATEGORY_HARASSMENT)
//                         .setThreshold(SafetySetting.HarmBlockThreshold.BLOCK_ONLY_HIGH)
//                         .build()
//                     // Thêm các SafetySetting khác nếu cần
//                 ));

//             // Gọi API để sinh nội dung
//             GenerateContentResponse response = model.generateContent(promptText);

//             // Xử lý và trả về phần text của response
//             return ResponseHandler.getText(response);
//         }
//     }
// }