package com.tiembanhngot.tiem_banh_online.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class StorageService {

    private final Path rootLocation;    

    public StorageService(@Value("${app.upload.dir}") String uploadDir) { // nhan file upload
        this.rootLocation = Paths.get(uploadDir);
        try {
            Files.createDirectories(rootLocation);
        } catch (IOException e) {
            throw new RuntimeException("Could not initialize storage location", e);
        }
    }

    public String store(MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new IOException("Failed to store empty file.");
        }

        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename());
        if (originalFilename.contains("..")) {
            throw new IOException("Cannot store file with relative path outside current directory " + originalFilename);
        }

        String fileExtension = StringUtils.getFilenameExtension(originalFilename);
        String uniqueFilename = UUID.randomUUID().toString() + "." + fileExtension;

        Path destinationFile = this.rootLocation.resolve(uniqueFilename);

        try {
            
            Path normalizedDestination = destinationFile.normalize().toAbsolutePath();
            Path normalizedRoot = this.rootLocation.normalize().toAbsolutePath();

            if (!normalizedDestination.startsWith(normalizedRoot)) {
                log.error("Security check failed: Destination path '{}' is outside the root storage directory '{}'",
                           normalizedDestination, normalizedRoot);
                throw new IOException("Cannot store file outside current directory.");
            }

            if (!normalizedDestination.getParent().equals(normalizedRoot)) {
                log.error("Security check failed: Destination path '{}' is not directly inside the root storage directory '{}'",
                            normalizedDestination, normalizedRoot);
                throw new IOException("Cannot store file in a subdirectory via filename manipulation.");
            }
        } catch (InvalidPathException e) {
            throw new IOException("Invalid path sequence for file " + originalFilename, e);
        }

        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, destinationFile, StandardCopyOption.REPLACE_EXISTING);
            log.info("File stored successfully at: {}", destinationFile);
            return uniqueFilename; // Trả về tên file đã lưu
        } catch (IOException e) {
            throw new IOException("Failed to store file " + originalFilename, e);
        }
    }

    public void delete(String filename) throws IOException {    // xoa file
        if (!StringUtils.hasText(filename) || filename.contains("/") || filename.contains("..")) {
            return;
        }
        Path fileToDelete = rootLocation.resolve(filename).normalize().toAbsolutePath();
        if (Files.exists(fileToDelete) && fileToDelete.getParent().equals(this.rootLocation.toAbsolutePath())) {
            Files.delete(fileToDelete);
        }
    }

}