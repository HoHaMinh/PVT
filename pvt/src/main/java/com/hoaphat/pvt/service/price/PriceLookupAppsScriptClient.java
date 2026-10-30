package com.hoaphat.pvt.service.price;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hoaphat.pvt.config.PriceLookupProperties;
import com.hoaphat.pvt.model.dto.price.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.*;

@Component
public class PriceLookupAppsScriptClient {
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final PriceLookupProperties properties;

    public PriceLookupAppsScriptClient(
            @Qualifier("priceLookupRestTemplate") RestTemplate restTemplate,
            ObjectMapper objectMapper,
            PriceLookupProperties properties) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    public PriceSearchApiResponse search(List<PriceSearchQuery> queries) {
        Map<String, Object> request = baseRequest("searchMaterialsBatch");
        request.put("queries", queries);
        PriceSearchApiResponse response = post(request, PriceSearchApiResponse.class);
        if (!response.isSuccess()) throw new IllegalStateException(apiMessage(response.getMessage()));
        return response;
    }

    public PriceLookupApiResponse lookup(List<PriceLookupQuery> groups) {
        Map<String, Object> request = baseRequest("lookupLatestPricesBatch");
        request.put("groups", groups);
        PriceLookupApiResponse response = post(request, PriceLookupApiResponse.class);
        if (!response.isSuccess()) throw new IllegalStateException(apiMessage(response.getMessage()));
        return response;
    }

    private Map<String, Object> baseRequest(String action) {
        validateConfiguration();
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("action", action);
        request.put("token", properties.getSharedToken().trim());
        return request;
    }

    private <T> T post(Map<String, Object> request, Class<T> responseType) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    properties.getWebAppUrl().trim(), HttpMethod.POST,
                    new HttpEntity<>(request, headers), String.class);
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
                throw new IllegalStateException("Apps Script không trả về dữ liệu hợp lệ (HTTP "
                        + response.getStatusCodeValue() + ").");
            }
            return objectMapper.readValue(response.getBody(), responseType);
        } catch (HttpStatusCodeException e) {
            if (e.getStatusCode() == HttpStatus.UNAUTHORIZED
                    || e.getStatusCode() == HttpStatus.FORBIDDEN) {
                throw new IllegalStateException(
                        "Web App tra giá đang yêu cầu đăng nhập Google (HTTP "
                                + e.getRawStatusCode()
                                + "). Hãy dùng URL /exec của deployment chạy dưới quyền chủ sở hữu "
                                + "và cho phép Anyone truy cập.", e);
            }
            throw new IllegalStateException("Apps Script từ chối yêu cầu (HTTP "
                    + e.getRawStatusCode() + "): " + safeMessage(e), e);
        } catch (RestClientException e) {
            throw new IllegalStateException("Không thể kết nối Apps Script: " + safeMessage(e), e);
        } catch (Exception e) {
            if (e instanceof IllegalStateException) throw (IllegalStateException) e;
            throw new IllegalStateException("Không thể đọc JSON từ Apps Script: " + safeMessage(e), e);
        }
    }

    private void validateConfiguration() {
        if (!properties.isConfigured()) {
            throw new IllegalStateException("Chưa bật hoặc chưa cấu hình kết nối tra giá Apps Script.");
        }
        try {
            URI uri = URI.create(properties.getWebAppUrl().trim());
            if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null
                    || !uri.getHost().equalsIgnoreCase("script.google.com")) throw new IllegalArgumentException();
        } catch (Exception e) {
            throw new IllegalStateException("URL Apps Script phải có dạng https://script.google.com/macros/s/.../exec.");
        }
    }

    private void validateGoogleContentRedirect(URI uri) {
        if (uri == null || !"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null
                || !"script.googleusercontent.com".equalsIgnoreCase(uri.getHost())) {
            throw new IllegalStateException("Apps Script trả về địa chỉ chuyển hướng không hợp lệ.");
        }
    }

    private String apiMessage(String message) {
        return message == null || message.trim().isEmpty() ? "Apps Script báo xử lý thất bại." : message.trim();
    }

    private String safeMessage(Exception e) {
        String message = e.getMessage();
        return message == null || message.trim().isEmpty() ? e.getClass().getSimpleName() : message;
    }
}
