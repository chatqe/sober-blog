package com.ld.poetry.config;

import com.ld.poetry.handle.PoetryRuntimeException;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;

@Order(Ordered.LOWEST_PRECEDENCE)
public class CustomEnvironmentPostProcessor implements EnvironmentPostProcessor {

    private static final String SOURCE_NAME = "sys_config";

    private static final String SOURCE_SQL = "select * from sys_config";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        try {
            Map<String, Object> map = new HashMap<>();

            String username = environment.getProperty("spring.datasource.username");
            String password = environment.getProperty("spring.datasource.password");
            String url = environment.getProperty("spring.datasource.url");
            String driver = environment.getProperty("spring.datasource.driver-class-name");
            Class.forName(driver);
            try (Connection connection = DriverManager.getConnection(url, username, password)) {
                try (Statement statement = connection.createStatement()) {
                    try (ResultSet resultSet = statement.executeQuery(SOURCE_SQL)) {
                        while (resultSet.next()) {
                            map.put(resultSet.getString("config_key"), resultSet.getString("config_value"));
                        }
                    }
                }
            }

            MutablePropertySources propertySources = environment.getPropertySources();
            PropertySource<?> source = new MapPropertySource(SOURCE_NAME, map);
            propertySources.addFirst(source);
        } catch (Exception e) {
            throw new PoetryRuntimeException(e);
        }
    }
}
