package com.tiembanhngot.tiem_banh_online.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.UUID;

@Service
@Slf4j
public class StorageService {

    private final Path rootLocation;

    // Inject đường dẫn từ application.properties
    public StorageService(@Value("${app.upload.dir}") String uploadDir) {
        this.rootLocation = Paths.get(uploadDir);
        try {
            Files.createDirectories(rootLocation); // Tạo thư mục nếu chưa có
        } catch (IOException e) {
            throw new RuntimeException("Could not initialize storage location", e);
        }
    }

    // Lưu file và trả về tên file đã lưu (duy nhất)
    public String store(MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new IOException("Failed to store empty file.");
        }
        // Làm sạch tên file gốc để tránh các ký tự đặc biệt hoặc đường dẫn
        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename());
        // Kiểm tra cơ bản chống directory traversal attack
        if (originalFilename.contains("..")) {
            throw new IOException("Cannot store file with relative path outside current directory " + originalFilename);
        }

        // Tạo tên file duy nhất để tránh trùng lặp và vấn đề bảo mật với tên gốc
        String fileExtension = StringUtils.getFilenameExtension(originalFilename);
        String uniqueFilename = UUID.randomUUID().toString() + "." + fileExtension;

        // Tính toán đường dẫn đích đầy đủ
        Path destinationFile = this.rootLocation.resolve(uniqueFilename);

        // === THAY THẾ KHỐI KIỂM TRA AN NINH ===
        try {
            // Chuẩn hóa đường dẫn đích và đường dẫn gốc để so sánh đáng tin cậy
            Path normalizedDestination = destinationFile.normalize().toAbsolutePath();
            Path normalizedRoot = this.rootLocation.normalize().toAbsolutePath();

            // Log để debug (có thể xóa sau khi hoạt động)
            log.debug("Normalized Root Path: {}", normalizedRoot);
            log.debug("Normalized Destination Path: {}", normalizedDestination);
            log.debug("Destination Parent: {}", normalizedDestination.getParent());

            // Kiểm tra xem đường dẫn đích có thực sự nằm BÊN TRONG thư mục gốc không
            if (!normalizedDestination.startsWith(normalizedRoot)) {
                 log.error("Security check failed: Destination path '{}' is outside the root storage directory '{}'",
                           normalizedDestination, normalizedRoot);
                 throw new IOException("Cannot store file outside current directory.");
            }
            // Kiểm tra thêm để đảm bảo nó nằm TRỰC TIẾP trong thư mục gốc, không phải thư mục con sâu hơn
            // (Quan trọng để ngăn tạo thư mục con bằng tên file)
             if (!normalizedDestination.getParent().equals(normalizedRoot)) {
                  log.error("Security check failed: Destination path '{}' is not directly inside the root storage directory '{}'",
                            normalizedDestination, normalizedRoot);
                  throw new IOException("Cannot store file in a subdirectory via filename manipulation.");
             }
        } catch (InvalidPathException e) {
            // Bắt lỗi nếu tên file tạo ra đường dẫn không hợp lệ
            log.error("Invalid path generated for filename '{}': {}", uniqueFilename, e.getMessage());
            throw new IOException("Invalid path sequence for file " + originalFilename, e);
        }
        // === KẾT THÚC THAY THẾ ===

        // Thực hiện copy file
        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, destinationFile, StandardCopyOption.REPLACE_EXISTING);
            log.info("File stored successfully at: {}", destinationFile);
            return uniqueFilename; // Trả về tên file đã lưu
        } catch (IOException e) {
             log.error("Failed to store file '{}' due to IO error: {}", uniqueFilename, e.getMessage());
             throw new IOException("Failed to store file " + originalFilename, e);
        }
    }

    // Xóa file ảnh cũ (ví dụ)
    public void delete(String filename) throws IOException {
         if (!StringUtils.hasText(filename) || filename.contains("/") || filename.contains("..")) {
             // Simple security check
             return;
         }
        Path fileToDelete = rootLocation.resolve(filename).normalize().toAbsolutePath();
         if (Files.exists(fileToDelete) && fileToDelete.getParent().equals(this.rootLocation.toAbsolutePath())) {
            Files.delete(fileToDelete);
         }
    }

    // Các phương thức khác: load file, load all files...
}