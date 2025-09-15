package br.distributed.system.chat.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI chatOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Chat System API")
                        .version("1.0.0")
                        .description("APIs REST para chat distribuído. Disciplina de Sistemas Distribuídos (CEFET-MG)"));
    }

    @Bean
    public OpenApiCustomizer sortTagsAlphabetically() {
        return openApi -> {
            if (openApi.getTags() != null) {
                openApi.getTags().sort((a, b) -> a.getName().compareToIgnoreCase(b.getName()));
            }
        };
    }
}

