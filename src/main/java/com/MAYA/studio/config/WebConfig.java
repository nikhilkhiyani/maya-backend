package com.MAYA.studio.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${file.upload-base}")
    private String uploadBase;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {

        Path basePath = Paths.get(uploadBase)
                .toAbsolutePath()
                .normalize();

        System.out.println("===================================");
        System.out.println("UPLOAD BASE: " + uploadBase);
        System.out.println("ABSOLUTE PATH: " + basePath);
        System.out.println("EXISTS: " + basePath.toFile().exists());
        System.out.println("===================================");

        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + basePath.toString() + "/");
    }
}
