package com.resilio.demo.service;

import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class ChaosStateService {

    public enum FailureMode {
        NONE,
        UNHEALTHY_500,
        LATENCY_SPIKE,
        FUNCTIONAL_REGRESSION
    }

    private final AtomicReference<FailureMode> activeMode = new AtomicReference<>(FailureMode.NONE);
    private final AtomicInteger failureCounter = new AtomicInteger(0);
    private final AtomicBoolean regressionInduced = new AtomicBoolean(false);

    public FailureMode getActiveMode() {
        return activeMode.get();
    }

    public void setMode(FailureMode mode) {
        this.activeMode.set(mode);
        if (mode == FailureMode.FUNCTIONAL_REGRESSION) {
            this.regressionInduced.set(true);
        }
    }

    public void reset() {
        this.activeMode.set(FailureMode.NONE);
        this.failureCounter.set(0);
        this.regressionInduced.set(false);
    }

    public boolean isUnhealthy() {
        return activeMode.get() == FailureMode.UNHEALTHY_500;
    }

    public boolean isLatencySpike() {
        return activeMode.get() == FailureMode.LATENCY_SPIKE;
    }

    public boolean isRegressionInduced() {
        return regressionInduced.get() || activeMode.get() == FailureMode.FUNCTIONAL_REGRESSION;
    }

    public void recordCheck() {
        if (isUnhealthy()) {
            failureCounter.incrementAndGet();
        }
    }

    public int getFailureCount() {
        return failureCounter.get();
    }
}
