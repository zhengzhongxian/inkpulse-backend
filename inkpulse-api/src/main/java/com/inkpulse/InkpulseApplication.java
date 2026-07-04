package com.inkpulse;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
@ConfigurationPropertiesScan
public class InkpulseApplication {

    public static void main(String[] args) {
        String env = System.getenv("APP_ENV");
        if (env == null) {
            env = System.getProperty("app.env", "dev");
        }

        if (!"dev".equalsIgnoreCase(env)) {
            System.setProperty("springdoc.api-docs.enabled", "false");
            System.setProperty("springdoc.swagger-ui.enabled", "false");
        } else {
            System.setProperty("springdoc.api-docs.enabled", "true");
            System.setProperty("springdoc.swagger-ui.enabled", "true");
        }

        SpringApplication.run(InkpulseApplication.class, args);
    }
}
