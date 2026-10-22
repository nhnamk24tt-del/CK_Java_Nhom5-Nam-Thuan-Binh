package com.rentwheels.backend.ui;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;
import java.util.LinkedHashMap;
import java.util.Map;

@Route(value = "contact", layout = MainLayout.class)
public class ContactView extends VerticalLayout {
    public ContactView(FrontendService service, UserSession session) {
        addClassName("page");
        TextField name = new TextField("Họ và tên");
        EmailField email = new EmailField("Email");
        TextField phone = new TextField("Số điện thoại");
        TextField subject = new TextField("Chủ đề");
        TextArea message = new TextArea("Nội dung");
        message.setMinHeight("140px");
        if (session.isAuthenticated()) {
            name.setValue(UiSupport.text(session.getUser(), "name"));
            email.setValue(UiSupport.text(session.getUser(), "email"));
            phone.setValue(UiSupport.text(session.getUser(), "phone"));
        }

        Button send = new Button("Gửi liên hệ", event -> {
            if (name.isEmpty() || email.isEmpty() || subject.isEmpty() || message.isEmpty()) {
                UiSupport.notifyError(new IllegalArgumentException("Vui lòng điền đầy đủ thông tin liên hệ."));
                return;
            }
            try {
                Map<String, Object> form = new LinkedHashMap<>();
                form.put("name", name.getValue());
                form.put("email", email.getValue());
                form.put("phone", phone.getValue());
                form.put("subject", subject.getValue());
                form.put("message", message.getValue());
                form.put("user_id", session.getUserId());
                service.sendMessage(form);
                UiSupport.notifySuccess("Tin nhắn của bạn đã được gửi.");
                subject.clear();
                message.clear();
            } catch (RuntimeException exception) {
                UiSupport.notifyError(exception);
            }
        });
        VerticalLayout card = new VerticalLayout(new H1("Liên hệ RentWheels"),
                new Paragraph("Gửi câu hỏi hoặc yêu cầu hỗ trợ, chúng tôi sẽ phản hồi sớm nhất."),
                name, email, phone, subject, message, send);
        card.addClassName("form-card");
        add(card);
    }
}
