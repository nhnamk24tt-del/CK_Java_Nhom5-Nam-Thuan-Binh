package com.rentwheels.backend;

import java.nio.file.Path;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    private final AppProperties properties;
    private final Path assetDirectory;

    public WebConfig(
            AppProperties properties,
            @Value("${app.assets-directory:../baitap1/public/assets}") String assetDirectory) {
        this.properties = properties;
        this.assetDirectory = Path.of(assetDirectory).toAbsolutePath().normalize();
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path avatarDirectory = properties.getAvatarDirectory().toAbsolutePath().normalize();
        registry.addResourceHandler("/avatars/**")
                .addResourceLocations(avatarDirectory.toUri().toString());
        registry.addResourceHandler("/assets/**")
                .addResourceLocations(assetDirectory.toUri().toString());
    }
}
