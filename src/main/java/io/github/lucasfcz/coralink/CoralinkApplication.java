package io.github.lucasfcz.coralink;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(exclude = {org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration.class})
@EnableScheduling
@EnableCaching
public class CoralinkApplication {

    public static void main(String[] args) {
        SpringApplication.run(CoralinkApplication.class, args);
    }
}