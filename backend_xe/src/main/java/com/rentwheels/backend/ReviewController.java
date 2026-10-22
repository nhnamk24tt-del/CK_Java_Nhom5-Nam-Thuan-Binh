package com.rentwheels.backend;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reviews")
public class ReviewController {
    private final JdbcTemplate jdbc;

    public ReviewController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @GetMapping("/car/{carId}")
    public java.util.List<Map<String, Object>> findByCarId(@PathVariable long carId) {
        return jdbc.queryForList("SELECT * FROM reviews WHERE car_id = ? ORDER BY created_at DESC", carId);
    }

    @PostMapping
    @Transactional
    public ResponseEntity<?> create(@RequestBody(required = false) Map<String, Object> body) {
        Map<String, Object> input = body == null ? Map.of() : body;
        Long carId = ApiSupport.longValue(input.get("car_id"));
        Long rating = ApiSupport.longValue(input.get("rating"));
        if (carId == null || rating == null) {
            return ApiSupport.error(HttpStatus.BAD_REQUEST, "Thiếu car_id hoặc rating.");
        }
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            var statement = connection.prepareStatement("""
                    INSERT INTO reviews (car_id, user_id, user_name, user_email, rating, comment)
                    VALUES (?, ?, ?, ?, ?, ?)
                    """, java.sql.Statement.RETURN_GENERATED_KEYS);
            statement.setLong(1, carId);
            statement.setObject(2, ApiSupport.longValue(input.get("user_id")));
            statement.setString(3, ApiSupport.string(input.get("user_name")));
            statement.setString(4, ApiSupport.string(input.get("user_email")));
            statement.setLong(5, rating);
            statement.setString(6, ApiSupport.string(input.get("comment")));
            return statement;
        }, keyHolder);

        Map<String, Object> review = jdbc.queryForMap(
                "SELECT * FROM reviews WHERE id = ?", keyHolder.getKey().longValue());
        Map<String, Object> stats = updateStats(carId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiSupport.response(
                "success", true, "review", review, "stats", stats));
    }

    @PostMapping("/recompute/{carId}")
    @Transactional
    public ResponseEntity<?> recomputeStats(@PathVariable long carId) {
        return ResponseEntity.ok(ApiSupport.response("success", true, "stats", updateStats(carId)));
    }

    private Map<String, Object> updateStats(long carId) {
        Map<String, Object> aggregate = jdbc.queryForMap(
                "SELECT AVG(rating) AS avg_rating, COUNT(*) AS cnt FROM reviews WHERE car_id = ? AND rating IS NOT NULL",
                carId);
        Object averageValue = aggregate.get("avg_rating");
        BigDecimal average = averageValue == null ? BigDecimal.ZERO
                : new BigDecimal(String.valueOf(averageValue)).setScale(1, RoundingMode.HALF_UP);
        long count = ((Number) aggregate.get("cnt")).longValue();
        jdbc.update("UPDATE thong_tin_xe SET rating = ?, reviews = ? WHERE id = ?", average, count, carId);
        return ApiSupport.response("avg", average.toPlainString(), "count", count);
    }
}
