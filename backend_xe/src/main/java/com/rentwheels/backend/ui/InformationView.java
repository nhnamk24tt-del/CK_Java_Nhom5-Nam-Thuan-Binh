package com.rentwheels.backend.ui;

import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import java.util.List;
import java.util.Map;

@Route(value = "policy/warranty", layout = MainLayout.class)
@RouteAlias("policy/returns")
@RouteAlias("policy/shipping")
@RouteAlias("policy/privacy")
public class InformationView extends VerticalLayout implements BeforeEnterObserver {
    private static final Map<String, List<String>> CONTENT = Map.of(
            "categories", List.of("Khám phá xe theo nhu cầu: xe gia đình, sedan, SUV và xe điện."),
            "pricing", List.of("Giá thuê được hiển thị theo từng xe và tính theo số ngày thuê.",
                    "Chi phí cuối cùng sẽ được xác nhận trong yêu cầu đặt xe."),
            "news", List.of("Tin tức và cập nhật mới từ RentWheels sẽ được đăng tại đây."),
            "policy/warranty", List.of("Thông tin bảo hành được trao đổi và xác nhận cùng đội ngũ hỗ trợ khi đặt xe."),
            "policy/returns", List.of("Vui lòng bàn giao xe đúng thời gian và tình trạng đã thống nhất trong hợp đồng."),
            "policy/shipping", List.of("Địa điểm nhận xe được ghi trong yêu cầu thuê và xác nhận trước khi bàn giao."),
            "policy/privacy", List.of("Thông tin cá nhân chỉ được sử dụng để quản lý tài khoản và hỗ trợ dịch vụ thuê xe."));

    public InformationView() {
        addClassName("page");
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        String path = event.getLocation().getPath();
        removeAll();
        H1 heading = new H1(switch (path) {
            case "categories" -> "Danh mục xe";
            case "pricing" -> "Bảng giá";
            case "news" -> "Tin tức";
            case "policy/warranty" -> "Chính sách bảo hành";
            case "policy/returns" -> "Chính sách đổi trả";
            case "policy/shipping" -> "Giao nhận và thanh toán";
            case "policy/privacy" -> "Chính sách bảo mật";
            default -> "RentWheels";
        });
        add(heading);
        CONTENT.getOrDefault(path, List.of("Thông tin đang được cập nhật."))
                .forEach(text -> add(new Paragraph(text)));
    }
}
