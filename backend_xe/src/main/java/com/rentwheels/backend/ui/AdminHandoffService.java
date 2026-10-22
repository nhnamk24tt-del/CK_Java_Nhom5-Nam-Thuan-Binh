package com.rentwheels.backend.ui;

import com.rentwheels.backend.TokenService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class AdminHandoffService {
    private static final SecureRandom RANDOM = new SecureRandom();
    private final JdbcTemplate jdbc;
    private final TokenService tokenService;

    public AdminHandoffService(JdbcTemplate jdbc, TokenService tokenService) {
        this.jdbc = jdbc;
        this.tokenService = tokenService;
        jdbc.execute("""
                CREATE TABLE IF NOT EXISTS rentwheels_admin_handoffs (
                    token_hash CHAR(64) PRIMARY KEY,
                    user_id BIGINT NOT NULL,
                    expires_at DATETIME(3) NOT NULL,
                    consumed_at DATETIME(3) NULL,
                    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
                    INDEX idx_rentwheels_admin_handoffs_expiry (expires_at)
                )
                """);
    }

    public String create(UserSession session) {
        if (!session.isAdmin()) {
            throw new IllegalStateException("Chỉ tài khoản quản trị mới có thể mở giao diện admin.");
        }
        jdbc.update("DELETE FROM rentwheels_admin_handoffs WHERE expires_at <= NOW(3)");
        byte[] randomBytes = new byte[32];
        RANDOM.nextBytes(randomBytes);
        String ticket = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
        jdbc.update("""
                INSERT INTO rentwheels_admin_handoffs (token_hash, user_id, expires_at)
                VALUES (?, ?, DATE_ADD(NOW(3), INTERVAL 60 SECOND))
                """, hash(ticket), session.getUserId());
        return ticket;
    }

    public AdminIdentity consume(String ticket) {
        if (ticket == null || !ticket.matches("[A-Za-z0-9_-]{43}")) {
            throw new IllegalArgumentException("Liên kết đăng nhập admin không hợp lệ hoặc đã hết hạn.");
        }
        int consumed = jdbc.update("""
                UPDATE rentwheels_admin_handoffs
                SET consumed_at = NOW(3)
                WHERE token_hash = ? AND consumed_at IS NULL AND expires_at > NOW(3)
                """, hash(ticket));
        if (consumed != 1) {
            throw new IllegalArgumentException("Liên kết đăng nhập admin không hợp lệ hoặc đã hết hạn.");
        }

        var users = jdbc.queryForList("""
                SELECT id, name, email, phone, address, gender, birthdate,
                       drivers_license, avatar, role
                FROM users
                WHERE id = ? AND role = 'admin'
                LIMIT 1
                """, jdbc.queryForObject(
                "SELECT user_id FROM rentwheels_admin_handoffs WHERE token_hash = ?",
                Long.class, hash(ticket)));
        if (users.isEmpty()) {
            throw new IllegalStateException("Tài khoản quản trị không còn hợp lệ.");
        }
        Map<String, Object> user = new LinkedHashMap<>(users.get(0));
        return new AdminIdentity(user, tokenService.createToken(user));
    }

    public record AdminIdentity(Map<String, Object> user, String accessToken) {
    }

    private static String hash(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("Không thể tạo mã đăng nhập an toàn.", exception);
        }
    }
}
