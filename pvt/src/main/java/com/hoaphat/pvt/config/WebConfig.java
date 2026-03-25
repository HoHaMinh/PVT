package com.hoaphat.pvt.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Value("${image.folder.path}")
    private String imageFolderPath;

    @Value("${video.folder.path}")
    private String videoFolderPath;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/img-local/**")
                .addResourceLocations("file:///" + imageFolderPath + "/");
        registry.addResourceHandler("/vid-local/**")
                .addResourceLocations("file:///" + videoFolderPath + "/");
    }
}
