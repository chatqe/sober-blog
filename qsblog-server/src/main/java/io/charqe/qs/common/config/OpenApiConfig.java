package io.charqe.qs.common.config;


import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenApi配置类
 *
 * @author sjq
 * @since 2026-01-01 18:25
 */
@Configuration
public class OpenApiConfig {

    @Value("${springdoc.info.title}")
    private String title;
    @Value("${springdoc.info.description}")
    private String description;
    @Value("${springdoc.info.version}")
    private String version;
    @Value("${springdoc.info.contact.name}")
    private String name;
    @Value("${springdoc.info.contact.email}")
    private String email;


    @Bean
    public OpenAPI apiInfo() {
        return new OpenAPI()
                .info(new Info()
                        .title(title)
                        .version(version)
                        .description(description)
                        .contact(new Contact()
                                .name(name)
                                .email(email)))
                .addServersItem(new Server()
                        .url("http://localhost:18081").description("本地"))
                .addServersItem(new Server()
                        .url("https://api.xxx.com").description("生产"));
        // 3. 安全方案
        // .components(new Components()                      // 1. 组件定义区：放公共的、可复用的 schema、参数、安全方案等
        //         .addSecuritySchemes("Bearer",             // 2. 给这个安全方案起个名字“Bearer”，后续接口直接引用
        //                 new SecurityScheme()
        //                         .type(SecurityScheme.Type.HTTP)    // 3. 认证类型：HTTP 认证（还有 APIKEY、OAUTH2、OPENIDCONNECT）
        //                         .scheme("bearer")                  // 4. 子类型：bearer 令牌（RFC 6750 标准）
        //                         .bearerFormat("JWT")));            // 5. 提示格式：JWT（Swagger UI 会显示“JWT”字样，非必需但友好）
    }

    /**
     * 公共前缀 /api 统一抽出来，避免每个 Controller 重复写
     */
    // @Bean
    // public GroupedOpenApi publicApi() {
    //     return GroupedOpenApi.builder()
    //             .group("public")
    //             .pathsToMatch("/api/**")
    //             .build();
    // }
    //
    // /**
    //  * 如果不想按路径，也可以按 Controller 所在包分组：
    //  *
    //  * @return
    //  */
    // @Bean
    // public GroupedOpenApi userApi() {
    //     return GroupedOpenApi.builder()
    //             .group("用户中心")
    //             .packagesToScan("com.xxx.user.adapter.web")   // 只扫描 user 模块
    //             .build();
    // }
}
