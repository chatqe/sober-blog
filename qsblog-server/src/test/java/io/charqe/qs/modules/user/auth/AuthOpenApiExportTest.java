package io.charqe.qs.modules.user.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springdoc.core.SpringDocConfigProperties;
import org.springdoc.core.SpringDocConfiguration;
import org.springdoc.webmvc.core.SpringDocWebMvcConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.http.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.sql.DataSource;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 只启动 MVC 和 SpringDoc；业务依赖全部 mock，不加载业务配置、任务、数据库或 Redis。 */
@SpringBootTest(classes = AuthOpenApiExportTest.ContractApp.class, properties = {
        "spring.config.name=auth-contract-isolated", "qiniu.downloadUrl=https://example.invalid/",
        "store.type=local", "springdoc.api-docs.enabled=true", "spring.main.banner-mode=off"
})
@AutoConfigureMockMvc(addFilters = false)
class AuthOpenApiExportTest {
    @Autowired MockMvc mvc;
    @Autowired ConfigurableListableBeanFactory beans;

    @Test
    void exportsCurrentControllerContractWithoutInfrastructure() throws Exception {
        assertEquals(0, beans.getBeanNamesForType(DataSource.class).length);
        assertEquals(0, beans.getBeanNamesForType(org.springframework.data.redis.connection.RedisConnectionFactory.class).length);
        byte[] json = mvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();
        var document = new ObjectMapper().readTree(json);
        var paths = document.path("paths");
        assertTrue(paths.has("/admin/user/login"));
        assertTrue(paths.has("/user/session"));
        assertTrue(paths.has("/admin/user/session"));
        assertTrue(paths.size() > 100);
        var schemas = document.path("components").path("schemas");
        for (String schema : new String[]{"FileCreateReqVO", "FileRespVO"}) {
            var properties = schemas.path(schema).path("properties");
            for (String field : new String[]{"originalName", "storagePath", "mimeType", "thirdPartyName", "fileSize", "cdnDomain"}) {
                assertTrue(properties.has(field), schema + "." + field);
            }
            for (String field : new String[]{"fileName", "filePath", "fileType", "storageType", "accessUrl", "cdnDomainValid", "ownerId", "fileHash"}) {
                assertFalse(properties.has(field), schema + "." + field);
            }
        }
        assertTrue(schemas.path("FileRespVO").path("properties").has("storedName"));
        assertFalse(schemas.path("FileCreateReqVO").path("properties").has("storedName"));
        Path output = Path.of("target", "auth-openapi.json");
        Files.createDirectories(output.getParent());
        Files.write(output, json);
    }

    @Configuration(proxyBeanMethods = false)
    @ImportAutoConfiguration({JacksonAutoConfiguration.class, HttpMessageConvertersAutoConfiguration.class,
            ValidationAutoConfiguration.class, WebMvcAutoConfiguration.class,
            SpringDocConfiguration.class, SpringDocWebMvcConfiguration.class})
    @EnableConfigurationProperties(SpringDocConfigProperties.class)
    static class ContractApp {
        @Bean
        static BeanDefinitionRegistryPostProcessor controllerMocks() {
            return new BeanDefinitionRegistryPostProcessor() {
                private final Map<Class<?>, String> dependencies = new HashMap<>();
                @Override
                public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) {
                    var scanner = new ClassPathScanningCandidateComponentProvider(false);
                    scanner.addIncludeFilter(new AnnotationTypeFilter(RestController.class));
                    for (var candidate : scanner.findCandidateComponents("io.charqe.qs.modules")) {
                        try {
                            Class<?> controller = Class.forName(candidate.getBeanClassName());
                            registry.registerBeanDefinition(controller.getName(), new RootBeanDefinition(controller));
                            for (var field : controller.getDeclaredFields()) {
                                Resource resource = field.getAnnotation(Resource.class);
                                if (resource != null || field.isAnnotationPresent(Autowired.class)) {
                                    dependencies.put(field.getType(), resource != null && !resource.name().isEmpty()
                                            ? resource.name() : field.getName());
                                }
                            }
                            for (var constructor : controller.getDeclaredConstructors()) {
                                for (Class<?> type : constructor.getParameterTypes()) {
                                    dependencies.putIfAbsent(type, type.getName());
                                }
                            }
                        } catch (ClassNotFoundException e) { throw new IllegalStateException(e); }
                    }
                }
                @Override
                public void postProcessBeanFactory(ConfigurableListableBeanFactory factory) {
                    dependencies.forEach((type, name) -> factory.registerSingleton(name, mock(type)));
                }
            };
        }
    }
}
