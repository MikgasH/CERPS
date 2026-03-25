package org.example.analyticsservice;

import org.example.analyticsservice.config.NativeImageConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ImportRuntimeHints;

@SpringBootApplication
@ComponentScan(basePackages = {"org.example.analyticsservice", "com.example.cerps.common"})
@ImportRuntimeHints(NativeImageConfig.class)
public class AnalyticsServiceApplication {

    public static void main(final String[] args) {
        SpringApplication.run(AnalyticsServiceApplication.class, args);
    }

}
