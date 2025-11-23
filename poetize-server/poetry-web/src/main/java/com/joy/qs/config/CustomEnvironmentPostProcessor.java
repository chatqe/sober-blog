package com.joy.qs.config;

import com.joy.qs.handle.BlogRuntimeException;
import lombok.SneakyThrows;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

/**
 * 在应用启动时从数据库加载配置，并将这些配置添加到 Spring Boot 应用的环境配置中。
 * 此外，它还负责检查数据库是否存在，并在不存在时初始化数据库。
 *
 * @author Upjqs
 * @since 2024-08-19 14:25
 */
@Order(Ordered.LOWEST_PRECEDENCE)
@ConditionalOnProperty(name = "custom.environment.processor.enabled", havingValue = "true")
public class CustomEnvironmentPostProcessor implements EnvironmentPostProcessor {

    private static final String SOURCE_NAME = "sys_config";

    private static final String SOURCE_SQL = "select * from poetize.sys_config";

    private static final String DATABASE = "poetize";

    private static final String sqlPath = "file:/home/poetry.sql";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {

        System.out.println("==============> 进入了 CustomEnvironmentPostProcessor");
        try {
            Map<String, Object> map = new HashMap<>();

            String username = environment.getProperty("spring.datasource.username");
            String password = environment.getProperty("spring.datasource.password");
            String url = environment.getProperty("spring.datasource.url").replace("/poetize", "");
            String driver = environment.getProperty("spring.datasource.driver-class-name");
            Class.forName(driver);
            try (Connection connection = DriverManager.getConnection(url, username, password)) {
                //初始化数据库
                initDb(connection);
                //加载配置文件
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
            throw new BlogRuntimeException(e);
        }
    }

    @SneakyThrows
    private void initDb(Connection connection) {
        try (Statement statement = connection.createStatement()) {
            try (ResultSet resultSet = statement.executeQuery("SHOW DATABASES LIKE '" + DATABASE + "'")) {
                if (!resultSet.next()) {
                    ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
                    ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
                    populator.addScripts(resolver.getResources(sqlPath));
                    populator.populate(connection);
                }
            }
        }
    }
}
