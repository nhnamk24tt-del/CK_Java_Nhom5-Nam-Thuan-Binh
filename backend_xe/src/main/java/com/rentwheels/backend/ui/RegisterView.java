package com.rentwheels.backend.ui;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;
import java.util.Map;

@Route(value = "register", layout = MainLayout.class)
public class RegisterView extends VerticalLayout {
    public RegisterView(FrontendService service, UserSession session) {
        addClassName("page");
        setAlignItems(Alignment.CENTER);

        TextField name = new TextField("Họ và tên");
        name.setRequiredIndicatorVisible(true);
        EmailField email = new EmailField("Email");
        email.setRequiredIndicatorVisible(true);
        TextField phone = new TextField("Số điện thoại");
        PasswordField password = new PasswordField("Mật khẩu");
        password.setMinLength(8);
        password.setRequiredIndicatorVisible(true);
        Button submit = new Button("Đăng ký", event -> {
            if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
                UiSupport.notifyError(new IllegalArgumentException("Vui lòng điền các trường bắt buộc."));
                return;
            }
            try {
                Map<String, Object> result = service.register(
                        name.getValue(), email.getValue(), phone.getValue(), password.getValue());
                Map<String, Object> user = UiSupport.mapValue(result.get("user"));
                session.signIn(user, String.valueOf(result.get("accessToken")));
                UiSupport.notifySuccess("Tạo tài khoản thành công.");
                UI.getCurrent().navigate(HomeView.class);
            } catch (RuntimeException exception) {
                UiSupport.notifyError(exception);
            }
        });
        VerticalLayout card = new VerticalLayout(
                new H1("Tạo tài khoản"), name, email, phone, password, submit,
                new Anchor("/login", "Đã có tài khoản? Đăng nhập"));
        card.addClassName("form-card");
        add(card);
    }
}
