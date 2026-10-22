package com.rentwheels.backend.ui;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import java.util.Map;

final class VehicleCard extends VerticalLayout {
    VehicleCard(Map<String, Object> car) {
        this(car, null);
    }

    VehicleCard(Map<String, Object> car, Runnable removeFromFavorites) {
        addClassName("vehicle-card");
        setPadding(false);
        setSpacing(true);
        add(UiSupport.carImage(car));

        H3 name = new H3(UiSupport.text(car, "name"));
        Paragraph details = new Paragraph(
                String.join(" · ",
                        UiSupport.text(car, "brand"),
                        UiSupport.text(car, "type"),
                        UiSupport.text(car, "seats") + " chỗ"));
        details.addClassName("muted");
        Paragraph price = new Paragraph(UiSupport.currency(car.get("price")) + " / ngày");
        price.addClassName("price");
        Button view = new Button("Xem chi tiết", event ->
                UI.getCurrent().navigate("vehicles/" + UiSupport.id(car)));
        view.addClassName("primary");
        add(name, details, price, view);
        if (removeFromFavorites != null) {
            add(new Button("Xóa khỏi yêu thích", event -> removeFromFavorites.run()));
        }
    }
}
