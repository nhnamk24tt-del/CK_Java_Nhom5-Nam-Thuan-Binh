package com.rentwheels.backend.ui;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import java.util.Map;

@Route(value = "my-bookings", layout = MainLayout.class)
public class BookingHistoryView extends VerticalLayout {
    public BookingHistoryView(FrontendService service, UserSession session) {
        addClassName("page");
        add(new H1("Lịch sử thuê xe"));
        if (!session.isAuthenticated()) {
            add(new Paragraph("Đăng nhập để xem các yêu cầu thuê xe của bạn."),
                    new Button("Đăng nhập", event -> UI.getCurrent().navigate(LoginView.class)));
            return;
        }

        Grid<Map<String, Object>> grid = new Grid<>();
        grid.addColumn(item -> UiSupport.text(item, "car_name")).setHeader("Xe");
        grid.addColumn(item -> UiSupport.text(item, "start_date")).setHeader("Ngày nhận");
        grid.addColumn(item -> UiSupport.text(item, "end_date")).setHeader("Ngày trả");
        grid.addColumn(item -> UiSupport.currency(item.get("total_price"))).setHeader("Tổng tiền");
        grid.addColumn(item -> status(UiSupport.text(item, "status"))).setHeader("Trạng thái");
        grid.setItems(service.bookings(session));
        grid.setWidthFull();
        grid.addClassName("table-wrap");
        add(grid);
    }

    private static String status(String value) {
        return switch (value.toLowerCase()) {
            case "approved" -> "Đã duyệt";
            case "rejected" -> "Từ chối";
            default -> "Chờ duyệt";
        };
    }
}
