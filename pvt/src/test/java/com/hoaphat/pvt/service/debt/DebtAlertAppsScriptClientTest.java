package com.hoaphat.pvt.service.debt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hoaphat.pvt.config.DebtAlertProperties;
import com.hoaphat.pvt.model.dto.debt.DebtAlertApiResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

class DebtAlertAppsScriptClientTest {
    private static final String URL = "https://script.google.com/macros/s/test-deployment/exec";

    private DebtAlertProperties properties;
    private DebtAlertAppsScriptClient client;
    private MockRestServiceServer server;

    @BeforeEach
    void setUp() {
        properties = new DebtAlertProperties();
        properties.setEnabled(true);
        properties.setWebAppUrl(URL);
        properties.setSharedToken("secret-token");
        RestTemplate restTemplate = new RestTemplate();
        server = MockRestServiceServer.bindTo(restTemplate).build();
        client = new DebtAlertAppsScriptClient(restTemplate, new ObjectMapper(), properties);
    }

    @Test
    void followsTrustedAppsScriptContentRedirectAndReadsJsonContract() {
        String contentUrl = "https://script.googleusercontent.com/macros/echo?user_content_key=test";
        server.expect(requestTo(URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.action").value("scanDebtAlerts"))
                .andExpect(jsonPath("$.token").value("secret-token"))
                .andRespond(withStatus(HttpStatus.FOUND)
                        .location(java.net.URI.create(contentUrl)));
        server.expect(requestTo(contentUrl))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(
                        "{\"success\":true,\"scanDate\":\"2026-07-07\","
                                + "\"employees\":[{\"alerts\":[{\"tPlus\":-4}]}]}",
                        MediaType.APPLICATION_JSON));

        DebtAlertApiResponse response = client.scan();

        assertTrue(response.isSuccess());
        assertEquals("2026-07-07", response.getScanDate());
        assertEquals(Integer.valueOf(-4),
                response.getEmployees().get(0).getAlerts().get(0).getTPlus());
        server.verify();
    }

    @Test
    void rejectsNonGoogleWebAppUrlBeforeSendingToken() {
        properties.setWebAppUrl("https://example.com/collect");

        IllegalStateException error = assertThrows(IllegalStateException.class, client::scan);

        assertTrue(error.getMessage().contains("script.google.com"));
    }

    @Test
    void malformedJsonBecomesControlledFailure() {
        server.expect(requestTo(URL))
                .andRespond(withSuccess("not-json", MediaType.TEXT_PLAIN));

        IllegalStateException error = assertThrows(IllegalStateException.class, client::scan);

        assertTrue(error.getMessage().contains("JSON"));
    }

    @Test
    void refusesRedirectToUntrustedHost() {
        server.expect(requestTo(URL))
                .andRespond(withStatus(HttpStatus.FOUND)
                        .location(java.net.URI.create("https://example.com/steal")));

        IllegalStateException error = assertThrows(IllegalStateException.class, client::scan);

        assertTrue(error.getMessage().contains("chuyển hướng"));
    }
}
