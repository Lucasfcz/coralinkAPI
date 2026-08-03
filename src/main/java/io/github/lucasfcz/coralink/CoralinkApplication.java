package io.github.lucasfcz.coralink;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class CoralinkApplication {

    public static void main(String[] args) {
        SpringApplication.run(CoralinkApplication.class, args);
    }
}