package com.rentwheels.backend.ui;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;

@Route(value = "admin/dashboard", layout = MainLayout.class)
public class AdminView extends VerticalLayout implements BeforeEnterObserver {
    private final FrontendService service;
    private final UserSession session;
    private final AdminHandoffService adminHandoff;
    private final boolean adminInstance;
    private final VerticalLayout content = new VerticalLayout();
    private List<Map<String, Object>> carData;
    private List<Map<String, Object>> bookingData;
    private List<Map<String, Object>> messageData;

    public AdminView(
            FrontendService service,
            UserSession session,
            AdminHandoffService adminHandoff,
            @Value("${app.instance-role:user}") String instanceRole) {
        this.service = service;
        this.session = session;
        this.adminHandoff = adminHandoff;
        adminInstance = "admin".equals(instanceRole);
        addClassName("page");
        if (adminInstance && !session.isAdmin()) {
            session.signOut();
            showLogin();
        } else if (session.isAdmin()) {
            showDashboard();
        }
        addAttachListener(this::completeHandoff);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if (!adminInstance) {
            event.forwardTo(HomeView.class);
        }
    }

    private void completeHandoff(AttachEvent event) {
        if (!adminInstance || session.isAdmin()) {
            return;
        }
        UI ui = event.getUI();
        ui.getPage().executeJs("""
                return window.location.hash.startsWith('#handoff=')
                    ? decodeURIComponent(window.location.hash.substring(9))
                    : '';
                """).then(String.class, ticket -> {
            if (ticket == null || ticket.isBlank()) {
                return;
            }
            try {
                AdminHandoffService.AdminIdentity identity = adminHandoff.consume(ticket);
                session.signIn(identity.user(), identity.accessToken());
                ui.getPage().executeJs("""
                        history.replaceState(null, '', window.location.pathname + window.location.search);
                        window.location.reload();
                        """);
            } catch (RuntimeException exception) {
                ui.getPage().executeJs(
                        "history.replaceState(null, '', window.location.pathname + window.location.search)");
                UiSupport.notifyError(exception);
            }
        });
    }

    private void showLogin() {
        EmailField email = new EmailField("Email quản trị");
        PasswordField password = new PasswordField("Mật khẩu");
        Button login = new Button("Đăng nhập quản trị", event -> {
            if (email.isEmpty() || password.isEmpty()) {
                UiSupport.notifyError(new IllegalArgumentException("Vui lòng nhập email và mật khẩu."));
                return;
            }
            try {
                Map<String, Object> result = service.login(email.getValue(), password.getValue(), true);
                session.signIn(UiSupport.mapValue(result.get("user")),
                        String.valueOf(result.get("accessToken")));
                UI.getCurrent().getPage().reload();
            } catch (RuntimeException exception) {
                UiSupport.notifyError(exception);
            }
        });
        VerticalLayout card = new VerticalLayout(new H1("Hệ thống quản trị viên"), email, password, login);
        card.addClassName("form-card");
        add(card);
    }

    private void showDashboard() {
        add(new H1("Bảng điều khiển quản trị"));
        Tab cars = new Tab("Quản lý xe");
        Tab stats = new Tab("Thống kê");
        Tab bookings = new Tab("Yêu cầu thuê");
        Tab messages = new Tab("Tin nhắn");
        Tabs tabs = new Tabs(cars, stats, bookings, messages);
        content.setPadding(false);
        content.setWidthFull();
        tabs.addSelectedChangeListener(event -> renderTab(tabs.getSelectedTab()));
        add(tabs, content);
        renderTab(cars);
    }

    private void renderTab(Tab tab) {
        content.removeAll();
        if ("Thống kê".equals(tab.getLabel())) {
            renderStatistics();
        } else if ("Yêu cầu thuê".equals(tab.getLabel())) {
            renderBookings();
        } else if ("Tin nhắn".equals(tab.getLabel())) {
            renderMessages();
        } else {
            renderCars();
        }
    }

