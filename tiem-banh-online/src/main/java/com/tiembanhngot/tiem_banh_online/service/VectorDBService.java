// package com.tiembanhngot.tiem_banh_online.service; // HOẶC PACKAGE PHÙ HỢP

// import org.springframework.stereotype.Service;
// import java.util.ArrayList;
// import java.util.Collections;
// import java.util.Comparator;
// import java.util.HashMap;
// import java.util.List;
// import java.util.Map;
// import java.util.stream.Collectors;

// @Service
// public class VectorDBService {
//     // Lưu trữ đơn giản trong bộ nhớ (CHỈ DÙNG CHO DEMO)
//     private final Map<String, List<Double>> vectorStore = new HashMap<>();
//     private final Map<String, String> documentStore = new HashMap<>();

//     public void addDocument(String docId, String text, List<Double> embedding) {
//         if (docId == null || text == null || embedding == null || embedding.isEmpty()) {
//             System.err.println("Invalid document data for ID: " + docId);
//             return;
//         }
//         vectorStore.put(docId, embedding);
//         documentStore.put(docId, text);
//         System.out.println("Added to In-Memory VectorDB: " + docId);
//     }

//     public List<String> searchSimilar(List<Double> queryEmbedding, int topK) {
//         if (queryEmbedding == null || queryEmbedding.isEmpty() || vectorStore.isEmpty()) {
//             return Collections.emptyList();
//         }

//         // Cấu trúc để lưu trữ docId và độ tương đồng
//         List<Map.Entry<String, Double>> similarities = new ArrayList<>();

//         for (Map.Entry<String, List<Double>> entry : vectorStore.entrySet()) {
//             double similarity = cosineSimilarity(queryEmbedding, entry.getValue());
//             similarities.add(Map.entry(entry.getKey(), similarity));
//         }

//         // Sắp xếp theo độ tương đồng giảm dần
//         similarities.sort((e1, e2) -> e2.getValue().compareTo(e1.getValue()));

//         // Lấy topK document texts
//         return similarities.stream()
//                 .limit(topK)
//                 .map(entry -> documentStore.get(entry.getKey()))
//                 .collect(Collectors.toList());
//     }

//     // Hàm tính cosine similarity đơn giản
//     private double cosineSimilarity(List<Double> vecA, List<Double> vecB) {
//         if (vecA.size() != vecB.size() || vecA.isEmpty()) {
//             return 0.0; // Hoặc ném exception
//         }
//         double dotProduct = 0.0;
//         double normA = 0.0;
//         double normB = 0.0;
//         for (int i = 0; i < vecA.size(); i++) {
//             dotProduct += vecA.get(i) * vecB.get(i);
//             normA += Math.pow(vecA.get(i), 2);
//             normB += Math.pow(vecB.get(i), 2);
//         }
//         if (normA == 0.0 || normB == 0.0) return 0.0; // Tránh chia cho 0
//         return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
//     }
// }