package com.rentwheels.backend.ui;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.router.Route;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;

@Route(value = "login", layout = MainLayout.class)
public class LoginView extends VerticalLayout {
    public LoginView(
            FrontendService service,
            UserSession session,
            AdminHandoffService adminHandoff,
            @Value("${app.admin-url:http://localhost:3001}") String adminUrl) {
        addClassName("page");
        setAlignItems(Alignment.CENTER);

        EmailField email = new EmailField("Email");
        email.setRequiredIndicatorVisible(true);
        PasswordField password = new PasswordField("Mật khẩu");
        password.setRequiredIndicatorVisible(true);
        Button submit = new Button("Đăng nhập", event -> {
            if (email.isEmpty() || password.isEmpty()) {
                closePendingTab();
                UiSupport.notifyError(new IllegalArgumentException("Vui lòng nhập email và mật khẩu."));
                return;
            }
            try {
                Map<String, Object> result = service.login(email.getValue(), password.getValue(), false);
                Map<String, Object> user = UiSupport.mapValue(result.get("user"));
                String token = String.valueOf(result.get("accessToken"));
                session.signIn(user, token);
                if (session.isAdmin()) {
                    String ticket = adminHandoff.create(session);
                    session.signOut();
                    UI.getCurrent().getPage().executeJs("""
                            const target = new URL($0);
                            target.hash = 'handoff=' + encodeURIComponent($1);
                            const adminTab = window.__rentwheelsAdminTab;
                            if (adminTab && !adminTab.closed) {
                              adminTab.location.replace(target.toString());
                              window.location.replace(new URL('/', window.location.origin).toString());
                            } else {
                              window.location.replace(target.toString());
                            }
                            window.__rentwheelsAdminTab = null;
                            """, adminUrl + "/admin/dashboard", ticket);
                    return;
                }
                closePendingTab();
                UiSupport.notifySuccess("Đăng nhập thành công.");
                UI.getCurrent().navigate(HomeView.class);
            } catch (RuntimeException exception) {
                closePendingTab();
                UiSupport.notifyError(exception);
            }
        });
        submit.getElement().executeJs("""
                this.addEventListener('click', () => {
                  window.__rentwheelsAdminTab = window.open('about:blank', '_blank');
                });
                """);
        VerticalLayout card = new VerticalLayout(new H1("Đăng nhập"), new Paragraph("Chào mừng bạn quay lại."),
                email, password, submit,
                new com.vaadin.flow.component.html.Anchor("/register", "Tạo tài khoản mới"));
        card.addClassName("form-card");
        add(card);
    }

    private static void closePendingTab() {
        UI.getCurrent().getPage().executeJs("""
                if (window.__rentwheelsAdminTab && !window.__rentwheelsAdminTab.closed) {
                  window.__rentwheelsAdminTab.close();
                }
                window.__rentwheelsAdminTab = null;
                """);
    }
}