    private void renderCars() {
        carData = service.cars();
        TextField name = new TextField("Tên xe");
        TextField brand = new TextField("Hãng");
        TextField type = new TextField("Phân khúc");
        NumberField price = new NumberField("Giá thuê/ngày");
        NumberField seats = new NumberField("Số chỗ");
        ComboBox<String> transmission = new ComboBox<>("Hộp số");
        transmission.setItems("Tự động", "Số sàn");
        ComboBox<String> fuel = new ComboBox<>("Nhiên liệu");
        fuel.setItems("Xăng", "Diesel", "Điện", "Hybrid");
        NumberField year = new NumberField("Năm");
        TextField color = new TextField("Màu sắc");
        NumberField quantity = new NumberField("Số lượng");
        TextField image = new TextField("Đường dẫn ảnh");
        TextArea description = new TextArea("Mô tả");
        Checkbox available = new Checkbox("Còn xe", true);
        Long[] editingId = {null};
        Button[] save = new Button[1];
        Details[] editor = new Details[1];
        save[0] = new Button("Thêm xe", event -> {
            if (name.isEmpty() || brand.isEmpty() || type.isEmpty() || price.isEmpty()) {
                UiSupport.notifyError(new IllegalArgumentException("Vui lòng nhập tên, hãng, phân khúc và giá xe."));
                return;
            }
            try {
                Map<String, Object> form = new LinkedHashMap<>();
                form.put("name", name.getValue());
                form.put("brand", brand.getValue());
                form.put("type", type.getValue());
                form.put("price", price.getValue());
                form.put("seats", seats.getValue() == null ? 5 : seats.getValue().intValue());
                form.put("transmission", transmission.getValue());
                form.put("fuel", fuel.getValue());
                form.put("year", year.getValue() == null ? LocalDate.now().getYear() : year.getValue().intValue());
                form.put("color", color.getValue());
                form.put("quantity", quantity.getValue() == null ? 1 : quantity.getValue().intValue());
                form.put("image_url", image.getValue());
                form.put("description", description.getValue());
                form.put("available", available.getValue());
                service.saveCar(session, form, editingId[0]);
                UiSupport.notifySuccess(editingId[0] == null ? "Đã thêm xe." : "Đã cập nhật xe.");
                editingId[0] = null;
                save[0].setText("Thêm xe");
                clearCarForm(name, brand, type, price, seats, transmission, fuel, year, color, quantity, image,
                        description, available);
                renderCars();
            } catch (RuntimeException exception) {
                UiSupport.notifyError(exception);
            }
        });
        save[0].addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        FormLayout fields = new FormLayout(name, brand, type, price, seats, transmission,
                fuel, year, color, quantity, image, description, available);
        fields.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("640px", 2));
        fields.setColspan(description, 2);
        fields.setWidthFull();
        Button clear = new Button("Hủy", event -> {
            editingId[0] = null;
            save[0].setText("Thêm xe");
            editor[0].setSummaryText("Thêm xe mới");
            clearCarForm(name, brand, type, price, seats, transmission, fuel, year, color, quantity, image,
                    description, available);
            editor[0].setOpened(false);
        });
        HorizontalLayout actions = new HorizontalLayout(save[0], clear);
        VerticalLayout form = new VerticalLayout(
                new H2("Thông tin xe"),
                new Paragraph("Hoàn thiện thông tin để khách hàng dễ dàng tìm và thuê xe."),
                fields,
                actions);
        form.addClassNames("surface-card", "admin-car-form");
        editor[0] = new Details("Thêm xe mới", form);
        editor[0].addClassName("admin-car-editor");

