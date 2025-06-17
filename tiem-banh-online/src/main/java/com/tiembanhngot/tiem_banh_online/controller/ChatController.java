// package com.tiembanhngot.tiem_banh_online.controller; // HOẶC PACKAGE PHÙ HỢP

// import com.tiembanhngot.tiem_banh_online.service.RAGService;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.http.ResponseEntity;
// import org.springframework.web.bind.annotation.*;

// import jakarta.annotation.PostConstruct; // Cho việc index dữ liệu mẫu
// import java.util.Arrays;                  // Cho việc index dữ liệu mẫu
// import java.util.List;                   // Cho việc index dữ liệu mẫu
// import java.util.Map;                    // Cho request body
// import java.util.Collections;

// @RestController
// @RequestMapping("/api/v1/chat") // Hoặc một endpoint phù hợp
// public class ChatController {

//     @Autowired
//     private RAGService ragService;

//     // Ví dụ: Index một vài tài liệu mẫu khi ứng dụng khởi động
//     @PostConstruct
//     public void initKnowledgeBase() {
//         System.out.println("Initializing RAG knowledge base with sample documents...");
//         List<String> sampleDocuments = Arrays.asList(
//             "Bánh mì ngọt là loại bánh mì có vị ngọt, thường được dùng cho bữa sáng hoặc ăn nhẹ. Các loại phổ biến bao gồm bánh mì sữa, bánh mì nho, bánh mì custard.",
//             "Bánh kem thường được dùng trong các dịp sinh nhật và lễ kỷ niệm. Bánh kem có nhiều hương vị như dâu, chocolate, vani và được trang trí đẹp mắt.",
//             "Croissant là một loại bánh ngọt kiểu Pháp, có hình lưỡi liềm, được làm từ bột ngàn lớp và bơ. Bánh có vị thơm bơ và giòn xốp.",
//             "Tiệm Bánh Ngon phục vụ từ 8 giờ sáng đến 9 giờ tối hàng ngày, kể cả cuối tuần và ngày lễ.",
//             "Để đặt bánh theo yêu cầu, quý khách vui lòng liên hệ số điện thoại 0900xxxxxx hoặc đặt trực tiếp tại cửa hàng trước ít nhất 1 ngày."
//         );
//         ragService.indexDocuments(sampleDocuments);
//         System.out.println("Knowledge base initialized.");
//     }

//     // Endpoint để người dùng gửi câu hỏi
//     // Nhận câu hỏi dạng JSON: {"query": "câu hỏi của bạn"}
//     @PostMapping("/ask")
//     public ResponseEntity<Map<String, String>> askQuestion(@RequestBody Map<String, String> payload) {
//         String userQuery = payload.get("query");
//         if (userQuery == null || userQuery.trim().isEmpty()) {
//             return ResponseEntity.badRequest().body(Collections.singletonMap("error", "Câu hỏi không được để trống."));
//         }
//         String response = ragService.getRAGResponse(userQuery);
//         return ResponseEntity.ok(Collections.singletonMap("answer", response));
//     }
// }