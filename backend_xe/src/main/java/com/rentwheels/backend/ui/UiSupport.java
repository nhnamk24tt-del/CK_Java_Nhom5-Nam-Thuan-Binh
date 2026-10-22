package com.rentwheels.backend.ui;

import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.notification.Notification;
import java.util.LinkedHashMap;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.Map;

final class UiSupport {
    private UiSupport() {
    }

    static String text(Map<String, Object> data, String field) {
        Object value = data.get(field);
        return value == null ? "" : String.valueOf(value);
    }

    static long id(Map<String, Object> data) {
        Object value = data.get("id");
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(String.valueOf(value));
    }

    static Map<String, Object> mapValue(Object value) {
        if (!(value instanceof Map<?, ?> source)) {
            throw new IllegalStateException("Phản hồi từ máy chủ không đúng định dạng.");
        }
        Map<String, Object> result = new LinkedHashMap<>();
        source.forEach((key, item) -> result.put(String.valueOf(key), item));
        return result;
    }

    static String currency(Object value) {
        if (value == null) {
            return "0 đ";
        }
        try {
            return NumberFormat.getIntegerInstance(Locale.forLanguageTag("vi-VN"))
                    .format(Double.parseDouble(String.valueOf(value))) + " đ";
        } catch (NumberFormatException exception) {
            return String.valueOf(value) + " đ";
        }
    }

    static Image carImage(Map<String, Object> car) {
        String path = text(car, "image_url");
        if (path.isBlank()) {
        		path = "data:image/svg+xml;utf8,%3Csvg xmlns='http://www.w3.org/2000/svg' width='400' height='260'%3E%3Crect width='100%25' height='100%25' fill='%23e5e7eb'/%3E%3Ctext x='50%25' y='50%25' dominant-baseline='middle' text-anchor='middle' fill='%236b7280' font-size='20' font-family='sans-serif'%3EChưa có ảnh%3C/text%3E%3C/svg%3E";
        } else if (!path.startsWith("http://") && !path.startsWith("https://") && !path.startsWith("/")) {
            path = "/" + path;
        }
        Image image = new Image(path, text(car, "name"));
        image.addClassName("vehicle-image");
        return image;
    }

    static void notifyError(RuntimeException exception) {
        Notification.show(exception.getMessage() == null ? "Đã xảy ra lỗi." : exception.getMessage(), 5000,
                Notification.Position.BOTTOM_START);
    }

    static void notifySuccess(String message) {
        Notification.show(message, 3500, Notification.Position.BOTTOM_START);
    }
}