        Grid<Map<String, Object>> grid = new Grid<>();
        grid.addComponentColumn(item -> {
            var thumbnail = UiSupport.carImage(item);
            thumbnail.addClassName("admin-car-thumb");
            return thumbnail;
        }).setHeader("Ảnh").setFlexGrow(0).setWidth("100px");
        grid.addComponentColumn(item -> {
            VerticalLayout details = new VerticalLayout(
                    new Span(UiSupport.text(item, "name")),
                    new Span(UiSupport.text(item, "brand") + " · " + UiSupport.text(item, "type")));
            details.setPadding(false);
            details.setSpacing(false);
            details.addClassName("admin-car-name");
            return details;
        }).setHeader("Xe").setFlexGrow(2);
        grid.addComponentColumn(item -> {
            VerticalLayout details = new VerticalLayout(
                    new Span(UiSupport.currency(item.get("price")) + " / ngày"),
                    new Span(UiSupport.text(item, "quantity") + " xe trong kho"));
            details.setPadding(false);
            details.setSpacing(false);
            details.addClassName("admin-car-inventory");
            return details;
        }).setHeader("Giá & tồn kho").setFlexGrow(1);
        grid.addComponentColumn(item -> {
            boolean active = !"0".equals(UiSupport.text(item, "available"))
                    && !"false".equalsIgnoreCase(UiSupport.text(item, "available"));
            Span badge = new Span(active ? "Đang cho thuê" : "Tạm ngưng");
            badge.addClassNames("admin-status-badge", active ? "is-available" : "is-unavailable");
            return badge;
        }).setHeader("Trạng thái").setFlexGrow(0);
        grid.addComponentColumn(item -> {
            Button edit = new Button("Sửa", event -> {
                    editingId[0] = UiSupport.id(item);
                    save[0].setText("Lưu thay đổi");
                    editor[0].setSummaryText("Chỉnh sửa xe: " + UiSupport.text(item, "name"));
                    editor[0].setOpened(true);
                    name.setValue(UiSupport.text(item, "name"));
                    brand.setValue(UiSupport.text(item, "brand"));
                    type.setValue(UiSupport.text(item, "type"));
                    price.setValue(asDouble(item.get("price")));
                    seats.setValue(asDouble(item.get("seats")));
                    transmission.setValue(UiSupport.text(item, "transmission"));
                    fuel.setValue(UiSupport.text(item, "fuel"));
                    year.setValue(asDouble(item.get("year")));
                    color.setValue(UiSupport.text(item, "color"));
                    quantity.setValue(asDouble(item.get("quantity")));
                    image.setValue(UiSupport.text(item, "image_url"));
                    description.setValue(UiSupport.text(item, "description"));
                    available.setValue(!"0".equals(UiSupport.text(item, "available"))
                            && !"false".equalsIgnoreCase(UiSupport.text(item, "available")));
                });
            Button delete = new Button("Xóa", event -> {
                    ConfirmDialog confirmation = new ConfirmDialog();
                    confirmation.setHeader("Xóa xe khỏi danh sách?");
                    confirmation.setText("Bạn có chắc muốn xóa " + UiSupport.text(item, "name") + "?");
                    confirmation.setCancelable(true);
                    confirmation.setCancelText("Hủy");
                    confirmation.setConfirmText("Xóa xe");
                    confirmation.setConfirmButtonTheme("error primary");
                    confirmation.addConfirmListener(confirm -> {
                        try {
                            service.deleteCar(session, UiSupport.id(item));
                            UiSupport.notifySuccess("Đã xóa xe.");
                            renderCars();
                        } catch (RuntimeException exception) {
                            UiSupport.notifyError(exception);
                        }
                    });
                    confirmation.open();
                });
            edit.addThemeVariants(ButtonVariant.LUMO_SMALL);
            delete.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_SMALL);
            VerticalLayout rowActions = new VerticalLayout(edit, delete);
            rowActions.setPadding(false);
            rowActions.setSpacing(false);
            return rowActions;
        }).setHeader("Thao tác").setFlexGrow(0).setWidth("110px");
        grid.setItems(carData);
        grid.setWidthFull();
        grid.setHeight("520px");
        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES, GridVariant.LUMO_COLUMN_BORDERS);
        grid.addClassName("admin-data-grid");

        TextField search = new TextField();
        search.setPlaceholder("Tìm theo tên xe, hãng hoặc phân khúc");
        search.setClearButtonVisible(true);
        search.setWidthFull();
        search.addValueChangeListener(event -> applyCarFilter(grid, event.getValue()));
        Button addCar = new Button("Thêm xe mới", event -> {
            editingId[0] = null;
            save[0].setText("Thêm xe");
            editor[0].setSummaryText("Thêm xe mới");
            clearCarForm(name, brand, type, price, seats, transmission, fuel, year, color, quantity, image,
                    description, available);
            editor[0].setOpened(true);
            name.focus();
        });
        addCar.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        HorizontalLayout listHeader = new HorizontalLayout(
                new H2("Danh sách xe"), new Span(carData.size() + " xe"), addCar, search);
        listHeader.setDefaultVerticalComponentAlignment(HorizontalLayout.Alignment.CENTER);
        listHeader.setWidthFull();
        listHeader.expand(search);
        listHeader.addClassName("admin-list-header");
        content.add(editor[0], listHeader, grid);
    }

    private void renderBookings() {
        bookingData = service.allBookings();
        HorizontalLayout summary = new HorizontalLayout(
                metric("Tổng yêu cầu", String.valueOf(bookingData.size())),
                metric("Chờ xác nhận", String.valueOf(countStatus(bookingData, "pending"))),
                metric("Đã xác nhận", String.valueOf(countStatus(bookingData, "approved"))),
                metric("Đã từ chối", String.valueOf(countStatus(bookingData, "rejected"))));
        summary.addClassName("metrics");
        summary.setWidthFull();
        content.add(new H2("Quản lý yêu cầu thuê"), summary);

        Grid<Map<String, Object>> grid = new Grid<>();
        grid.addColumn(item -> UiSupport.text(item, "id")).setHeader("Mã").setFlexGrow(0).setWidth("80px");
        grid.addColumn(item -> UiSupport.text(item, "customer_name")).setHeader("Khách hàng");
        grid.addColumn(item -> UiSupport.text(item, "customer_phone")).setHeader("Điện thoại");
        grid.addColumn(item -> UiSupport.text(item, "car_name")).setHeader("Xe");
        grid.addColumn(item -> UiSupport.text(item, "start_date") + " – " + UiSupport.text(item, "end_date"))
                .setHeader("Thời gian");
        grid.addColumn(item -> UiSupport.currency(item.get("total_price"))).setHeader("Thành tiền");
        grid.addColumn(item -> UiSupport.text(item, "pickup_location")).setHeader("Địa điểm nhận");
        grid.addComponentColumn(item -> statusBadge(UiSupport.text(item, "status"))).setHeader("Trạng thái");
        grid.addComponentColumn(item -> {
            if (!"pending".equalsIgnoreCase(UiSupport.text(item, "status"))) {
                return new Span("—");
            }
            Button approve = new Button("Xác nhận", event -> changeBooking(item, true));
            approve.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);
            Button reject = new Button("Từ chối", event -> changeBooking(item, false));
            reject.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_SMALL);
            approve.setWidthFull();
            reject.setWidthFull();
            VerticalLayout actions = new VerticalLayout(approve, reject);
            actions.setPadding(false);
            actions.setSpacing(false);
            actions.setWidthFull();
            actions.addClassName("booking-actions");
            return actions;
        }).setHeader("Thao tác").setFlexGrow(0).setWidth("150px");
        grid.setItems(bookingData);
        grid.setWidthFull();
        grid.setHeight("500px");
        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES, GridVariant.LUMO_COLUMN_BORDERS);
        grid.addClassName("admin-data-grid");
        content.add(grid);
    }

    private void changeBooking(Map<String, Object> booking, boolean approve) {
        try {
            if (approve) {
                service.approveBooking(session, UiSupport.id(booking));
            } else {
                service.rejectBooking(session, UiSupport.id(booking));
            }
            UiSupport.notifySuccess(approve ? "Đã duyệt yêu cầu thuê." : "Đã từ chối yêu cầu thuê.");
            renderBookings();
        } catch (RuntimeException exception) {
            UiSupport.notifyError(exception);
        }
    }

    private void renderMessages() {
        messageData = service.allMessages(session);
        Grid<Map<String, Object>> grid = new Grid<>();
        grid.addColumn(item -> UiSupport.text(item, "sender_name")).setHeader("Người gửi");
        grid.addColumn(item -> UiSupport.text(item, "subject")).setHeader("Chủ đề");
        grid.addColumn(item -> UiSupport.text(item, "created_at")).setHeader("Ngày gửi");
        grid.addComponentColumn(item -> statusBadge(UiSupport.text(item, "status"))).setHeader("Trạng thái");
        grid.addItemClickListener(event -> showMessage(event.getItem()));
        grid.addComponentColumn(item -> new Button("Xem tin nhắn", click -> showMessage(item)))
                .setHeader("Nội dung").setFlexGrow(0);
        grid.setItems(messageData);
        grid.setWidthFull();
        grid.setHeight("520px");
        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES, GridVariant.LUMO_COLUMN_BORDERS);
        grid.addClassName("admin-data-grid");
        long unread = messageData.stream()
                .filter(item -> "new".equalsIgnoreCase(UiSupport.text(item, "status"))).count();
        content.add(new H2("Hộp thư liên hệ"),
                new Paragraph(messageData.size() + " tin nhắn · " + unread + " chưa đọc"),
                grid);
    }

    private void showMessage(Map<String, Object> selected) {
        content.removeAll();
        Map<String, Object> message = service.message(session, UiSupport.id(selected));
        TextArea reply = new TextArea("Phản hồi");
        reply.setValue(UiSupport.text(message, "admin_reply"));
        reply.setWidthFull();
        reply.setMinHeight("140px");
        Button send = new Button("Lưu phản hồi", event -> {
            if (reply.isEmpty()) {
                UiSupport.notifyError(new IllegalArgumentException("Vui lòng nhập nội dung phản hồi."));
                return;
            }
            try {
                service.replyToMessage(session, UiSupport.id(message), reply.getValue());
                UiSupport.notifySuccess("Đã lưu phản hồi.");
                renderMessages();
            } catch (RuntimeException exception) {
                UiSupport.notifyError(exception);
            }
        });
        Button markRead = new Button("Đánh dấu đã đọc", event -> {
            try {
                service.markMessageRead(session, UiSupport.id(message));
                UiSupport.notifySuccess("Đã đánh dấu tin nhắn.");
                renderMessages();
            } catch (RuntimeException exception) {
                UiSupport.notifyError(exception);
            }
        });
        VerticalLayout messageCard = new VerticalLayout(
                new Span("NỘI DUNG LIÊN HỆ"),
                new H2(UiSupport.text(message, "subject")),
                new Paragraph(UiSupport.text(message, "sender_name") + " · "
                        + UiSupport.text(message, "sender_email")),
                new Paragraph("Điện thoại: " + UiSupport.text(message, "sender_phone")),
                new Paragraph("Ngày gửi: " + UiSupport.text(message, "created_at")),
                new Paragraph(UiSupport.text(message, "message")));
        messageCard.addClassNames("surface-card", "admin-message-card");
        String previousReply = UiSupport.text(message, "admin_reply");
        if (!previousReply.isBlank()) {
            messageCard.add(new H3("Phản hồi trước đó"), new Paragraph(previousReply));
        }
        HorizontalLayout actions = new HorizontalLayout(send, markRead,
                new Button("Quay lại hộp thư", event -> renderMessages()));
        content.add(new H2("Chi tiết tin nhắn"), messageCard, reply, actions);
    }

    private void renderStatistics() {
        carData = service.cars();
        bookingData = service.allBookings();
        messageData = service.allMessages(session);
        long pending = bookingData.stream()
                .filter(item -> "pending".equalsIgnoreCase(UiSupport.text(item, "status"))).count();
        double revenue = bookingData.stream()
                .filter(item -> "approved".equalsIgnoreCase(UiSupport.text(item, "status")))
                .mapToDouble(item -> asDouble(item.get("total_price"))).sum();
        HorizontalLayout metrics = new HorizontalLayout(
                metric("Tổng số loại xe", String.valueOf(carData.size())),
                metric("Yêu cầu thuê", String.valueOf(bookingData.size())),
                metric("Đang chờ duyệt", String.valueOf(pending)),
                metric("Tin nhắn", String.valueOf(messageData.size())),
                metric("Doanh thu ước tính", UiSupport.currency(revenue)));
        metrics.addClassName("metrics");
        metrics.setWidthFull();
        content.add(new H2("Tổng quan"), metrics);

        List<String> monthLabels = new ArrayList<>();
        Map<String, List<Double>> bookingSeries = new LinkedHashMap<>();
        bookingSeries.put("Chờ xác nhận", new ArrayList<>());
        bookingSeries.put("Đã xác nhận", new ArrayList<>());
        bookingSeries.put("Đã từ chối", new ArrayList<>());
        List<Double> monthlyRevenue = new ArrayList<>();
        for (int offset = 11; offset >= 0; offset--) {
            YearMonth month = YearMonth.now().minusMonths(offset);
            monthLabels.add(month.getMonth().getDisplayName(TextStyle.SHORT, Locale.forLanguageTag("vi-VN"))
                    + " " + String.valueOf(month.getYear()).substring(2));
            bookingSeries.get("Chờ xác nhận").add((double) countBookings(month, "pending"));
            bookingSeries.get("Đã xác nhận").add((double) countBookings(month, "approved"));
            bookingSeries.get("Đã từ chối").add((double) countBookings(month, "rejected"));
            monthlyRevenue.add(bookingData.stream()
                    .filter(item -> UiSupport.text(item, "created_at").startsWith(month.toString()))
                    .filter(item -> "approved".equalsIgnoreCase(UiSupport.text(item, "status")))
                    .mapToDouble(item -> asDouble(item.get("total_price"))).sum());
        }

        Map<String, Long> bookingStatus = new LinkedHashMap<>();
        bookingStatus.put("Chờ xác nhận", countStatus(bookingData, "pending"));
        bookingStatus.put("Đã xác nhận", countStatus(bookingData, "approved"));
        bookingStatus.put("Đã từ chối", countStatus(bookingData, "rejected"));
        Map<String, Long> carsByType = new LinkedHashMap<>();
        carData.forEach(car -> {
            String type = UiSupport.text(car, "type");
            carsByType.merge(type.isBlank() ? "Khác" : type, 1L, Long::sum);
        });
        Map<String, Long> topCarTypes = new LinkedHashMap<>();
        carsByType.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(8)
                .forEach(entry -> topCarTypes.put(entry.getKey(), entry.getValue()));

        content.add(
                chartCard("Yêu cầu thuê theo tháng", "Số yêu cầu được tạo trong 12 tháng gần nhất, phân theo trạng thái.",
                        groupedBarChart(monthLabels, bookingSeries)),
                chartCard("Doanh thu ước tính theo tháng", "Tổng giá trị yêu cầu đã xác nhận, nhóm theo ngày tạo.",
                        areaChart(monthLabels, monthlyRevenue)),
                chartCard("Cơ cấu yêu cầu thuê", "Tỷ lệ yêu cầu theo trạng thái hiện tại.",
                        donutChart(bookingStatus)),
                chartCard("Danh mục xe trong kho", "Hiển thị tối đa 8 danh mục xe đang quản lý theo phân khúc.",
                        horizontalBarChart(topCarTypes)));
    }

    private long countBookings(YearMonth month, String status) {
        return bookingData.stream()
                .filter(item -> UiSupport.text(item, "created_at").startsWith(month.toString()))
                .filter(item -> status.equalsIgnoreCase(UiSupport.text(item, "status")))
                .count();
    }

    private static long countStatus(List<Map<String, Object>> rows, String status) {
        return rows.stream().filter(item -> status.equalsIgnoreCase(UiSupport.text(item, "status"))).count();
    }

    private static Div chartCard(String title, String description, String svg) {
        Div chart = new Div();
        chart.addClassName("admin-chart");
        Div drawing = new Div();
        drawing.addClassName("admin-chart-drawing");
        drawing.getElement().setProperty("innerHTML", svg);
        chart.add(new H3(title), new Paragraph(description), drawing);
        return chart;
    }

    private static VerticalLayout metric(String title, String value) {
        VerticalLayout card = new VerticalLayout(new Span(title), new Span(value));
        card.addClassName("metric-card");
        card.setPadding(true);
        card.setWidthFull();
        card.getChildren().skip(1).findFirst().ifPresent(component -> component.getElement().getStyle()
                .set("font-size", "1.5rem").set("font-weight", "700"));
        return card;
    }

    private static Span statusBadge(String value) {
        String normalized = value.toLowerCase(Locale.ROOT);
        Span badge = new Span(status(value));
        badge.addClassName("admin-status-badge");
        badge.addClassName(switch (normalized) {
            case "approved", "replied" -> "is-success";
            case "rejected" -> "is-danger";
            case "new", "pending" -> "is-pending";
            default -> "is-neutral";
        });
        return badge;
    }

    private static String groupedBarChart(List<String> labels, Map<String, List<Double>> series) {
        int width = 900;
        int height = 340;
        int left = 48;
        int right = 18;
        int top = 42;
        int bottom = 54;
        double plotWidth = width - left - right;
        double plotHeight = height - top - bottom;
        double max = series.values().stream().flatMap(List::stream)
                .mapToDouble(Double::doubleValue).max().orElse(0);
        double step = chartStep(max);
        max = Math.max(step, Math.ceil(max / step) * step);
        String[] colors = {"#f59e0b", "#2563eb", "#ef4444"};
        StringBuilder svg = chartStart(width, height, "Biểu đồ cột yêu cầu thuê theo tháng");
        for (int tick = 0; tick <= 4; tick++) {
            double value = max * tick / 4;
            double y = top + plotHeight - plotHeight * tick / 4;
            svg.append("<line class=\"chart-grid\" x1=\"").append(left).append("\" x2=\"")
                    .append(width - right).append("\" y1=\"").append(y).append("\" y2=\"").append(y)
                    .append("\"/><text class=\"chart-axis\" x=\"").append(left - 10).append("\" y=\"")
                    .append(y + 4).append("\" text-anchor=\"end\">").append(formatChartNumber(value))
                    .append("</text>");
        }
        double groupWidth = plotWidth / Math.max(1, labels.size());
        double barWidth = Math.min(13, groupWidth * .2);
        int seriesIndex = 0;
        for (List<Double> values : series.values()) {
            for (int index = 0; index < values.size(); index++) {
                double value = values.get(index);
                double barHeight = plotHeight * value / max;
                double x = left + index * groupWidth + groupWidth / 2
                        + (seriesIndex - (series.size() - 1) / 2.0) * (barWidth + 2) - barWidth / 2;
                double y = top + plotHeight - barHeight;
                svg.append("<rect x=\"").append(x).append("\" y=\"").append(y)
                        .append("\" width=\"").append(barWidth).append("\" height=\"").append(barHeight)
                        .append("\" rx=\"3\" fill=\"").append(colors[seriesIndex % colors.length])
                        .append("\"><title>").append(escapeXml(labels.get(index))).append(": ")
                        .append(formatChartNumber(value)).append("</title></rect>");
            }
            seriesIndex++;
        }
        for (int index = 0; index < labels.size(); index++) {
            double x = left + index * groupWidth + groupWidth / 2;
            svg.append("<text class=\"chart-axis\" x=\"").append(x).append("\" y=\"")
                    .append(height - 20).append("\" text-anchor=\"middle\">")
                    .append(escapeXml(labels.get(index))).append("</text>");
        }
        svg.append("</svg><div class=\"admin-chart-legend\">");
        seriesIndex = 0;
        for (String label : series.keySet()) {
            svg.append(legendItem(colors[seriesIndex++ % colors.length], label));
        }
        return svg.append("</div>").toString();
    }

    private static String areaChart(List<String> labels, List<Double> values) {
        int width = 900;
        int height = 320;
        int left = 68;
        int right = 22;
        int top = 24;
        int bottom = 54;
        double plotWidth = width - left - right;
        double plotHeight = height - top - bottom;
        double max = values.stream().mapToDouble(Double::doubleValue).max().orElse(0);
        double step = chartStep(max);
        max = Math.max(step, Math.ceil(max / step) * step);
        StringBuilder svg = chartStart(width, height, "Biểu đồ miền doanh thu theo tháng");
        for (int tick = 0; tick <= 4; tick++) {
            double value = max * tick / 4;
            double y = top + plotHeight - plotHeight * tick / 4;
            svg.append("<line class=\"chart-grid\" x1=\"").append(left).append("\" x2=\"")
                    .append(width - right).append("\" y1=\"").append(y).append("\" y2=\"").append(y)
                    .append("\"/><text class=\"chart-axis\" x=\"").append(left - 8).append("\" y=\"")
                    .append(y + 4).append("\" text-anchor=\"end\">").append(formatCompactCurrency(value))
                    .append("</text>");
        }
        double xStep = plotWidth / Math.max(1, values.size() - 1);
        StringBuilder line = new StringBuilder();
        StringBuilder area = new StringBuilder();
        for (int index = 0; index < values.size(); index++) {
            double x = left + index * xStep;
            double y = top + plotHeight - plotHeight * values.get(index) / max;
            line.append(index == 0 ? "M " : " L ").append(x).append(' ').append(y);
            if (index == 0) {
                area.append("M ").append(x).append(' ').append(top + plotHeight).append(" L ")
                        .append(x).append(' ').append(y);
            } else {
                area.append(" L ").append(x).append(' ').append(y);
            }
        }
        double finalX = left + (values.size() - 1) * xStep;
        area.append(" L ").append(finalX).append(' ').append(top + plotHeight).append(" Z");
        svg.append("<defs><linearGradient id=\"revenue-area\" x1=\"0\" x2=\"0\" y1=\"0\" y2=\"1\">")
                .append("<stop offset=\"0%\" stop-color=\"#2563eb\" stop-opacity=\".28\"/>")
                .append("<stop offset=\"100%\" stop-color=\"#2563eb\" stop-opacity=\".02\"/>")
                .append("</linearGradient></defs><path d=\"").append(area)
                .append("\" fill=\"url(#revenue-area)\"/><path d=\"").append(line)
                .append("\" fill=\"none\" stroke=\"#2563eb\" stroke-width=\"3\" stroke-linecap=\"round\"/>");
        for (int index = 0; index < values.size(); index++) {
            double x = left + index * xStep;
            double y = top + plotHeight - plotHeight * values.get(index) / max;
            svg.append("<circle cx=\"").append(x).append("\" cy=\"").append(y)
                    .append("\" r=\"4\" fill=\"#fff\" stroke=\"#2563eb\" stroke-width=\"2\"><title>")
                    .append(escapeXml(labels.get(index))).append(": ")
                    .append(formatCompactCurrency(values.get(index))).append("</title></circle>")
                    .append("<text class=\"chart-axis\" x=\"").append(x).append("\" y=\"")
                    .append(height - 20).append("\" text-anchor=\"middle\">")
                    .append(escapeXml(labels.get(index))).append("</text>");
        }
        return svg.append("</svg>").toString();
    }

    private static String donutChart(Map<String, Long> values) {
        int width = 900;
        int height = 270;
        double total = values.values().stream().mapToLong(Long::longValue).sum();
        double circumference = 2 * Math.PI * 72;
        String[] colors = {"#f59e0b", "#2563eb", "#ef4444"};
        StringBuilder svg = chartStart(width, height, "Biểu đồ vành khuyên trạng thái yêu cầu thuê");
        svg.append("<g transform=\"rotate(-90 145 135)\">");
        double offset = 0;
        int index = 0;
        for (Map.Entry<String, Long> entry : values.entrySet()) {
            double length = total == 0 ? 0 : circumference * entry.getValue() / total;
            svg.append("<circle cx=\"145\" cy=\"135\" r=\"72\" fill=\"none\" stroke=\"")
                    .append(colors[index++ % colors.length]).append("\" stroke-width=\"26\"")
                    .append(" stroke-dasharray=\"").append(length).append(' ')
                    .append(circumference - length).append("\" stroke-dashoffset=\"")
                    .append(-offset).append("\"/>");
            offset += length;
        }
        svg.append("</g><text class=\"chart-donut-total\" x=\"145\" y=\"132\" text-anchor=\"middle\">")
                .append(formatChartNumber(total)).append("</text><text class=\"chart-axis\" x=\"145\" y=\"154\"")
                .append(" text-anchor=\"middle\">yêu cầu</text></svg><div class=\"admin-chart-legend\">");
        index = 0;
        for (Map.Entry<String, Long> entry : values.entrySet()) {
            svg.append(legendItem(colors[index++ % colors.length], entry.getKey() + " · " + entry.getValue()));
        }
        return svg.append("</div>").toString();
    }

    private static String horizontalBarChart(Map<String, Long> values) {
        int width = 900;
        int rowHeight = 42;
        int height = Math.max(150, 48 + values.size() * rowHeight);
        int left = 155;
        int right = 42;
        long max = Math.max(1, values.values().stream().mapToLong(Long::longValue).max().orElse(0));
        StringBuilder svg = chartStart(width, height, "Biểu đồ cột ngang số lượng xe theo phân khúc");
        int index = 0;
        for (Map.Entry<String, Long> entry : values.entrySet()) {
            int y = 22 + index * rowHeight;
            double barWidth = (width - left - right) * entry.getValue() / max;
            svg.append("<text class=\"chart-axis\" x=\"").append(left - 12).append("\" y=\"")
                    .append(y + 17).append("\" text-anchor=\"end\">")
                    .append(escapeXml(entry.getKey())).append("</text>")
                    .append("<rect x=\"").append(left).append("\" y=\"").append(y)
                    .append("\" width=\"").append(width - left - right).append("\" height=\"22\" rx=\"8\"")
                    .append(" fill=\"#eff4fb\"/><rect x=\"").append(left).append("\" y=\"").append(y)
                    .append("\" width=\"").append(Math.max(2, barWidth)).append("\" height=\"22\" rx=\"8\"")
                    .append(" fill=\"#14b8a6\"><title>").append(escapeXml(entry.getKey()))
                    .append(": ").append(entry.getValue()).append(" xe</title></rect>")
                    .append("<text class=\"chart-value\" x=\"").append(left + barWidth + 8)
                    .append("\" y=\"").append(y + 16).append("\">").append(entry.getValue()).append("</text>");
            index++;
        }
        if (values.isEmpty()) {
            svg.append("<text class=\"chart-axis\" x=\"24\" y=\"48\">Chưa có dữ liệu xe.</text>");
        }
        return svg.append("</svg>").toString();
    }

    private static StringBuilder chartStart(int width, int height, String label) {
        return new StringBuilder("<svg class=\"admin-chart-svg\" viewBox=\"0 0 ")
                .append(width).append(' ').append(height)
                .append("\" role=\"img\" aria-label=\"").append(escapeXml(label))
                .append("\" preserveAspectRatio=\"xMidYMid meet\">");
    }

    private static String legendItem(String color, String label) {
        return "<span class=\"admin-chart-legend-item\"><i style=\"--legend-color:" + color
                + "\"></i>" + escapeXml(label) + "</span>";
    }

    private static double chartStep(double max) {
        if (max <= 0) {
            return 1;
        }
        double rawStep = max / 4;
        double magnitude = Math.pow(10, Math.floor(Math.log10(rawStep)));
        double normalized = rawStep / magnitude;
        double nice = normalized <= 1 ? 1 : normalized <= 2 ? 2 : normalized <= 5 ? 5 : 10;
        return nice * magnitude;
    }

    private static String formatChartNumber(double value) {
        return String.format(Locale.ROOT, "%.0f", value);
    }

    private static String formatCompactCurrency(double value) {
        if (value >= 1_000_000) {
            return String.format(Locale.ROOT, "%.1f tr", value / 1_000_000).replace(".0", "");
        }
        if (value >= 1_000) {
            return String.format(Locale.ROOT, "%.0f k", value / 1_000);
        }
        return formatChartNumber(value);
    }

    private static String escapeXml(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
    }

    private void applyCarFilter(Grid<Map<String, Object>> grid, String query) {
        String normalized = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        grid.setItems(carData.stream().filter(item -> normalized.isBlank()
                || UiSupport.text(item, "name").toLowerCase(Locale.ROOT).contains(normalized)
                || UiSupport.text(item, "brand").toLowerCase(Locale.ROOT).contains(normalized)
                || UiSupport.text(item, "type").toLowerCase(Locale.ROOT).contains(normalized)).toList());
    }

    private static void clearCarForm(
            TextField name, TextField brand, TextField type, NumberField price, NumberField seats,
            ComboBox<String> transmission, ComboBox<String> fuel, NumberField year, TextField color,
            NumberField quantity, TextField image, TextArea description, Checkbox available) {
        name.clear();
        brand.clear();
        type.clear();
        price.clear();
        seats.clear();
        transmission.clear();
        fuel.clear();
        year.clear();
        color.clear();
        quantity.clear();
        image.clear();
        description.clear();
        available.setValue(true);
    }

    private static double asDouble(Object value) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (NumberFormatException exception) {
            return 0;
        }
    }

    private static String status(String value) {
        return switch (value.toLowerCase(Locale.ROOT)) {
            case "approved" -> "Đã duyệt";
            case "rejected" -> "Từ chối";
            case "new" -> "Chưa đọc";
            case "read" -> "Đã đọc";
            case "replied" -> "Đã phản hồi";
            case "pending" -> "Chờ xác nhận";
            default -> value.isBlank() ? "Không rõ" : value;
        };
    }
}
