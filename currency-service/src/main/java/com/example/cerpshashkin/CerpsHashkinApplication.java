package com.example.cerpshashkin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@EnableCaching
@SpringBootApplication
@ComponentScan(basePackages = {"com.example.cerpshashkin", "com.example.cerps.common"})
public class CerpsHashkinApplication {

    private CerpsHashkinApplication() {
    }

    public static void main(final String[] args) {
        SpringApplication.run(CerpsHashkinApplication.class, args);
    }
}
