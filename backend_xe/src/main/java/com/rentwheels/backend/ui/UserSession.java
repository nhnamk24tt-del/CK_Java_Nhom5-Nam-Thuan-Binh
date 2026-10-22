package com.rentwheels.backend.ui;

import com.vaadin.flow.spring.annotation.VaadinSessionScope;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
@VaadinSessionScope
public class UserSession {
    private Map<String, Object> user;
    private String token;

    public boolean isAuthenticated() {
        return user != null && user.get("id") != null && token != null;
    }

    public boolean isAdmin() {
        return isAuthenticated()
                && "admin".equalsIgnoreCase(String.valueOf(user.getOrDefault("role", "")).trim());
    }

    public Map<String, Object> getUser() {
        return user == null ? Map.of() : Collections.unmodifiableMap(user);
    }

    public String getToken() {
        return token;
    }

    public void signIn(Map<String, Object> user, String token) {
        this.user = new LinkedHashMap<>(user);
        this.token = token;
    }

    public void signOut() {
        user = null;
        token = null;
    }

    public Long getUserId() {
        Object value = user == null ? null : user.get("id");
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return value == null ? null : Long.valueOf(String.valueOf(value));
        } catch (NumberFormatException exception) {
            return null;
        }
    }
}
