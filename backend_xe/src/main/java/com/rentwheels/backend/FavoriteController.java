package com.rentwheels.backend;

import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/favorites")
public class FavoriteController {
    private final JdbcTemplate jdbc;
    private final TokenService tokens;

    public FavoriteController(JdbcTemplate jdbc, TokenService tokens) {
        this.jdbc = jdbc;
        this.tokens = tokens;
    }

    @GetMapping
    public ResponseEntity<?> getFavorites(
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        Map<String, Object> user = verifyAuthorization(authorization);
        if (user == null) {
            return ApiSupport.error(HttpStatus.UNAUTHORIZED, "Token không hợp lệ.");
        }
        return ResponseEntity.ok(jdbc.queryForList(
                "SELECT car_id FROM favorites WHERE user_id = ? ORDER BY created_at DESC",
                ApiSupport.longValue(user.get("id"))).stream()
                .map(row -> row.get("car_id"))
                .toList());
    }

    @PostMapping
    public ResponseEntity<?> addFavorite(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody(required = false) Map<String, Object> body) {
        Map<String, Object> user = verifyAuthorization(authorization);
        if (user == null) {
            return ApiSupport.error(HttpStatus.UNAUTHORIZED, "Token không hợp lệ.");
        }
        Long carId = ApiSupport.longValue(body == null ? null : body.get("car_id"));
        if (carId == null) {
            return ApiSupport.error(HttpStatus.BAD_REQUEST, "Thiếu car_id để thêm yêu thích.");
        }
        jdbc.update("INSERT IGNORE INTO favorites (user_id, car_id, created_at) VALUES (?, ?, NOW())",
                ApiSupport.longValue(user.get("id")), carId);
        return ResponseEntity.ok(ApiSupport.response("success", true, "message", "Thêm vào yêu thích thành công."));
    }

    @DeleteMapping("/{carId}")
    public ResponseEntity<?> removeFavorite(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable long carId) {
        Map<String, Object> user = verifyAuthorization(authorization);
        if (user == null) {
            return ApiSupport.error(HttpStatus.UNAUTHORIZED, "Token không hợp lệ.");
        }
        jdbc.update("DELETE FROM favorites WHERE user_id = ? AND car_id = ?",
                ApiSupport.longValue(user.get("id")), carId);
        return ResponseEntity.ok(ApiSupport.response("success", true, "message", "Xoá khỏi yêu thích thành công."));
    }

    private Map<String, Object> verifyAuthorization(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return null;
        }
        return tokens.verify(authorization.substring(7));
    }
}
