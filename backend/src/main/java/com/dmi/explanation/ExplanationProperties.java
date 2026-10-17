package com.dmi.explanation;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "dmi.explanation")
public class ExplanationProperties {

    private boolean enabled;
    /** When false (default), harness providers run synchronously without timeout executor. */
    private boolean realInferenceEnabled;
    private String provider = "disabled";
    private long timeoutMs = 5_000;
    private int maxSummaryChars = 2_000;
    private int maxClaimChars = 800;
    private int maxClaims = 12;

    public boolean enabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean realInferenceEnabled() {
        return realInferenceEnabled;
    }

    public void setRealInferenceEnabled(boolean realInferenceEnabled) {
        this.realInferenceEnabled = realInferenceEnabled;
    }

    public String provider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public long timeoutMs() {
        return timeoutMs;
    }

    public void setTimeoutMs(long timeoutMs) {
        this.timeoutMs = timeoutMs;
    }

    public int maxSummaryChars() {
        return maxSummaryChars;
    }

    public void setMaxSummaryChars(int maxSummaryChars) {
        this.maxSummaryChars = maxSummaryChars;
    }

    public int maxClaimChars() {
        return maxClaimChars;
    }

    public void setMaxClaimChars(int maxClaimChars) {
        this.maxClaimChars = maxClaimChars;
    }

    public int maxClaims() {
        return maxClaims;
    }

    public void setMaxClaims(int maxClaims) {
        this.maxClaims = maxClaims;
    }
}
