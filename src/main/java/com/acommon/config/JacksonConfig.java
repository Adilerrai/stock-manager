package com.acommon.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.hibernate.proxy.HibernateProxy;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JacksonConfig {

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jacksonCustomizer() {
        return builder -> {
            // Ignorer automatiquement les propriétés internes de proxy Hibernate sur toutes les entités
            builder.mixIn(HibernateProxy.class, HibernateProxyMixin.class);
            builder.failOnEmptyBeans(false);
        };
    }

    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "byteBuddyInterceptor"})
    private interface HibernateProxyMixin {
    }
}
