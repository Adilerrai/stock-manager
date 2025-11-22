package com;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.PropertySource;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EntityScan("com")
@EnableJpaRepositories("com")
public class PointVenteApplication {

    public static void main(String[] args) {
        SpringApplication.run(PointVenteApplication.class, args);
    }
}
