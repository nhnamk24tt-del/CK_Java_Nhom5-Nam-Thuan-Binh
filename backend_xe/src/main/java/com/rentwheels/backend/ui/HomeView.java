package com.rentwheels.backend.ui;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;

@Route(value = "", layout = MainLayout.class)
public class HomeView extends VerticalLayout {
    public HomeView(FrontendService service, UserSession session) {
        addClassName("page");
        setPadding(false);
        setSpacing(true);

        String title = session.isAuthenticated()
                ? "Xin chào, " + session.getUser().getOrDefault("name", "bạn")
                : "Thuê xe dễ dàng, hành trình trọn vẹn";
        String description = session.isAuthenticated()
                ? "Tài khoản của bạn đã đăng nhập. Khám phá xe, theo dõi đơn thuê và cập nhật hồ sơ ngay tại đây."
                : "Chọn chiếc xe phù hợp, đặt lịch trực tuyến và bắt đầu chuyến đi của bạn.";
        Button explore = new Button("Khám phá xe",
                event -> getUI().ifPresent(ui -> ui.navigate(VehicleListView.class)));
        VerticalLayout hero = new VerticalLayout(new H1(title), new Paragraph(description), explore);
        hero.addClassName("hero");
        add(hero);

        if (session.isAuthenticated()) {
            HorizontalLayout accountActions = new HorizontalLayout(
                    new Button("Hồ sơ của tôi",
                            event -> getUI().ifPresent(ui -> ui.navigate(ProfileView.class))),
                    new Button("Đơn thuê của tôi",
                            event -> getUI().ifPresent(ui -> ui.navigate(BookingHistoryView.class))));
            accountActions.setWidthFull();
            accountActions.addClassName("surface-card");
            add(accountActions);
        }

        HorizontalLayout benefits = new HorizontalLayout(
                feature("Lựa chọn đa dạng", "Nhiều dòng xe và mức giá phù hợp."),
                feature("Đặt xe trực tuyến", "Gửi yêu cầu thuê xe nhanh chóng."),
                feature("Hỗ trợ khách hàng", "Liên hệ đội ngũ RentWheels khi cần."));
        benefits.setWidthFull();
        benefits.setFlexGrow(1, benefits.getChildren().toArray(com.vaadin.flow.component.Component[]::new));
        add(benefits, new H2("Xe nổi bật"));

        VerticalLayout vehicleGrid = new VerticalLayout();
        vehicleGrid.addClassName("vehicle-grid");
        vehicleGrid.setPadding(false);
        vehicleGrid.setSpacing(false);
        service.cars().stream().limit(6).forEach(car -> vehicleGrid.add(new VehicleCard(car)));
        add(vehicleGrid);
    }

    private static VerticalLayout feature(String title, String description) {
        VerticalLayout item = new VerticalLayout(new H2(title), new Paragraph(description));
        item.addClassName("surface-card");
        item.setPadding(true);
        return item;
    }
}
