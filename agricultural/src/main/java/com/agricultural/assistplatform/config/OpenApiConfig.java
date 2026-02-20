package com.agricultural.assistplatform.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Swagger/OpenAPI 配置（与接口文档一致 Base URL: http://localhost:8080/api）
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .openapi("3.0.1")
                .info(new Info()
                        .title("助农产品销售系统 API")
                        .description("用户端 / 商家端 / 管理员端 RESTful 接口")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("助农平台")
                                .email("support@agricultural.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0.html")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8080/api")
                                .description("本地开发环境")
                ));
    }
}
