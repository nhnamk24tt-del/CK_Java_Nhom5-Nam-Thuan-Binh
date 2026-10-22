package com.rentwheels.backend.ui;

import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.Route;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Route(value = "vehicles", layout = MainLayout.class)
public class VehicleListView extends VerticalLayout {
    private final VerticalLayout vehicleGrid = new VerticalLayout();
    private final List<Map<String, Object>> allCars;

    public VehicleListView(FrontendService service) {
        addClassName("page");
        setPadding(false);
        allCars = service.cars();

        TextField search = new TextField("Tìm xe");
        search.setPlaceholder("Tên xe hoặc hãng xe");
        search.setValueChangeMode(ValueChangeMode.EAGER);
        ComboBox<String> category = new ComboBox<>("Danh mục");
        category.setItems(allCars.stream().map(car -> UiSupport.text(car, "type"))
                .filter(type -> !type.isBlank()).distinct().sorted().toList());
        category.setClearButtonVisible(true);
        search.addValueChangeListener(event -> refresh(event.getValue(), category.getValue()));
        category.addValueChangeListener(event -> refresh(search.getValue(), event.getValue()));

        H1 title = new H1("Danh sách xe");
        vehicleGrid.addClassName("vehicle-grid");
        vehicleGrid.setPadding(false);
        vehicleGrid.setSpacing(false);
        add(title, search, category, vehicleGrid);
        refresh("", null);
    }

    private void refresh(String query, String type) {
        String normalized = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        vehicleGrid.removeAll();
        List<Map<String, Object>> filtered = allCars.stream()
                .filter(car -> type == null || type.isBlank() || type.equals(UiSupport.text(car, "type")))
                .filter(car -> normalized.isBlank()
                        || (UiSupport.text(car, "name") + " " + UiSupport.text(car, "brand"))
                                .toLowerCase(Locale.ROOT).contains(normalized))
                .toList();
        if (filtered.isEmpty()) {
            vehicleGrid.add(new com.vaadin.flow.component.html.Paragraph("Không tìm thấy xe phù hợp."));
            return;
        }
        filtered.forEach(car -> vehicleGrid.add(new VehicleCard(car)));
    }
}
