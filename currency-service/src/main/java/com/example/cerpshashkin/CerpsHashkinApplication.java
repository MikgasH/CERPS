package com.example.cerpshashkin;

import com.example.cerpshashkin.config.NativeImageConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ImportRuntimeHints;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
@ComponentScan(basePackages = {"com.example.cerpshashkin", "com.example.cerps.common"})
@ImportRuntimeHints(NativeImageConfig.class)
public class CerpsHashkinApplication {

    private CerpsHashkinApplication() {
    }

    public static void main(final String[] args) {
        SpringApplication.run(CerpsHashkinApplication.class, args);
    }
}
