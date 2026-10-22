package com.rentwheels.backend.ui;

import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.router.RouterLink;
import org.springframework.beans.factory.annotation.Value;

public class MainLayout extends AppLayout implements BeforeEnterObserver {
    private final boolean adminInstance;

    public MainLayout(
            UserSession session,
            @Value("${app.instance-role:user}") String instanceRole) {
        adminInstance = "admin".equals(instanceRole);
        if (adminInstance) {
            H1 logo = new H1("RentWheels Admin");
            logo.addClassName("brand");
            HorizontalLayout header = new HorizontalLayout(logo);
            header.setDefaultVerticalComponentAlignment(HorizontalLayout.Alignment.CENTER);
            header.setWidthFull();
            header.expand(logo);
            if (session.isAdmin()) {
                header.add(new Span("Xin chào, " + session.getUser().getOrDefault("name", "quản trị viên")));
                header.add(new Button("Đăng xuất", event -> {
                    session.signOut();
                    UI.getCurrent().getPage().reload();
                }));
            } else {
                header.add(new Span("Đăng nhập quản trị viên"));
            }
            addToNavbar(header);
            return;
        }

        DrawerToggle toggle = new DrawerToggle();
        H1 logo = new H1("RentWheels");
        logo.addClassName("brand");
        HorizontalLayout header = new HorizontalLayout(toggle, logo);
        header.setDefaultVerticalComponentAlignment(HorizontalLayout.Alignment.CENTER);
        header.setWidthFull();
        header.expand(logo);

        if (session.isAuthenticated()) {
            Span greeting = new Span("Xin chào, " + session.getUser().getOrDefault("name", "bạn"));
            greeting.addClassName("user-greeting");
            RouterLink account = new RouterLink("Tài khoản", ProfileView.class);
            header.add(greeting, account);
        } else {
            header.add(new RouterLink("Đăng nhập", LoginView.class));
        }
        addToNavbar(header);

        SideNav navigation = new SideNav();
        navigation.addItem(new SideNavItem("Trang chủ", HomeView.class));
        navigation.addItem(new SideNavItem("Thuê xe", VehicleListView.class));
        navigation.addItem(new SideNavItem("Liên hệ", ContactView.class));
        navigation.addItem(new SideNavItem("Yêu thích", WishlistView.class));
        navigation.addItem(new SideNavItem("Đơn thuê", BookingHistoryView.class));
        addToDrawer(navigation);
        setPrimarySection(Section.DRAWER);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if (adminInstance && event.getNavigationTarget() != AdminView.class) {
            event.forwardTo(AdminView.class);
        }
    }
}
