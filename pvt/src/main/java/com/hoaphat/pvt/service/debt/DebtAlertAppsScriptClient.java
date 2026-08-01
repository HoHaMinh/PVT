package com.hoaphat.pvt.service.debt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hoaphat.pvt.config.DebtAlertProperties;
import com.hoaphat.pvt.model.dto.debt.DebtAlertApiResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class DebtAlertAppsScriptClient {
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final DebtAlertProperties properties;

    public DebtAlertAppsScriptClient(
            @Qualifier("debtAlertRestTemplate") RestTemplate restTemplate,
            ObjectMapper objectMapper,
            DebtAlertProperties properties) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    public DebtAlertApiResponse scan() {
        validateConfiguration();
        Map<String, String> request = new LinkedHashMap<>();
        request.put("action", "scanDebtAlerts");
        request.put("token", properties.getSharedToken().trim());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(java.util.Collections.singletonList(MediaType.APPLICATION_JSON));

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    properties.getWebAppUrl().trim(), HttpMethod.POST,
                    new HttpEntity<>(request, headers), String.class);

            // Apps Script ContentService always responds through a short-lived
            // script.googleusercontent.com URL. HttpURLConnection deliberately
            // does not follow a POST -> GET 302 redirect, so handle that one
            // trusted redirect explicitly instead of treating it as a failure.
            if (response.getStatusCode() == HttpStatus.FOUND
                    || response.getStatusCode() == HttpStatus.SEE_OTHER
                    || response.getStatusCode() == HttpStatus.MOVED_PERMANENTLY) {
                URI redirectUri = response.getHeaders().getLocation();
                validateGoogleContentRedirect(redirectUri);
                response = restTemplate.exchange(redirectUri, HttpMethod.GET,
                        new HttpEntity<>(headers), String.class);
            }
            if (!response.getStatusCode().is2xxSuccessful()
                    || response.getBody() == null || response.getBody().trim().isEmpty()) {
                throw new IllegalStateException(
                        "Apps Script không trả về dữ liệu hợp lệ (HTTP "
                                + response.getStatusCodeValue() + ").");
            }
            return objectMapper.readValue(response.getBody(), DebtAlertApiResponse.class);
        } catch (RestClientException e) {
            throw new IllegalStateException("Không thể kết nối Apps Script: " + safeMessage(e), e);
        } catch (Exception e) {
            if (e instanceof IllegalStateException) {
                throw (IllegalStateException) e;
            }
            throw new IllegalStateException("Không thể đọc JSON từ Apps Script: " + safeMessage(e), e);
        }
    }

    private void validateConfiguration() {
        if (!properties.isConfigured()) {
            throw new IllegalStateException("Chưa cấu hình URL hoặc token Apps Script.");
        }
        try {
            URI uri = URI.create(properties.getWebAppUrl().trim());
            if (!"https".equalsIgnoreCase(uri.getScheme())
                    || uri.getHost() == null
                    || !uri.getHost().equalsIgnoreCase("script.google.com")) {
                throw new IllegalArgumentException();
            }
        } catch (Exception e) {
            throw new IllegalStateException(
                    "URL Apps Script phải là URL HTTPS dạng script.google.com/macros/s/.../exec.");
        }
    }

    private void validateGoogleContentRedirect(URI uri) {
        if (uri == null
                || !"https".equalsIgnoreCase(uri.getScheme())
                || uri.getHost() == null
                || !"script.googleusercontent.com".equalsIgnoreCase(uri.getHost())) {
            throw new IllegalStateException(
                    "Apps Script trả về địa chỉ chuyển hướng không hợp lệ.");
        }
    }

    private String safeMessage(Exception e) {
        String message = e.getMessage();
        return message == null || message.trim().isEmpty()
                ? e.getClass().getSimpleName() : message;
    }
}
