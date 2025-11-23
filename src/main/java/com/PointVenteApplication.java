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
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Logger logger = org.slf4j.LoggerFactory.getLogger(PointVenteApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(PointVenteApplication.class, args);
    }


    @Bean
    CommandLineRunner runner() {
        Role role;
        if(roleRepository.existsByNom(("ADMIN"))){
           role = roleRepository.findByNom("ADMIN").get();
            logger.info("Role ADMIN already exists");
        } else{
           role = new Role();
           role.setNom("ADMIN");
           roleRepository.save(role);
           logger.info("Role ADMIN already exists");
        }



        User user;

        if(userRepository.existsByUsername(("admin"))){
            user = userRepository.findByUsername("admin").get();
            logger.info("User admin already exists");
        } else{
            user = new User();
            user.setUsername("admin2");
            user.setPassword(passwordEncoder.encode("admin"));
            user.setEmail("admin2@me.com");
            user.setNomComplet("Admin Admin");
            user.setTelephone("+1234567890");
            user.setRole(role);
            userRepository.save(user);
            logger.info("User save successful");
        }



        return args -> {
            // Code to run at startup
        };
    }
}
