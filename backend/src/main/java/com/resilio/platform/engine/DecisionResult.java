package com.resilio.platform.engine;

public class DecisionResult {
    private final FailureCategory category;
    private final RecoveryAction action;
    private final String rationale;
    private final boolean requiresVerification;
    private final String requiredVerificationStep;

    public DecisionResult(FailureCategory category, RecoveryAction action, String rationale,
                          boolean requiresVerification, String requiredVerificationStep) {
        this.category = category;
        this.action = action;
        this.rationale = rationale;
        this.requiresVerification = requiresVerification;
        this.requiredVerificationStep = requiredVerificationStep;
    }

    public FailureCategory getCategory() { return category; }
    public RecoveryAction getAction() { return action; }
    public String getRationale() { return rationale; }
    public boolean isRequiresVerification() { return requiresVerification; }
    public String getRequiredVerificationStep() { return requiredVerificationStep; }
}
