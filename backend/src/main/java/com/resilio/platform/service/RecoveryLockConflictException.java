package com.resilio.platform.service;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class RecoveryLockConflictException extends RuntimeException {

    private final String existingAttemptId;
    private final String lockOwner;

    public RecoveryLockConflictException(String message, String existingAttemptId, String lockOwner) {
        super(message);
        this.existingAttemptId = existingAttemptId;
        this.lockOwner = lockOwner;
    }

    public String getExistingAttemptId() { return existingAttemptId; }
    public String getLockOwner() { return lockOwner; }
}
