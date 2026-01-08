package org.example.userservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"org.example.userservice", "com.example.cerps.common"})
public class UserServiceApplication {

    public static void main(final String[] args) {
        SpringApplication.run(UserServiceApplication.class, args);
    }

}
