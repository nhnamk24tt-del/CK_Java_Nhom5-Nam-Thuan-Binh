package com.rentwheels.backend.ui;

import com.rentwheels.backend.AuthController;
import com.rentwheels.backend.BookingController;
import com.rentwheels.backend.CarController;
import com.rentwheels.backend.FavoriteController;
import com.rentwheels.backend.MessageController;
import com.rentwheels.backend.ReviewController;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
public class FrontendService {
    private final AuthController auth;
    private final CarController cars;
    private final BookingController bookings;
    private final FavoriteController favorites;
    private final MessageController messages;
    private final ReviewController reviews;

    public FrontendService(
            AuthController auth,
            CarController cars,
            BookingController bookings,
            FavoriteController favorites,
            MessageController messages,
            ReviewController reviews) {
        this.auth = auth;
        this.cars = cars;
        this.bookings = bookings;
        this.favorites = favorites;
        this.messages = messages;
        this.reviews = reviews;
    }

    public Map<String, Object> login(String email, String password, boolean adminOnly) {
        Map<String, Object> credentials = Map.of("email", email, "password", password);
        return responseMap(adminOnly ? auth.loginAdmin(credentials) : auth.login(credentials));
    }

    public Map<String, Object> register(String name, String email, String phone, String password) {
        return responseMap(auth.register(Map.of(
                "fullName", name,
                "email", email,
                "phone", phone,
                "password", password)));
    }

    public List<Map<String, Object>> cars() {
        return cars.findAll();
    }

    public Map<String, Object> car(long id) {
        return responseMap(cars.findOne(id));
    }

    public List<Map<String, Object>> bookings(UserSession session) {
        Long userId = requireUserId(session);
        return responseList(bookings.findByUserId(userId, null));
    }

    public List<Map<String, Object>> allBookings() {
        return bookings.findAll();
    }

    public List<Map<String, Object>> favoriteCars(UserSession session) {
        Object favoriteIds = expect(favorites.getFavorites(bearer(session)));
        if (!(favoriteIds instanceof List<?> ids)) {
            throw new IllegalStateException("Danh sách yêu thích trả về từ máy chủ không đúng định dạng.");
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object id : ids) {
            if (id instanceof Number number) {
                result.add(car(number.longValue()));
            }
        }
        return result;
    }

    public void toggleFavorite(UserSession session, long carId, boolean favorite) {
        if (favorite) {
            expect(favorites.removeFavorite(bearer(session), carId));
        } else {
            expect(favorites.addFavorite(bearer(session), Map.of("car_id", carId)));
        }
    }

    public List<Map<String, Object>> reviews(long carId) {
        return reviews.findByCarId(carId);
    }

    public void addReview(UserSession session, long carId, int rating, String comment) {
        Map<String, Object> body = userRequest(session);
        body.put("car_id", carId);
        body.put("rating", rating);
        body.put("comment", comment);
        body.put("user_id", requireUserId(session));
        body.put("user_name", session.getUser().getOrDefault("name", ""));
        body.put("user_email", session.getUser().getOrDefault("email", ""));
        expect(reviews.create(body));
    }

    public void createBooking(
            UserSession session,
            long carId,
            String startDate,
            String endDate,
            double totalPrice,
            String pickupLocation,
            String notes) {
        Map<String, Object> body = userRequest(session);
        body.put("user_id", requireUserId(session));
        body.put("car_id", carId);
        body.put("start_date", startDate);
        body.put("end_date", endDate);
        body.put("total_price", totalPrice);
        body.put("pickup_location", pickupLocation);
        body.put("notes", notes);
        body.put("customer_name", session.getUser().getOrDefault("name", ""));
        body.put("customer_phone", session.getUser().getOrDefault("phone", ""));
        expect(bookings.create(body));
    }

    public Map<String, Object> updateProfile(UserSession session, Map<String, Object> updates) {
        Map<String, Object> result = responseMap(auth.updateProfile(bearer(session), updates));
        return map(result.get("user"));
    }

    public void sendMessage(Map<String, Object> form) {
        expect(messages.create(form));
    }

    public List<Map<String, Object>> allMessages(UserSession session) {
        Map<String, Object> response = responseMap(messages.findAll(userRequest(session)));
        if (!Boolean.TRUE.equals(response.get("success"))) {
            throw new IllegalStateException(String.valueOf(response.getOrDefault("message", "Không thể tải tin nhắn.")));
        }
        return responseListValue(response.get("data"));
    }

    public Map<String, Object> message(UserSession session, long id) {
        Map<String, Object> response = responseMap(messages.findOne(id, userRequest(session)));
        return map(response.get("data"));
    }

    public void replyToMessage(UserSession session, long id, String reply) {
        Map<String, Object> body = userRequest(session);
        body.put("response_text", reply);
        expect(messages.reply(id, body));
    }

    public void markMessageRead(UserSession session, long id) {
        expect(messages.markRead(id, userRequest(session)));
    }

    public void saveCar(UserSession session, Map<String, Object> data, Long id) {
        Map<String, Object> body = userRequest(session);
        body.putAll(data);
        if (id == null) {
            expect(cars.create(body));
        } else {
            expect(cars.update(id, body));
        }
    }

    public void deleteCar(UserSession session, long id) {
        expect(cars.delete(id, userRequest(session)));
    }

    public void approveBooking(UserSession session, long id) {
        expect(bookings.approve(id, userRequest(session)));
    }

    public void rejectBooking(UserSession session, long id) {
        expect(bookings.reject(id, userRequest(session)));
    }

    private static Map<String, Object> userRequest(UserSession session) {
        if (!session.isAuthenticated()) {
            throw new IllegalStateException("Vui lòng đăng nhập để tiếp tục.");
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("activeUser", session.getUser());
        return body;
    }

    private static String bearer(UserSession session) {
        if (!session.isAuthenticated()) {
            throw new IllegalStateException("Vui lòng đăng nhập để tiếp tục.");
        }
        return "Bearer " + session.getToken();
    }

    private static Long requireUserId(UserSession session) {
        Long id = session.getUserId();
        if (id == null) {
            throw new IllegalStateException("Phiên đăng nhập không hợp lệ. Vui lòng đăng nhập lại.");
        }
        return id;
    }

    private static Object expect(ResponseEntity<?> response) {
        if (!response.getStatusCode().is2xxSuccessful()) {
            Object body = response.getBody();
            String message = body instanceof Map<?, ?> map && map.get("message") != null
                    ? String.valueOf(map.get("message"))
                    : "Yêu cầu không thành công (" + response.getStatusCode().value() + ").";
            throw new IllegalStateException(message);
        }
        return response.getBody();
    }

    private static Map<String, Object> responseMap(ResponseEntity<?> response) {
        return map(expect(response));
    }

    private static Map<String, Object> map(Object value) {
        if (!(value instanceof Map<?, ?> source)) {
            throw new IllegalStateException("Phản hồi từ máy chủ không đúng định dạng.");
        }
        Map<String, Object> result = new LinkedHashMap<>();
        source.forEach((key, item) -> result.put(String.valueOf(key), item));
        return result;
    }

    private static List<Map<String, Object>> responseList(ResponseEntity<?> response) {
        return responseListValue(expect(response));
    }

    private static List<Map<String, Object>> responseListValue(Object value) {
        if (!(value instanceof List<?> source)) {
            throw new IllegalStateException("Danh sách trả về từ máy chủ không đúng định dạng.");
        }
        return source.stream().map(FrontendService::map).toList();
    }
}
