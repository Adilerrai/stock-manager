package com;

import com.acommon.persistant.model.Role;
import com.acommon.persistant.model.User;
import com.acommon.repository.RoleRepository;
import com.acommon.repository.UserRepository;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.PropertySource;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootApplication
@EntityScan("com")
@EnableJpaRepositories("com")
public class PointVenteApplication {
 

    public static void main(String[] args) {
        SpringApplication.run(PointVenteApplication.class, args);
    }

}
