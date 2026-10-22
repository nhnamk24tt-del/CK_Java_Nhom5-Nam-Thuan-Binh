package com.rentwheels.backend;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private static final String USER_FIELDS =
            "id, name, email, phone, address, gender, birthdate, drivers_license, avatar, role";

    private final JdbcTemplate jdbc;
    private final TokenService tokens;
    private final AppProperties properties;

    public AuthController(JdbcTemplate jdbc, TokenService tokens, AppProperties properties) {
        this.jdbc = jdbc;
        this.tokens = tokens;
        this.properties = properties;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody(required = false) Map<String, Object> body) {
        Map<String, Object> input = body == null ? Map.of() : body;
        String name = firstString(input, "fullName", "name");
        String email = normalizeEmail(input.get("email"));
        String password = ApiSupport.string(input.get("password"));
        if (name == null || name.isBlank() || email == null || email.isBlank()
                || password == null || password.isBlank()) {
            return ApiSupport.error(HttpStatus.BAD_REQUEST, "Vui lòng nhập đầy đủ thông tin.");
        }

        if (!jdbc.queryForList("SELECT id FROM users WHERE email = ? LIMIT 1", email).isEmpty()) {
            return ApiSupport.error(HttpStatus.CONFLICT, "Email đã tồn tại.");
        }

        try {
            jdbc.update("INSERT INTO users (name, email, password, phone, role, created_at) "
                            + "VALUES (?, ?, ?, ?, 'user', NOW())",
                    name, email, tokens.hashPassword(password), ApiSupport.string(input.get("phone")));
        } catch (DuplicateKeyException exception) {
            return ApiSupport.error(HttpStatus.CONFLICT, "Email đã tồn tại.");
        }

        Map<String, Object> user = jdbc.queryForMap(
                "SELECT id, name, email, phone, avatar, role FROM users WHERE email = ? LIMIT 1", email);
        return ResponseEntity.ok(ApiSupport.response(
                "success", true,
                "message", "Đăng ký thành công.",
                "accessToken", tokens.createToken(user),
                "user", user));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody(required = false) Map<String, Object> body) {
        Map<String, Object> input = body == null ? Map.of() : body;
        String email = normalizeEmail(input.get("email"));
        String password = ApiSupport.string(input.get("password"));
        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            return ApiSupport.error(HttpStatus.BAD_REQUEST, "Vui lòng nhập email và mật khẩu.");
        }
        return loginWithRole(email, password, false);
    }

    @PostMapping("/login-admin")
    public ResponseEntity<?> loginAdmin(@RequestBody(required = false) Map<String, Object> body) {
        Map<String, Object> input = body == null ? Map.of() : body;
        String email = normalizeEmail(input.get("email"));
        String password = ApiSupport.string(input.get("password"));
        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            return ApiSupport.error(HttpStatus.BAD_REQUEST, "Vui lòng nhập email và mật khẩu");
        }
        return loginWithRole(email, password, true);
    }

    @PostMapping("/logout")
    public Map<String, Object> logout() {
        return ApiSupport.response("success", true, "message", "Đăng xuất thành công.");
    }

    @GetMapping("/me")
    public ResponseEntity<?> currentUser(@RequestHeader(value = "Authorization", required = false) String authorization) {
        Map<String, Object> payload = verifyAuthorization(authorization);
        if (payload == null) {
            return ApiSupport.error(HttpStatus.UNAUTHORIZED, "Token không hợp lệ.");
        }
        Long userId = ApiSupport.longValue(payload.get("id"));
        var users = jdbc.queryForList("SELECT " + USER_FIELDS + " FROM users WHERE id = ? LIMIT 1", userId);
        if (users.isEmpty()) {
            return ApiSupport.error(HttpStatus.NOT_FOUND, "Không tìm thấy người dùng.");
        }
        return ResponseEntity.ok(users.get(0));
    }

    @PutMapping("/update-profile")
    public ResponseEntity<?> updateProfile(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody(required = false) Map<String, Object> body) {
        Map<String, Object> payload = verifyAuthorization(authorization);
        if (payload == null) {
            return ApiSupport.error(HttpStatus.UNAUTHORIZED, "Token không hợp lệ.");
        }
        Long userId = ApiSupport.longValue(payload.get("id"));
        Map<String, Object> input = body == null ? Map.of() : body;
        String avatar = ApiSupport.string(input.get("avatar"));
        if (avatar != null && avatar.startsWith("data:")) {
            try {
                avatar = saveAvatar(avatar, userId);
            } catch (IllegalArgumentException | IOException exception) {
                return ApiSupport.error(HttpStatus.BAD_REQUEST, "Không thể lưu ảnh đại diện.");
            }
        }

        jdbc.update("""
                UPDATE users
                SET name = COALESCE(?, name),
                    phone = COALESCE(?, phone),
                    address = COALESCE(?, address),
                    gender = COALESCE(?, gender),
                    birthdate = COALESCE(?, birthdate),
                    drivers_license = COALESCE(?, drivers_license),
                    avatar = COALESCE(?, avatar)
                WHERE id = ?
                """,
                input.get("name"), input.get("phone"), input.get("address"), input.get("gender"),
                input.get("birthdate"), input.get("drivers_license"), avatar, userId);

        var users = jdbc.queryForList("SELECT " + USER_FIELDS + " FROM users WHERE id = ? LIMIT 1", userId);
        if (users.isEmpty()) {
            return ApiSupport.error(HttpStatus.NOT_FOUND, "Không tìm thấy người dùng.");
        }
        return ResponseEntity.ok(ApiSupport.response(
                "success", true,
                "message", "Cập nhật thông tin thành công.",
                "user", users.get(0)));
    }

    private ResponseEntity<?> loginWithRole(String email, String password, boolean adminOnly) {
        String query = "SELECT id, name, email, phone, password, avatar, role FROM users WHERE email = ?"
                + (adminOnly ? " AND role = 'admin'" : "") + " LIMIT 1";
        var users = jdbc.queryForList(query, email);
        if (users.isEmpty()) {
            String message = adminOnly ? "Tài khoản admin không tồn tại" : "Sai email hoặc mật khẩu.";
            return ApiSupport.error(HttpStatus.UNAUTHORIZED, message);
        }

        Map<String, Object> record = users.get(0);
        if (!java.security.MessageDigest.isEqual(
                tokens.hashPassword(password).getBytes(java.nio.charset.StandardCharsets.US_ASCII),
                String.valueOf(record.get("password")).getBytes(java.nio.charset.StandardCharsets.US_ASCII))) {
            return ApiSupport.error(HttpStatus.UNAUTHORIZED, adminOnly ? "Sai mật khẩu admin" : "Sai email hoặc mật khẩu.");
        }

        record.remove("password");
        return ResponseEntity.ok(ApiSupport.response(
                "success", true,
                "message", adminOnly ? "Đăng nhập admin thành công" : "Đăng nhập thành công.",
                "accessToken", tokens.createToken(record),
                "user", record));
    }

    private Map<String, Object> verifyAuthorization(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return null;
        }
        return tokens.verify(authorization.substring(7));
    }

    private String saveAvatar(String dataUrl, Long userId) throws IOException {
        var matcher = java.util.regex.Pattern
                .compile("^data:image/(png|jpeg|jpg|gif);base64,([A-Za-z0-9+/=]+)$")
                .matcher(dataUrl);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Dữ liệu ảnh không hợp lệ.");
        }
        String extension = matcher.group(1).equals("jpeg") ? "jpg" : matcher.group(1);
        byte[] bytes = Base64.getDecoder().decode(matcher.group(2));
        if (bytes.length > 5 * 1024 * 1024) {
            throw new IllegalArgumentException("Ảnh vượt quá dung lượng cho phép.");
        }
        Path directory = properties.getAvatarDirectory().toAbsolutePath().normalize();
        Files.createDirectories(directory);
        String filename = "avatar_" + userId + "_" + UUID.randomUUID() + "." + extension;
        Files.write(directory.resolve(filename), bytes);
        return "/avatars/" + filename;
    }

    private static String normalizeEmail(Object value) {
        String email = ApiSupport.string(value);
        return email == null ? null : email.toLowerCase(java.util.Locale.ROOT);
    }

    private static String firstString(Map<String, Object> body, String... keys) {
        for (String key : keys) {
            String value = ApiSupport.string(body.get(key));
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }
}
