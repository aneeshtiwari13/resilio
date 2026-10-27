package com.resilio.platform.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SecurityBootstrapService {

    private static final Logger log = LoggerFactory.getLogger(SecurityBootstrapService.class);

    private final UserAccountRepository userRepo;
    private final PasswordEncoder passwordEncoder;

    @Value("${resilio.security.admin-password:Admin@Resilio2026!}")
    private String adminPassword;

    @Value("${resilio.security.operator-password:Operator@Resilio2026!}")
    private String operatorPassword;

    @Value("${resilio.security.viewer-password:Viewer@Resilio2026!}")
    private String viewerPassword;

    public SecurityBootstrapService(UserAccountRepository userRepo, PasswordEncoder passwordEncoder) {
        this.userRepo = userRepo;
        this.passwordEncoder = passwordEncoder;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void bootstrapDefaultUsers() {
        seedUserIfMissing("admin", adminPassword, "ROLE_ADMIN");
        seedUserIfMissing("operator", operatorPassword, "ROLE_OPERATOR");
        seedUserIfMissing("viewer", viewerPassword, "ROLE_VIEWER");
    }

    private void seedUserIfMissing(String username, String rawPassword, String role) {
        if (!userRepo.existsByUsername(username)) {
            UserAccount account = new UserAccount(
                    username,
                    passwordEncoder.encode(rawPassword),
                    role,
                    true
            );
            userRepo.save(account);
            log.info("Bootstrapped default security account: username={}, role={}", username, role);
        }
    }
}
