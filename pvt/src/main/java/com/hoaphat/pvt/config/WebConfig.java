package com.hoaphat.pvt.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.concurrent.TimeUnit;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Value("${image.folder.path}")
    private String imageFolderPath;

    @Value("${video.folder.path}")
    private String videoFolderPath;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Cấu hình Cache cho Ảnh (Lưu cache 30 ngày)
        registry.addResourceHandler("/img-local/**")
                .addResourceLocations("file:///" + imageFolderPath + "/")
                .setCacheControl(CacheControl.maxAge(30, TimeUnit.DAYS).cachePublic());

        // Cấu hình Cache cho Video (Lưu cache 30 ngày)
        registry.addResourceHandler("/vid-local/**")
                .addResourceLocations("file:///" + videoFolderPath + "/")
                .setCacheControl(CacheControl.maxAge(30, TimeUnit.DAYS).cachePublic());
    }
}