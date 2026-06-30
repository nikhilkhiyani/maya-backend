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
        return uploadFile(file, null);
    }

    public String uploadFile(MultipartFile file, String subfolder) {

        try {
            Path uploadPath = subfolder == null || subfolder.isBlank()
                    ? Paths.get(uploadDir)
                    : Paths.get(uploadDir, subfolder);

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

            String urlPath = subfolder == null || subfolder.isBlank()
                    ? "/uploads/" + filename
                    : "/uploads/" + subfolder + "/" + filename;
            return urlPath;

        } catch (IOException e) {
            throw new RuntimeException("Failed to upload file", e);
        }
    }
}
