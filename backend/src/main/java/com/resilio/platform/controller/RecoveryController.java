package com.resilio.platform.controller;

import com.resilio.platform.model.RecoveryEvent;
import com.resilio.platform.repository.RecoveryEventRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/recovery")
public class RecoveryController {

    private final RecoveryEventRepository recoveryRepo;

    public RecoveryController(RecoveryEventRepository recoveryRepo) {
        this.recoveryRepo = recoveryRepo;
    }

    @GetMapping("/events")
    public ResponseEntity<List<RecoveryEvent>> getEvents() {
        return ResponseEntity.ok(recoveryRepo.findAllByOrderByStartedAtDesc());
    }

    @GetMapping("/events/{id}")
    public ResponseEntity<RecoveryEvent> getEventById(@PathVariable Long id) {
        return recoveryRepo.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
