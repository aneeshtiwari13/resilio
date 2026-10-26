package com.resilio.platform;

import com.resilio.platform.service.ReleaseRegistryService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class ResilioApplication {

    public static void main(String[] args) {
        SpringApplication.run(ResilioApplication.class, args);
    }

    @Bean
    public CommandLineRunner initReleaseRegistry(ReleaseRegistryService releaseRegistry) {
        return args -> {
            releaseRegistry.reconcileStateOnStartup();
        };
    }
}
