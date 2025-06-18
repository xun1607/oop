// package com.tiembanhngot.tiem_banh_online.service; // HOẶC PACKAGE PHÙ HỢP

// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.stereotype.Service;
// import java.io.IOException;
// import java.util.List;
// import java.util.stream.Collectors;
// import com.fasterxml.jackson.databind.JsonNode;
// import com.fasterxml.jackson.databind.ObjectMapper;

// @Service
// public class RAGService {

//     @Autowired
//     private GeminiService geminiService; // SỬ DỤNG GeminiService

//     @Autowired
//     private VectorDBService vectorDBService; // Giả sử bạn có service này

//     // Hàm này nên được gọi một lần để chuẩn bị dữ liệu (hoặc khi dữ liệu thay đổi)
//     public void indexDocuments(List<String> documents) {
//         if (documents == null || documents.isEmpty()) {
//             System.out.println("No documents to index.");
//             return;
//         }
//         for (int i = 0; i < documents.size(); i++) {
//             String doc = documents.get(i);
//             if (doc == null || doc.trim().isEmpty()) {
//                 System.err.println("Skipping empty document at index: " + i);
//                 continue;
//             }
//             try {
//                 // Thực tế bạn sẽ chia doc thành các chunks nhỏ hơn nếu doc quá dài
//                 System.out.println("Generating embedding for document (size: " + doc.length() + "): " + (doc.length() > 50 ? doc.substring(0, 50) + "..." : doc));
//                 List<Double> embedding = geminiService.getEmbeddings(doc);
//                 // Giả sử VectorDBService có phương thức addDocument
//                 vectorDBService.addDocument("doc_" + i, doc, embedding);
//                 System.out.println("Indexed document (using Gemini): " + i);
//             } catch (IOException e) {
//                 System.err.println("Error generating embedding for document " + i + " (" + (doc.length() > 50 ? doc.substring(0, 50) + "..." : doc) + "): " + e.getMessage());
//                 // Xử lý lỗi phù hợp, ví dụ: log, bỏ qua, thử lại
//             } catch (Exception e) { // Bắt các lỗi khác từ vectorDBService
//                  System.err.println("Error adding document " + i + " to VectorDB: " + e.getMessage());
//             }
//         }
//     }

//     public String getRAGResponse(String userQuery) {
//         if (userQuery == null || userQuery.trim().isEmpty()) {
//             return "Vui lòng cung cấp câu hỏi.";
//         }
//         try {
//             // 1. Tạo embedding cho câu hỏi người dùng
//             System.out.println("Generating embedding for query: " + userQuery);
//             List<Double> queryEmbedding = geminiService.getEmbeddings(userQuery);

//             if (queryEmbedding.isEmpty()) {
//                 System.err.println("Failed to generate embedding for the query.");
//                 return "Không thể xử lý câu hỏi của bạn vào lúc này.";
//             }

//             // 2. Truy xuất (Retrieve) các đoạn văn bản liên quan từ Vector DB
//             int topK = 3; // Số lượng context chunks muốn lấy
//             System.out.println("Searching for similar documents in VectorDB (topK=" + topK + ")");
//             List<String> retrievedContexts = vectorDBService.searchSimilar(queryEmbedding, topK);

//             if (retrievedContexts == null || retrievedContexts.isEmpty()) {
//                 System.out.println("No relevant contexts found in VectorDB.");
//                 // Có thể trả lời trực tiếp bằng LLM mà không có context, hoặc thông báo không tìm thấy
//                  return geminiService.generateResponse("Trả lời câu hỏi sau bằng tiếng Việt: " + userQuery + "\nNếu không biết, hãy nói không biết.");
//             }

//             // 3. Xây dựng prompt cho LLM
//             String contextString = retrievedContexts.stream().collect(Collectors.joining("\n\n---\n\n")); // Thêm separator
//             String prompt = String.format(
//                 "Dựa vào những thông tin sau đây:\n\"\"\"\n%s\n\"\"\"\n\n" +
//                 "Hãy trả lời câu hỏi một cách ngắn gọn và chính xác nhất có thể bằng tiếng Việt: \"%s\"\n" +
//                 "Nếu thông tin không đủ để trả lời hoặc câu hỏi không liên quan đến thông tin đã cho, hãy nói: " +
//                 "\"Tôi không có đủ thông tin để trả lời câu hỏi này dựa trên những gì tôi biết.\".",
//                 contextString,
//                 userQuery
//             );

//             System.out.println("---- PROMPT FOR GEMINI ----");
//             System.out.println(prompt);
//             System.out.println("---------------------------");

//             // 4. Gọi LLM để tạo câu trả lời (Generate)
//             String llmResponse = geminiService.generateResponse(prompt);
//             System.out.println("---- GEMINI RESPONSE ----");
//             System.out.println(llmResponse);
//             System.out.println("-------------------------");
//             return llmResponse;

//         } catch (IOException e) {
//             System.err.println("IOException in RAG process: " + e.getMessage());
//             e.printStackTrace();
//             return "Đã xảy ra lỗi vào lúc này (IO). Vui lòng thử lại sau.";
//         } catch (Exception e) {
//             System.err.println("Unexpected error in RAG process: " + e.getMessage());
//             e.printStackTrace();
//             return "Đã có lỗi không mong muốn xảy ra. Vui lòng thử lại sau.";
//         }
//     }
// }