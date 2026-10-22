package com.rentwheels.backend;

import java.nio.file.Path;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public class AppProperties {
    private String jwtSecret = "rentwheels_secret_key";
    private String avatarDirectory = "public/avatars";

    public String getJwtSecret() {
        return jwtSecret;
    }

    public void setJwtSecret(String jwtSecret) {
        this.jwtSecret = jwtSecret;
    }

    public Path getAvatarDirectory() {
        return Path.of(avatarDirectory);
    }

    public void setAvatarDirectory(String avatarDirectory) {
        this.avatarDirectory = avatarDirectory;
    }
}
