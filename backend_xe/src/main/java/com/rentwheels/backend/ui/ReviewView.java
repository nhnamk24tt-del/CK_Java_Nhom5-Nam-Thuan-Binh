package com.rentwheels.backend.ui;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import java.util.Map;

@Route(value = "vehicles/:carId/evaluate", layout = MainLayout.class)
public class ReviewView extends VerticalLayout implements BeforeEnterObserver {
    private final FrontendService service;
    private final UserSession session;

    public ReviewView(FrontendService service, UserSession session) {
        this.service = service;
        this.session = session;
        addClassName("page");
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        removeAll();
        String parameter = event.getRouteParameters().get("carId").orElse("");
        long carId;
        try {
            carId = Long.parseLong(parameter);
        } catch (NumberFormatException exception) {
            UI.getCurrent().navigate(VehicleListView.class);
            return;
        }
        add(new H1("Đánh giá xe"));
        Grid<Map<String, Object>> grid = new Grid<>();
        grid.addColumn(item -> UiSupport.text(item, "user_name")).setHeader("Người đánh giá");
        grid.addColumn(item -> UiSupport.text(item, "rating")).setHeader("Điểm");
        grid.addColumn(item -> UiSupport.text(item, "comment")).setHeader("Nội dung");
        grid.addColumn(item -> UiSupport.text(item, "created_at")).setHeader("Ngày");
        grid.setItems(service.reviews(carId));
        grid.setWidthFull();
        add(grid);
        if (!session.isAuthenticated()) {
            add(new Paragraph("Đăng nhập để gửi đánh giá."),
                    new Button("Đăng nhập", click -> UI.getCurrent().navigate(LoginView.class)));
            return;
        }

        ComboBox<Integer> rating = new ComboBox<>("Số sao");
        rating.setItems(1, 2, 3, 4, 5);
        rating.setValue(5);
        TextArea comment = new TextArea("Nhận xét");
        Button submit = new Button("Gửi đánh giá", click -> {
            if (rating.isEmpty()) {
                UiSupport.notifyError(new IllegalArgumentException("Vui lòng chọn số sao."));
                return;
            }
            try {
                service.addReview(session, carId, rating.getValue(), comment.getValue());
                UiSupport.notifySuccess("Đã gửi đánh giá.");
                UI.getCurrent().getPage().reload();
            } catch (RuntimeException exception) {
                UiSupport.notifyError(exception);
            }
        });
        add(rating, comment, submit);
    }
}
