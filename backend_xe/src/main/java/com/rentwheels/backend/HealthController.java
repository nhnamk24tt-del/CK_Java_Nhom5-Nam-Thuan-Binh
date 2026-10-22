package com.rentwheels.backend;

import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {
    private final String instanceRole;

    public HealthController(@Value("${app.instance-role:user}") String instanceRole) {
        this.instanceRole = instanceRole;
    }

    @GetMapping("/api/health")
    public Map<String, String> health() {
        return Map.of("status", "UP", "application", "rentwheels-java", "instanceRole", instanceRole);
    }
}
