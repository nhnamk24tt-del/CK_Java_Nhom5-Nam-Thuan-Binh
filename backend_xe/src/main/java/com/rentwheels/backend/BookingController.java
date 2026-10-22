package com.rentwheels.backend;

import java.sql.Date;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {
    private static final String BOOKING_LIST_SQL = """
            SELECT b.id, b.user_id, u.name AS user_name, u.email AS user_email,
                   u.phone AS user_phone, b.car_id, c.name AS car_name,
                   b.start_date, b.end_date, b.total_price, b.pickup_location,
                   b.status, b.created_at, b.customer_name, b.customer_phone, b.notes
            FROM booking_requests b
            LEFT JOIN users u ON b.user_id = u.id
            LEFT JOIN thong_tin_xe c ON b.car_id = c.id
            """;

    private final JdbcTemplate jdbc;

    public BookingController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @GetMapping
    public java.util.List<Map<String, Object>> findAll() {
        return jdbc.queryForList(BOOKING_LIST_SQL + " ORDER BY b.created_at DESC");
    }

    @GetMapping("/my-bookings")
    public ResponseEntity<?> findByUserId(
            @RequestParam(value = "user_id", required = false) Long queryUserId,
            @RequestBody(required = false) Map<String, Object> body) {
        Long userId = queryUserId != null ? queryUserId
                : ApiSupport.longValue(body == null ? null : body.get("user_id"));
        if (userId == null) {
            return ApiSupport.error(HttpStatus.BAD_REQUEST, "Thiếu user_id để truy xuất lịch sử thuê xe.");
        }
        return ResponseEntity.ok(jdbc.queryForList(
                BOOKING_LIST_SQL + " WHERE b.user_id = ? ORDER BY b.created_at DESC", userId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> findOne(@PathVariable long id) {
        var bookings = jdbc.queryForList("SELECT * FROM booking_requests WHERE id = ?", id);
        if (bookings.isEmpty()) {
            return ApiSupport.error(HttpStatus.NOT_FOUND, "Không tìm thấy yêu cầu");
        }
        return ResponseEntity.ok(bookings.get(0));
    }

    @PostMapping("/create")
    public ResponseEntity<?> create(@RequestBody(required = false) Map<String, Object> body) {
        if (!CarController.isAuthenticated(body)) {
            return ApiSupport.error(HttpStatus.UNAUTHORIZED, "Yêu cầu đăng nhập để thực hiện hành động này.");
        }
        if (body == null || body.isEmpty()) {
            return ApiSupport.error(HttpStatus.BAD_REQUEST, "Dữ liệu yêu cầu không được để trống!");
        }

        Long userId = ApiSupport.longValue(body.get("user_id"));
        Long carId = ApiSupport.longValue(body.get("car_id"));
        LocalDate startDate = parseDate(body.get("start_date"));
        LocalDate endDate = parseDate(body.get("end_date"));
        if (userId == null || carId == null || startDate == null || endDate == null || !endDate.isAfter(startDate)) {
            return ApiSupport.error(HttpStatus.BAD_REQUEST, "Thông tin người dùng, xe hoặc ngày thuê không hợp lệ.");
        }

        String pickupLocation = firstString(body, "pickup_location", "pickupLocation");
        String customerName = firstString(body, "customer_name", "name");
        String customerPhone = firstString(body, "customer_phone", "phone");
        Object totalPrice = body.getOrDefault("total_price", 0);
        String notes = ApiSupport.string(body.get("notes"));
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            var statement = connection.prepareStatement("""
                    INSERT INTO booking_requests
                    (user_id, car_id, start_date, end_date, total_price, pickup_location,
                     status, notes, customer_name, customer_phone, created_at)
                    VALUES (?, ?, ?, ?, ?, ?, 'pending', ?, ?, ?, NOW())
                    """, java.sql.Statement.RETURN_GENERATED_KEYS);
            statement.setLong(1, userId);
            statement.setLong(2, carId);
            statement.setDate(3, Date.valueOf(startDate));
            statement.setDate(4, Date.valueOf(endDate));
            statement.setObject(5, totalPrice);
            statement.setString(6, pickupLocation == null ? "" : pickupLocation);
            statement.setString(7, notes == null ? "" : notes);
            statement.setString(8, customerName == null ? "" : customerName);
            statement.setString(9, customerPhone == null ? "" : customerPhone);
            return statement;
        }, keyHolder);

        Map<String, Object> booking = new LinkedHashMap<>();
        booking.put("id", keyHolder.getKey().longValue());
        booking.put("user_id", userId);
        booking.put("car_id", carId);
        booking.put("start_date", startDate.toString());
        booking.put("end_date", endDate.toString());
        booking.put("total_price", totalPrice);
        booking.put("pickup_location", pickupLocation == null ? "" : pickupLocation);
        booking.put("status", "pending");
        booking.put("customer_name", customerName == null ? "" : customerName);
        booking.put("customer_phone", customerPhone == null ? "" : customerPhone);
        booking.put("notes", notes == null ? "" : notes);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiSupport.response(
                "success", true, "message", "Tạo yêu cầu thành công!", "booking", booking, "data", booking));
    }

    @PutMapping("/{id}/approve")
    @Transactional
    public ResponseEntity<?> approve(
            @PathVariable long id,
            @RequestBody(required = false) Map<String, Object> body) {
        if (!CarController.isAdmin(body)) {
            return ApiSupport.error(HttpStatus.FORBIDDEN, "Từ chối truy cập! Bạn không phải Admin.");
        }
        var bookings = jdbc.queryForList(
                "SELECT id, car_id, status FROM booking_requests WHERE id = ? FOR UPDATE", id);
        if (bookings.isEmpty()) {
            return ApiSupport.error(HttpStatus.NOT_FOUND, "Không tìm thấy yêu cầu với ID: " + id);
        }
        if (!"pending".equalsIgnoreCase(ApiSupport.string(bookings.get(0).get("status")))) {
            return ApiSupport.error(HttpStatus.CONFLICT, "Yêu cầu này đã được xử lý.");
        }
        Long carId = ApiSupport.longValue(bookings.get(0).get("car_id"));
        if (carId == null) {
            return ApiSupport.error(HttpStatus.BAD_REQUEST, "Dữ liệu yêu cầu không hợp lệ.");
        }
        int updated = jdbc.update(
                "UPDATE thong_tin_xe SET quantity = quantity - 1, available = IF(quantity - 1 > 0, 1, 0) "
                        + "WHERE id = ? AND quantity > 0",
                carId);
        if (updated == 0) {
            return ApiSupport.error(HttpStatus.BAD_REQUEST, "Xe không còn sẵn trong kho để duyệt yêu cầu.");
        }
        jdbc.update("UPDATE booking_requests SET status = 'approved' WHERE id = ?", id);
        Map<String, Object> booking = Map.of("id", id, "status", "approved");
        Map<String, Object> car = jdbc.queryForMap(
                "SELECT id, available, quantity FROM thong_tin_xe WHERE id = ?", carId);
        return ResponseEntity.ok(ApiSupport.response(
                "success", true,
                "message", "Duyệt yêu cầu thành công!",
                "data", ApiSupport.response("booking", booking, "car", car)));
    }

    @PutMapping("/{id}/reject")
    @Transactional
    public ResponseEntity<?> reject(
            @PathVariable long id,
            @RequestBody(required = false) Map<String, Object> body) {
        if (!CarController.isAdmin(body)) {
            return ApiSupport.error(HttpStatus.FORBIDDEN, "Từ chối truy cập! Bạn không phải Admin.");
        }
        var bookings = jdbc.queryForList(
                "SELECT status FROM booking_requests WHERE id = ? FOR UPDATE", id);
        if (bookings.isEmpty()) {
            return ApiSupport.error(HttpStatus.NOT_FOUND, "Không tìm thấy yêu cầu với ID: " + id);
        }
        if (!"pending".equalsIgnoreCase(ApiSupport.string(bookings.get(0).get("status")))) {
            return ApiSupport.error(HttpStatus.CONFLICT, "Yêu cầu này đã được xử lý.");
        }
        jdbc.update("UPDATE booking_requests SET status = 'rejected' WHERE id = ?", id);
        return ResponseEntity.ok(ApiSupport.response(
                "success", true, "message", "Từ chối yêu cầu thành công!",
                "data", ApiSupport.response("id", id, "status", "rejected")));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(
            @PathVariable long id,
            @RequestBody(required = false) Map<String, Object> body) {
        if (!CarController.isAdmin(body)) {
            return ApiSupport.error(HttpStatus.FORBIDDEN, "Từ chối truy cập! Bạn không phải Admin.");
        }
        int deleted = jdbc.update("DELETE FROM booking_requests WHERE id = ?", id);
        if (deleted == 0) {
            return ApiSupport.error(HttpStatus.NOT_FOUND, "Không tìm thấy yêu cầu với ID: " + id);
        }
        return ResponseEntity.ok(ApiSupport.response("success", true, "message", "Yêu cầu đã được xóa!"));
    }

    private static LocalDate parseDate(Object value) {
        String text = ApiSupport.string(value);
        if (text == null || text.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(text.substring(0, Math.min(text.length(), 10)));
        } catch (java.time.DateTimeException exception) {
            return null;
        }
    }

    private static String firstString(Map<String, Object> body, String... keys) {
        for (String key : keys) {
            String value = ApiSupport.string(body.get(key));
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }
}
