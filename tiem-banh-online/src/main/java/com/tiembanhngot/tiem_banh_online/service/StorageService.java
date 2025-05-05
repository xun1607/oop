package com.tiembanhngot.tiem_banh_online.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.UUID;

@Service
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
        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename());
        if (originalFilename.contains("..")) {
            // Security check
            throw new IOException("Cannot store file with relative path outside current directory " + originalFilename);
        }

        // Tạo tên file duy nhất
        String fileExtension = StringUtils.getFilenameExtension(originalFilename);
        String uniqueFilename = UUID.randomUUID().toString() + "." + fileExtension;
        Path destinationFile = this.rootLocation.resolve(uniqueFilename).normalize().toAbsolutePath();

        if (!destinationFile.getParent().equals(this.rootLocation.toAbsolutePath())) {
            // Security check
             throw new IOException("Cannot store file outside current directory.");
        }

        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, destinationFile, StandardCopyOption.REPLACE_EXISTING);
            return uniqueFilename; // Trả về tên file đã lưu
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