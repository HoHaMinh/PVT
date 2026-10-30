package com.hoaphat.pvt.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.price-lookup")
public class PriceLookupProperties {
    private boolean enabled;
    private String webAppUrl = "";
    private String sharedToken = "";
    private int connectTimeoutSeconds = 10;
    private int readTimeoutSeconds = 330;

    public boolean isConfigured() {
        return enabled && webAppUrl != null && !webAppUrl.trim().isEmpty()
                && sharedToken != null && !sharedToken.trim().isEmpty();
    }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getWebAppUrl() { return webAppUrl; }
    public void setWebAppUrl(String webAppUrl) { this.webAppUrl = webAppUrl; }
    public String getSharedToken() { return sharedToken; }
    public void setSharedToken(String sharedToken) { this.sharedToken = sharedToken; }
    public int getConnectTimeoutSeconds() { return connectTimeoutSeconds; }
    public void setConnectTimeoutSeconds(int connectTimeoutSeconds) { this.connectTimeoutSeconds = connectTimeoutSeconds; }
    public int getReadTimeoutSeconds() { return readTimeoutSeconds; }
    public void setReadTimeoutSeconds(int readTimeoutSeconds) { this.readTimeoutSeconds = readTimeoutSeconds; }
}
