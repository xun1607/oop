package com.tiembanhngot.tiem_banh_online.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class MvcConfig implements WebMvcConfigurer {

    @Value("${app.upload.dir}") // Lấy đường dẫn từ properties
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path uploadPath = Paths.get(uploadDir);
        String absoluteUploadPath = uploadPath.toFile().getAbsolutePath();

        // Map URL /uploads/** tới thư mục vật lý trên server
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:/" + absoluteUploadPath + "/");

    }
}