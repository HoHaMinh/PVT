package com.hoaphat.pvt.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.debt-alert")
public class DebtAlertProperties {
    private boolean enabled = false;
    private String webAppUrl = "";
    private String sharedToken = "";
    private int connectTimeoutSeconds = 10;
    private int readTimeoutSeconds = 330;
    private long startupDelayMs = 10000L;
    private long retryDelayMs = 900000L;
    private int earliestSyncHour = 7;
    private int taskDeadlineHour = 16;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getWebAppUrl() {
        return webAppUrl;
    }

    public void setWebAppUrl(String webAppUrl) {
        this.webAppUrl = webAppUrl;
    }

    public String getSharedToken() {
        return sharedToken;
    }

    public void setSharedToken(String sharedToken) {
        this.sharedToken = sharedToken;
    }

    public int getConnectTimeoutSeconds() {
        return connectTimeoutSeconds;
    }

    public void setConnectTimeoutSeconds(int connectTimeoutSeconds) {
        this.connectTimeoutSeconds = connectTimeoutSeconds;
    }

    public int getReadTimeoutSeconds() {
        return readTimeoutSeconds;
    }

    public void setReadTimeoutSeconds(int readTimeoutSeconds) {
        this.readTimeoutSeconds = readTimeoutSeconds;
    }

    public long getStartupDelayMs() {
        return startupDelayMs;
    }

    public void setStartupDelayMs(long startupDelayMs) {
        this.startupDelayMs = startupDelayMs;
    }

    public long getRetryDelayMs() {
        return retryDelayMs;
    }

    public void setRetryDelayMs(long retryDelayMs) {
        this.retryDelayMs = retryDelayMs;
    }

    public int getEarliestSyncHour() {
        return earliestSyncHour;
    }

    public void setEarliestSyncHour(int earliestSyncHour) {
        this.earliestSyncHour = earliestSyncHour;
    }

    public int getTaskDeadlineHour() {
        return taskDeadlineHour;
    }

    public void setTaskDeadlineHour(int taskDeadlineHour) {
        this.taskDeadlineHour = taskDeadlineHour;
    }

    public boolean isConfigured() {
        return enabled
                && webAppUrl != null && !webAppUrl.trim().isEmpty()
                && sharedToken != null && !sharedToken.trim().isEmpty();
    }
}
