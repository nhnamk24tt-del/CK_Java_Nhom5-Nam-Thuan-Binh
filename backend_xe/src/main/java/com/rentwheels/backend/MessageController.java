package com.rentwheels.backend;

import java.util.Map;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/messages")
public class MessageController {
    private final JdbcTemplate jdbc;

    public MessageController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @PostMapping("/create")
    public ResponseEntity<?> create(@RequestBody(required = false) Map<String, Object> body) {
        Map<String, Object> input = body == null ? Map.of() : body;
        String name = ApiSupport.string(input.get("name"));
        String email = ApiSupport.string(input.get("email"));
        String subject = ApiSupport.string(input.get("subject"));
        String message = ApiSupport.string(input.get("message"));
        if (isBlank(name) || isBlank(email) || isBlank(subject) || isBlank(message)) {
            return ApiSupport.error(HttpStatus.BAD_REQUEST, "Vui lòng cung cấp đầy đủ tên, email, chủ đề và nội dung.");
        }

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            var statement = connection.prepareStatement("""
                    INSERT INTO messages (user_id, sender_name, sender_email, sender_phone, subject, message)
                    VALUES (?, ?, ?, ?, ?, ?)
                    """, java.sql.Statement.RETURN_GENERATED_KEYS);
            statement.setObject(1, ApiSupport.longValue(input.get("user_id")));
            statement.setString(2, name);
            statement.setString(3, email);
            statement.setString(4, ApiSupport.string(input.get("phone")));
            statement.setString(5, subject);
            statement.setString(6, message);
            return statement;
        }, keyHolder);
        Map<String, Object> created = jdbc.queryForMap(
                "SELECT * FROM messages WHERE id = ?", keyHolder.getKey().longValue());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiSupport.response(
                "success", true, "message", "Tin nhắn của bạn đã được gửi.", "data", created));
    }

    @PostMapping("/list")
    public ResponseEntity<?> findAll(@RequestBody(required = false) Map<String, Object> body) {
        if (!CarController.isAdmin(body)) {
            return ApiSupport.error(HttpStatus.FORBIDDEN, "Từ chối truy cập! Bạn không phải Admin.");
        }
        return ResponseEntity.ok(ApiSupport.response(
                "success", true, "data", jdbc.queryForList("SELECT * FROM messages ORDER BY created_at DESC")));
    }

    @GetMapping("/my")
    public ResponseEntity<?> findByEmail(@RequestParam(required = false) String email) {
        if (isBlank(email)) {
            return ApiSupport.error(HttpStatus.BAD_REQUEST, "Vui lòng cung cấp email để truy vấn tin nhắn.");
        }
        return ResponseEntity.ok(ApiSupport.response(
                "success", true, "data",
                jdbc.queryForList("SELECT * FROM messages WHERE sender_email = ? ORDER BY created_at DESC", email.trim())));
    }

    @PostMapping("/{id}")
    public ResponseEntity<?> findOne(
            @PathVariable long id,
            @RequestBody(required = false) Map<String, Object> body) {
        if (!CarController.isAdmin(body)) {
            return ApiSupport.error(HttpStatus.FORBIDDEN, "Từ chối truy cập! Bạn không phải Admin.");
        }
        var messages = jdbc.queryForList("SELECT * FROM messages WHERE id = ? LIMIT 1", id);
        if (messages.isEmpty()) {
            return ApiSupport.error(HttpStatus.NOT_FOUND, "Không tìm thấy tin nhắn.");
        }
        return ResponseEntity.ok(ApiSupport.response("success", true, "data", messages.get(0)));
    }

    @PutMapping("/{id}/reply")
    public ResponseEntity<?> reply(
            @PathVariable long id,
            @RequestBody(required = false) Map<String, Object> body) {
        if (!CarController.isAdmin(body)) {
            return ApiSupport.error(HttpStatus.FORBIDDEN, "Từ chối truy cập! Bạn không phải Admin.");
        }
        Map<String, Object> input = body == null ? Map.of() : body;
        String reply = firstString(input, "response_text", "reply");
        if (isBlank(reply)) {
            return ApiSupport.error(HttpStatus.BAD_REQUEST, "Vui lòng cung cấp nội dung phản hồi.");
        }
        int updated = jdbc.update(
                "UPDATE messages SET admin_reply = ?, status = 'replied', admin_reply_at = NOW() WHERE id = ?",
                reply, id);
        if (updated == 0) {
            return ApiSupport.error(HttpStatus.NOT_FOUND, "Không tìm thấy tin nhắn để phản hồi.");
        }
        return ResponseEntity.ok(ApiSupport.response(
                "success", true, "message", "Phản hồi đã được lưu.", "data", getById(id)));
    }

    @PutMapping("/{id}/mark-read")
    public ResponseEntity<?> markRead(
            @PathVariable long id,
            @RequestBody(required = false) Map<String, Object> body) {
        if (!CarController.isAdmin(body)) {
            return ApiSupport.error(HttpStatus.FORBIDDEN, "Từ chối truy cập! Bạn không phải Admin.");
        }
        int updated = jdbc.update("UPDATE messages SET status = 'read' WHERE id = ?", id);
        if (updated == 0) {
            return ApiSupport.error(HttpStatus.NOT_FOUND, "Không tìm thấy tin nhắn.");
        }
        return ResponseEntity.ok(ApiSupport.response(
                "success", true, "message", "Tin nhắn đã được đánh dấu đã đọc.", "data", getById(id)));
    }

    private Map<String, Object> getById(long id) {
        return jdbc.queryForMap("SELECT * FROM messages WHERE id = ? LIMIT 1", id);
    }

    private static String firstString(Map<String, Object> body, String... fields) {
        for (String field : fields) {
            String value = ApiSupport.string(body.get(field));
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
