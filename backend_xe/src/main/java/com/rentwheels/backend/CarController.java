package com.rentwheels.backend;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cars")
public class CarController {
    private static final Set<String> EDITABLE_FIELDS = Set.of(
            "name", "type", "brand", "price", "seats", "transmission", "fuel",
            "available", "year", "color", "description", "features", "quantity", "image_url");

    private final JdbcTemplate jdbc;

    public CarController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @GetMapping
    public List<Map<String, Object>> findAll() {
        return jdbc.queryForList("SELECT * FROM thong_tin_xe");
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> findOne(@PathVariable long id) {
        var cars = jdbc.queryForList("SELECT * FROM thong_tin_xe WHERE id = ?", id);
        if (cars.isEmpty()) {
            return ApiSupport.error(HttpStatus.NOT_FOUND, "Không tìm thấy xe yêu cầu");
        }
        return ResponseEntity.ok(cars.get(0));
    }

    @PutMapping("/{id}/book")
    public ResponseEntity<?> book(@PathVariable long id, @RequestBody(required = false) Map<String, Object> body) {
        if (!isAuthenticated(body)) {
            return ApiSupport.error(HttpStatus.UNAUTHORIZED, "Yêu cầu đăng nhập để thực hiện hành động này.");
        }
        int updated = jdbc.update(
                "UPDATE thong_tin_xe SET quantity = quantity - 1, available = IF(quantity - 1 > 0, 1, 0) "
                        + "WHERE id = ? AND quantity > 0",
                id);
        if (updated == 0) {
            if (jdbc.queryForList("SELECT id FROM thong_tin_xe WHERE id = ?", id).isEmpty()) {
                return ApiSupport.error(HttpStatus.NOT_FOUND, "Không tìm thấy xe yêu cầu");
            }
            return ApiSupport.error(HttpStatus.BAD_REQUEST, "Mẫu xe này hiện tại đã hết sạch xe trong kho!");
        }
        Map<String, Object> car = jdbc.queryForMap(
                "SELECT id, available, quantity FROM thong_tin_xe WHERE id = ?", id);
        return ResponseEntity.ok(ApiSupport.response(
                "success", true, "message", "Đặt xe thành công!", "data", car));
    }

    @PostMapping("/admin/add")
    public ResponseEntity<?> create(@RequestBody(required = false) Map<String, Object> body) {
        if (!isAdmin(body)) {
            return ApiSupport.error(HttpStatus.FORBIDDEN, "Từ chối truy cập! Bạn không phải Admin.");
        }
        if (body == null || body.isEmpty()) {
            return ApiSupport.error(HttpStatus.BAD_REQUEST, "Dữ liệu thêm xe không được để trống!");
        }
        Map<String, Object> car = new LinkedHashMap<>();
        car.put("name", body.get("name"));
        car.put("type", body.get("type"));
        car.put("brand", body.get("brand"));
        car.put("price", body.get("price"));
        car.put("seats", defaultValue(body.get("seats"), 5));
        car.put("transmission", defaultValue(body.get("transmission"), "Tự động"));
        car.put("fuel", defaultValue(body.get("fuel"), "Xăng"));
        car.put("available", parseAvailability(body.get("available")));
        car.put("year", defaultValue(body.get("year"), 2026));
        car.put("color", defaultValue(body.get("color"), ""));
        car.put("description", defaultValue(body.get("description"), ""));
        car.put("features", normalizeFeatures(body.get("features")));
        car.put("quantity", defaultValue(body.get("quantity"), 10));
        car.put("rating", 0.0);
        car.put("reviews", 0);
        car.put("image_url", body.get("image_url"));

        String columns = String.join(", ", car.keySet());
        String placeholders = String.join(", ", java.util.Collections.nCopies(car.size(), "?"));
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            var statement = connection.prepareStatement(
                    "INSERT INTO thong_tin_xe (" + columns + ") VALUES (" + placeholders + ")",
                    java.sql.Statement.RETURN_GENERATED_KEYS);
            int parameterIndex = 1;
            for (Object value : car.values()) {
                statement.setObject(parameterIndex++, value);
            }
            return statement;
        }, keyHolder);
        Map<String, Object> created = jdbc.queryForMap(
                "SELECT * FROM thong_tin_xe WHERE id = ?", keyHolder.getKey().longValue());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiSupport.response(
                "success", true, "message", "Thêm xe mới thành công!", "data", created));
    }

    @PutMapping("/admin/edit/{id}")
    public ResponseEntity<?> update(
            @PathVariable long id,
            @RequestBody(required = false) Map<String, Object> body) {
        if (!isAdmin(body)) {
            return ApiSupport.error(HttpStatus.FORBIDDEN, "Từ chối truy cập! Bạn không phải Admin.");
        }
        if (body == null || body.isEmpty()) {
            return ApiSupport.error(HttpStatus.BAD_REQUEST, "Dữ liệu cập nhật không được để trống!");
        }
        List<String> columns = new ArrayList<>();
        List<Object> values = new ArrayList<>();
        for (String field : EDITABLE_FIELDS) {
            if (body.containsKey(field)) {
                columns.add(field + " = ?");
                Object value = body.get(field);
                values.add(field.equals("features") ? normalizeFeatures(value)
                        : field.equals("available") ? parseAvailability(value) : value);
            }
        }
        if (columns.isEmpty()) {
            return ApiSupport.error(HttpStatus.BAD_REQUEST, "Không có trường xe hợp lệ để cập nhật.");
        }
        values.add(id);
        int updated = jdbc.update(
                "UPDATE thong_tin_xe SET " + String.join(", ", columns) + " WHERE id = ?",
                values.toArray());
        if (updated == 0) {
            return ApiSupport.error(HttpStatus.NOT_FOUND, "Không tìm thấy xe với ID: " + id);
        }
        Map<String, Object> car = jdbc.queryForMap("SELECT * FROM thong_tin_xe WHERE id = ?", id);
        return ResponseEntity.ok(ApiSupport.response(
                "success", true, "message", "Cập nhật thông tin xe thành công!", "data", car));
    }

    @DeleteMapping("/admin/delete/{id}")
    public ResponseEntity<?> delete(
            @PathVariable long id,
            @RequestBody(required = false) Map<String, Object> body) {
        if (!isAdmin(body)) {
            return ApiSupport.error(HttpStatus.FORBIDDEN, "Từ chối truy cập! Bạn không phải Admin.");
        }
        int deleted = jdbc.update("DELETE FROM thong_tin_xe WHERE id = ?", id);
        if (deleted == 0) {
            return ApiSupport.error(HttpStatus.NOT_FOUND, "Không tìm thấy xe với ID: " + id);
        }
        return ResponseEntity.ok(ApiSupport.response(
                "success", true, "message", "Chiếc xe đã được xóa thành công!"));
    }

    static boolean isAuthenticated(Map<String, Object> body) {
        return body != null && ApiSupport.longValue(
                body.get("activeUser") instanceof Map<?, ?> user ? user.get("id") : null) != null;
    }

    static boolean isAdmin(Map<String, Object> body) {
        if (body == null || !(body.get("activeUser") instanceof Map<?, ?> user)) {
            return false;
        }
        return "admin".equals(ApiSupport.string(user.get("role")));
    }

    private static Object defaultValue(Object value, Object fallback) {
        return value == null || String.valueOf(value).isBlank() ? fallback : value;
    }

    private static boolean parseAvailability(Object value) {
        if (value == null) {
            return true;
        }
        String normalized = String.valueOf(value).toLowerCase(java.util.Locale.ROOT);
        return !(normalized.equals("false") || normalized.equals("0"));
    }

    private static String normalizeFeatures(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof List<?> list) {
            return list.stream().map(String::valueOf).collect(java.util.stream.Collectors.joining(", "));
        }
        return String.valueOf(value);
    }
}
