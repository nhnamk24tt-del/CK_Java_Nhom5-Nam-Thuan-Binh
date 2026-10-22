package com.rentwheels.backend.ui;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;
import java.util.LinkedHashMap;
import java.util.Map;
import java.time.LocalDate;

@Route(value = "profile", layout = MainLayout.class)
public class ProfileView extends VerticalLayout {
    public ProfileView(FrontendService service, UserSession session) {
        addClassName("page");
        if (!session.isAuthenticated()) {
            add(new H1("Hồ sơ"), new Button("Đăng nhập để xem hồ sơ",
                    event -> UI.getCurrent().navigate(LoginView.class)));
            return;
        }

        Map<String, Object> user = session.getUser();
        TextField name = new TextField("Họ và tên");
        name.setValue(UiSupport.text(user, "name"));
        TextField phone = new TextField("Số điện thoại");
        phone.setValue(UiSupport.text(user, "phone"));
        TextArea address = new TextArea("Địa chỉ");
        address.setValue(UiSupport.text(user, "address"));
        ComboBox<String> gender = new ComboBox<>("Giới tính");
        gender.setItems("Nam", "Nữ", "Khác");
        gender.setValue(UiSupport.text(user, "gender"));
        DatePicker birthdate = new DatePicker("Ngày sinh");
        try {
            String value = UiSupport.text(user, "birthdate");
            if (!value.isBlank()) {
                birthdate.setValue(LocalDate.parse(value.substring(0, Math.min(10, value.length()))));
            }
        } catch (java.time.DateTimeException exception) {
            UiSupport.notifyError(new IllegalStateException("Ngày sinh trong hồ sơ hiện tại không hợp lệ.", exception));
        }
        TextField driversLicense = new TextField("Số giấy phép lái xe");
        driversLicense.setValue(UiSupport.text(user, "drivers_license"));
        Button save = new Button("Lưu thay đổi", event -> {
            try {
                Map<String, Object> updates = new LinkedHashMap<>();
                updates.put("name", name.getValue());
                updates.put("phone", phone.getValue());
                updates.put("address", address.getValue());
                updates.put("gender", gender.getValue());
                updates.put("birthdate", birthdate.getValue() == null ? null : birthdate.getValue().toString());
                updates.put("drivers_license", driversLicense.getValue());
                Map<String, Object> updatedUser = service.updateProfile(session, updates);
                session.signIn(updatedUser, session.getToken());
                UiSupport.notifySuccess("Đã cập nhật hồ sơ.");
            } catch (RuntimeException exception) {
                UiSupport.notifyError(exception);
            }
        });
        Button signOut = new Button("Đăng xuất", event -> {
            session.signOut();
            UI.getCurrent().navigate(HomeView.class);
        });
        VerticalLayout form = new VerticalLayout(new H1("Hồ sơ cá nhân"), name,
                new TextField("Email", UiSupport.text(user, "email")), phone, address,
                gender, birthdate, driversLicense, save, signOut);
        form.addClassName("form-card");
        add(form);
    }
}
