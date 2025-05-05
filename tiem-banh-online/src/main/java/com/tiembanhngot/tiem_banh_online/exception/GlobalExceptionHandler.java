package com.tiembanhngot.tiem_banh_online.exception;

import jakarta.persistence.EntityNotFoundException; // Import nếu dùng JPA exception trực tiếp
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.NoHandlerFoundException; // Cho lỗi 404

@ControllerAdvice // Áp dụng cho tất cả Controller
@Slf4j
public class GlobalExceptionHandler {

    // Xử lý lỗi 404 Not Found (khi không tìm thấy URL handler)
    @ExceptionHandler(NoHandlerFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleNoHandlerFound(NoHandlerFoundException ex, Model model) {
        log.warn("Resource not found: {}", ex.getRequestURL());
        model.addAttribute("errorMessage", "Lỗi 404: Trang bạn tìm kiếm không tồn tại.");
        model.addAttribute("errorDetails", ex.getMessage());
        return "error/404"; // Trả về view /templates/error/404.html
    }

    // Xử lý lỗi không tìm thấy Entity (ví dụ: Product, Order, Category)
    @ExceptionHandler({ProductNotFoundException.class, OrderNotFoundException.class, CategoryNotFoundException.class, EntityNotFoundException.class})
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleResourceNotFound(RuntimeException ex, Model model) {
        log.warn("Resource not found: {}", ex.getMessage());
        model.addAttribute("errorMessage", "Lỗi 404: Không tìm thấy tài nguyên yêu cầu.");
        model.addAttribute("errorDetails", ex.getMessage());
        return "error/404"; // Trả về view /templates/error/404.html
    }

    // Xử lý lỗi bị từ chối truy cập (phân quyền)
    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public String handleAccessDenied(AccessDeniedException ex, Model model) {
        log.warn("Access denied: {}", ex.getMessage());
        model.addAttribute("errorMessage", "Lỗi 403: Bạn không có quyền truy cập tài nguyên này.");
        model.addAttribute("errorDetails", ex.getMessage());
        return "error/403"; // Trả về view /templates/error/403.html
    }

     // Xử lý lỗi ràng buộc dữ liệu (ví dụ: unique constraint)
    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.CONFLICT) // Hoặc BAD_REQUEST tùy ngữ cảnh
    public String handleDataIntegrityViolation(DataIntegrityViolationException ex, Model model) {
         log.error("Data integrity violation: {}", ex.getMessage());
         // Cố gắng đưa ra thông báo thân thiện hơn
         String userMessage = "Lỗi ràng buộc dữ liệu. Dữ liệu bạn nhập có thể bị trùng lặp hoặc không hợp lệ.";
         if (ex.getCause() != null && ex.getCause().getMessage() != null) {
              String causeMessage = ex.getCause().getMessage().toLowerCase();
              if (causeMessage.contains("unique constraint") || causeMessage.contains("duplicate key")) {
                  userMessage = "Lỗi: Dữ liệu bạn nhập bị trùng với dữ liệu đã tồn tại.";
                   // Có thể cố gắng phân tích chi tiết hơn về constraint nào bị vi phạm
              } else if (causeMessage.contains("foreign key constraint")) {
                   userMessage = "Lỗi: Không thể thực hiện thao tác do ràng buộc khóa ngoại.";
              }
         }
         model.addAttribute("errorMessage", userMessage);
         model.addAttribute("errorDetails", ex.getMessage()); // Thông tin chi tiết cho dev
         return "error/500"; // Hoặc trả về form trước đó với lỗi
    }


    // Xử lý các lỗi RuntimeException chung khác (Lỗi 500)
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String handleGenericError(Exception ex, Model model) {
        log.error("An unexpected error occurred: ", ex); // Log stack trace đầy đủ
        model.addAttribute("errorMessage", "Lỗi 500: Đã có lỗi xảy ra phía máy chủ. Vui lòng thử lại sau.");
        // Không nên hiển thị ex.getMessage() trực tiếp cho người dùng cuối vì lý do bảo mật
        model.addAttribute("errorDetails", "Internal Server Error"); // Thông báo chung
        return "error/500"; // Trả về view /templates/error/500.html
    }
}