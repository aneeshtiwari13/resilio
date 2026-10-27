package com.resilio.platform.security;

import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserAccountRepository userRepo;

    public AuthController(AuthenticationManager authenticationManager, UserAccountRepository userRepo) {
        this.authenticationManager = authenticationManager;
        this.userRepo = userRepo;
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, String> credentials) {
        String username = credentials.getOrDefault("username", "");
        String password = credentials.getOrDefault("password", "");

        Map<String, Object> resp = new HashMap<>();

        try {
            Authentication auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, password)
            );
            SecurityContextHolder.getContext().setAuthentication(auth);

            var account = userRepo.findByUsername(username).orElseThrow();

            resp.put("authenticated", true);
            resp.put("username", account.getUsername());
            resp.put("role", account.getRole());
            resp.put("message", "Login successful");
            return ResponseEntity.ok(resp);

        } catch (BadCredentialsException e) {
            resp.put("authenticated", false);
            resp.put("error", "Invalid username or password");
            return ResponseEntity.status(401).body(resp);
        }
    }

    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> getCurrentUser(Authentication authentication) {
        Map<String, Object> resp = new HashMap<>();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            resp.put("authenticated", false);
            resp.put("username", "anonymous");
            resp.put("role", "ROLE_ANONYMOUS");
            return ResponseEntity.ok(resp);
        }

        String username = authentication.getName();
        var accountOpt = userRepo.findByUsername(username);

        resp.put("authenticated", true);
        resp.put("username", username);
        resp.put("role", accountOpt.map(UserAccount::getRole).orElse("ROLE_VIEWER"));
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getAuthStatus() {
        Map<String, Object> resp = new HashMap<>();
        resp.put("securityEnabled", true);
        resp.put("rolesSupported", new String[]{"ROLE_VIEWER", "ROLE_OPERATOR", "ROLE_ADMIN"});
        return ResponseEntity.ok(resp);
    }
}
