package com.MAYA.studio.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.UUID;

@Service
public class FileStorageService {

    @Value("${file.upload-base}")
    private String uploadDir;

    public String uploadFile(MultipartFile file) {

        try {
            Path uploadPath = Paths.get(uploadDir);

            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            String originalFilename = StringUtils.cleanPath(
                    file.getOriginalFilename()
            );

            String extension = "";

            if (originalFilename.contains(".")) {
                extension = originalFilename.substring(
                        originalFilename.lastIndexOf(".")
                );
            }

            String filename = UUID.randomUUID() + extension;

            Path filePath = uploadPath.resolve(filename);

            Files.copy(
                    file.getInputStream(),
                    filePath,
                    StandardCopyOption.REPLACE_EXISTING
            );

            return "/static/uploads/" + filename;

        } catch (IOException e) {
            throw new RuntimeException("Failed to upload file", e);
        }
    }
}
