package com.rentwheels.backend.ui;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import java.util.List;
import java.util.Map;

@Route(value = "wishlist", layout = MainLayout.class)
public class WishlistView extends VerticalLayout {
    public WishlistView(FrontendService service, UserSession session) {
        addClassName("page");
        add(new H1("Xe yêu thích"));
        if (!session.isAuthenticated()) {
            add(new Paragraph("Đăng nhập để lưu và xem xe yêu thích."),
                    new Button("Đăng nhập", event -> UI.getCurrent().navigate(LoginView.class)));
            return;
        }
        List<Map<String, Object>> cars = service.favoriteCars(session);
        if (cars.isEmpty()) {
            add(new Paragraph("Bạn chưa lưu xe nào."));
            return;
        }
        VerticalLayout grid = new VerticalLayout();
        grid.addClassName("vehicle-grid");
        grid.setPadding(false);
        grid.setSpacing(false);
        cars.forEach(car -> grid.add(new VehicleCard(car, () -> {
            try {
                service.toggleFavorite(session, UiSupport.id(car), true);
                UiSupport.notifySuccess("Đã xóa xe khỏi danh sách yêu thích.");
                UI.getCurrent().getPage().reload();
            } catch (RuntimeException exception) {
                UiSupport.notifyError(exception);
            }
        })));
        add(grid);
    }
}
