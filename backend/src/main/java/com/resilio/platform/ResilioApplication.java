package com.resilio.platform;

import com.resilio.platform.model.Deployment;
import com.resilio.platform.repository.DeploymentRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.time.Instant;

@SpringBootApplication
public class ResilioApplication {

    public static void main(String[] args) {
        SpringApplication.run(ResilioApplication.class, args);
    }

    @Bean
    public CommandLineRunner initBaseline(DeploymentRepository deploymentRepo) {
        return args -> {
            if (deploymentRepo.count() == 0) {
                Deployment baseline = new Deployment(
                        "v1.0.0",
                        "a1b2c3d4e5f6",
                        "BLUE",
                        "resilio/demo-app:1.0.0"
                );
                baseline.setStatus("ACTIVE");
                baseline.setKnownGood(true);
                baseline.setPromotedAt(Instant.now());
                baseline.setVerifiedAt(Instant.now());
                deploymentRepo.save(baseline);
            }
        };
    }
}
