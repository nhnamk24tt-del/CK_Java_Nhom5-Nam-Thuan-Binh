package com.rentwheels.backend;

import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.theme.Theme;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(AppProperties.class)
@Theme("rentwheels")
public class RentwheelsApplication implements AppShellConfigurator {
    public static void main(String[] args) {
        SpringApplication.run(RentwheelsApplication.class, args);
    }
}
