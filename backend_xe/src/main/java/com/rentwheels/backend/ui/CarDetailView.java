package com.rentwheels.backend.ui;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;

@Route(value = "vehicles/:carId", layout = MainLayout.class)
public class CarDetailView extends VerticalLayout implements BeforeEnterObserver {
    private final FrontendService service;
    private final UserSession session;

    @Autowired
    public CarDetailView(FrontendService service, UserSession session) {
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

        Map<String, Object> car;
        try {
            car = service.car(carId);
        } catch (RuntimeException exception) {
            UiSupport.notifyError(exception);
            UI.getCurrent().navigate(VehicleListView.class);
            return;
        }

        add(UiSupport.carImage(car), new H1(UiSupport.text(car, "name")),
                new Paragraph(UiSupport.text(car, "brand") + " · " + UiSupport.text(car, "type")),
                new Paragraph(UiSupport.currency(car.get("price")) + " / ngày"),
                new Paragraph(UiSupport.text(car, "description")));
        if (!isAvailable(car)) {
            add(new Paragraph("Xe hiện không còn sẵn để đặt."));
        }

        Button favorite = new Button("Thêm vào yêu thích", click -> {
            if (!session.isAuthenticated()) {
                UI.getCurrent().navigate(LoginView.class);
                return;
            }
            try {
                service.toggleFavorite(session, carId, false);
                UiSupport.notifySuccess("Đã thêm xe vào danh sách yêu thích.");
            } catch (RuntimeException exception) {
                UiSupport.notifyError(exception);
            }
        });
        add(favorite);

        H2 rentTitle = new H2("Gửi yêu cầu thuê xe");
        DatePicker startDate = new DatePicker("Ngày nhận xe");
        DatePicker endDate = new DatePicker("Ngày trả xe");
        startDate.setMin(LocalDate.now());
        endDate.setMin(LocalDate.now().plusDays(1));
        TextField pickup = new TextField("Địa điểm nhận xe");
        TextArea notes = new TextArea("Ghi chú");
        Button book = new Button("Gửi yêu cầu", click -> {
            if (!isAvailable(car)) {
                Notification.show("Xe hiện không còn sẵn để đặt.");
                return;
            }
            if (!session.isAuthenticated()) {
                UI.getCurrent().navigate(LoginView.class);
                return;
            }
            LocalDate start = startDate.getValue();
            LocalDate end = endDate.getValue();
            if (start == null || end == null || !end.isAfter(start)) {
                Notification.show("Vui lòng chọn ngày trả xe sau ngày nhận xe.");
                return;
            }
            try {
                double days = ChronoUnit.DAYS.between(start, end);
                double price = Double.parseDouble(String.valueOf(car.get("price")));
                service.createBooking(session, carId, start.toString(), end.toString(),
                        days * price, pickup.getValue(), notes.getValue());
                UiSupport.notifySuccess("Đã gửi yêu cầu thuê xe.");
                UI.getCurrent().navigate(BookingHistoryView.class);
            } catch (RuntimeException exception) {
                UiSupport.notifyError(exception);
            }
        });
        add(rentTitle, new HorizontalLayout(startDate, endDate), pickup, notes, book,
                new Button("Xem và gửi đánh giá", click -> UI.getCurrent().navigate(
                        "vehicles/" + carId + "/evaluate")));
    }

    private static boolean isAvailable(Map<String, Object> car) {
        String value = UiSupport.text(car, "available");
        String quantity = UiSupport.text(car, "quantity");
        return !value.equalsIgnoreCase("false") && !value.equals("0")
                && (quantity.isBlank() || !quantity.equals("0"));
    }
}
