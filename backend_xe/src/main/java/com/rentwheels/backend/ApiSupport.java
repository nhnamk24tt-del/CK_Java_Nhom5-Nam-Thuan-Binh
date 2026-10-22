package com.rentwheels.backend;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public final class ApiSupport {
    private ApiSupport() {
    }

    public static Map<String, Object> response(Object... entries) {
        Map<String, Object> body = new LinkedHashMap<>();
        for (int index = 0; index < entries.length; index += 2) {
            body.put((String) entries[index], entries[index + 1]);
        }
        return body;
    }

    public static ResponseEntity<Map<String, Object>> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(response("success", false, "message", message));
    }

    public static String string(Object value) {
        return value == null ? null : String.valueOf(value).trim();
    }

    public static Long longValue(Object value) {
        if (value == null || string(value).isEmpty()) {
            return null;
        }
        try {
            return Long.valueOf(string(value));
        } catch (NumberFormatException exception) {
            return null;
        }
    }
}
