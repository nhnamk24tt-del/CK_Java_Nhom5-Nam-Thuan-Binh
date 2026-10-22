package com.rentwheels.backend;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Service;

@Service
public class TokenService {
    private static final long TOKEN_LIFETIME_MILLIS = 7L * 24 * 60 * 60 * 1000;

    private final ObjectMapper objectMapper;
    private final String secret;

    public TokenService(ObjectMapper objectMapper, AppProperties properties) {
        this.objectMapper = objectMapper;
        this.secret = properties.getJwtSecret();
    }

    public String hashPassword(String password) {
        return hmac(password);
    }

    public String createToken(Map<String, ?> user) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("id", user.get("id"));
        payload.put("email", user.get("email"));
        payload.put("role", user.get("role"));
        payload.put("exp", System.currentTimeMillis() + TOKEN_LIFETIME_MILLIS);
        try {
            String encodedPayload = Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(objectMapper.writeValueAsBytes(payload));
            return encodedPayload + "." + Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(hmacBytes(encodedPayload));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Không thể tạo access token.", exception);
        }
    }

    public Map<String, Object> verify(String token) {
        if (token == null) {
            return null;
        }
        String[] parts = token.split("\\.", -1);
        if (parts.length != 2) {
            return null;
        }
        try {
            byte[] suppliedSignature = Base64.getUrlDecoder().decode(parts[1]);
            if (!MessageDigest.isEqual(suppliedSignature, hmacBytes(parts[0]))) {
                return null;
            }
            byte[] payloadBytes = Base64.getUrlDecoder().decode(parts[0]);
            Map<String, Object> payload = objectMapper.readValue(payloadBytes,
                    objectMapper.getTypeFactory().constructMapType(Map.class, String.class, Object.class));
            Object expiryValue = payload.get("exp");
            if (!(expiryValue instanceof Number expiry) || expiry.longValue() < System.currentTimeMillis()) {
                return null;
            }
            return payload;
        } catch (IllegalArgumentException | IOException exception) {
            return null;
        }
    }

    private String hmac(String value) {
        return java.util.HexFormat.of().formatHex(hmacBytes(value));
    }

    private byte[] hmacBytes(String value) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return mac.doFinal(value.getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Không thể khởi tạo bộ ký token.", exception);
        }
    }
}
