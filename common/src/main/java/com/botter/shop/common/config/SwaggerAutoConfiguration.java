package com.botter.shop.common.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @ProjectName  botter-shop-mic
 * @Author       Botter
 * @Description  Swagger 公共自动配置，放在 common 模块中。
 *               各服务只要引入 common 依赖 + springdoc 依赖，即可自动获得 Swagger UI，
 *               无需再手动编写 Knife4jConfiguration。
 *               若服务需要自定义 OpenAPI 信息，只需自行声明同名 Bean 即可覆盖（@ConditionalOnMissingBean）。
 */
@Configuration
@ConditionalOnClass(OpenAPI.class)   // 仅当 springdoc 在 classpath 时生效
public class SwaggerAutoConfiguration {

    /** 自动读取 spring.application.name 作为 API 标题前缀 */
    @Value("${spring.application.name:Service}")
    private String applicationName;

    /**
     * 默认 OpenAPI 信息 Bean。
     * 若服务自行定义了 OpenAPI Bean，则本 Bean 不会创建（@ConditionalOnMissingBean）。
     */
    @Bean
    @ConditionalOnMissingBean(OpenAPI.class)
    public OpenAPI defaultOpenApi() {
        String title = Character.toUpperCase(applicationName.charAt(0))
                + applicationName.substring(1).replace("-", " ");
        return new OpenAPI().info(new Info()
                .title(title + " API")
                .description(applicationName + " 服务接口文档")
                .version("1.0"));
    }

    /**
     * 默认 GroupedOpenApi Bean，扫描 com.botter.shop 下所有 controller。
     * 若服务需要更精细的分组，自行声明 GroupedOpenApi Bean 即可。
     */
    @Bean
    @ConditionalOnMissingBean(GroupedOpenApi.class)
    public GroupedOpenApi defaultGroupedOpenApi() {
        return GroupedOpenApi.builder()
                .group(applicationName)
                .packagesToScan("com.botter.shop")
                .build();
    }
}
