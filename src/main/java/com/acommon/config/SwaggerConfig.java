package com.acommon.config;


import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("pointvent API")
                        .description("API pour la plateforme SaaS de gestion des cabinets d'avocats")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Équipe pointvent")
                                .email("contact@pointvent.ma")
                                .url("https://pointvent.ma"))
                        .license(new License()
                                .name("Propriétaire")
                                .url("https://pointvent.ma/license")))
                .addSecurityItem(new SecurityRequirement().addList("JWT"))
                .components(new Components()
                        .addSecuritySchemes("JWT", new SecurityScheme()
                                .name("JWT")
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .in(SecurityScheme.In.HEADER)
                                .description("Entrez votre token JWT avec le préfixe Bearer")));
    }
}
