package com.hoaphat.pvt.service.price;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hoaphat.pvt.config.PriceLookupProperties;
import com.hoaphat.pvt.model.dto.price.PriceSearchApiResponse;
import com.hoaphat.pvt.model.dto.price.PriceLookupApiResponse;
import com.hoaphat.pvt.model.dto.price.PriceLookupQuery;
import com.hoaphat.pvt.model.dto.price.PriceSelectedCandidate;
import com.hoaphat.pvt.model.dto.price.PriceSearchQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.*;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.math.BigDecimal;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class PriceLookupAppsScriptClientTest {
    private static final String URL = "https://script.google.com/macros/s/price-deployment/exec";
    private PriceLookupProperties properties;
    private PriceLookupAppsScriptClient client;
    private MockRestServiceServer server;

    @BeforeEach
    void setUp() {
        properties = new PriceLookupProperties();
        properties.setEnabled(true);
        properties.setWebAppUrl(URL);
        properties.setSharedToken("price-secret");
        RestTemplate restTemplate = new RestTemplate();
        server = MockRestServiceServer.bindTo(restTemplate).build();
        client = new PriceLookupAppsScriptClient(restTemplate, new ObjectMapper(), properties);
    }

    @Test
    void sendsBatchSearchAndFollowsTrustedRedirect() {
        String contentUrl = "https://script.googleusercontent.com/macros/echo?user_content_key=price";
        server.expect(requestTo(URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.action").value("searchMaterialsBatch"))
                .andExpect(jsonPath("$.token").value("price-secret"))
                .andExpect(jsonPath("$.queries[0].id").value("line-1"))
                .andRespond(withStatus(HttpStatus.FOUND).location(URI.create(contentUrl)));
        server.expect(requestTo(contentUrl)).andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"success\":true,\"results\":[{\"id\":\"line-1\","
                        + "\"keyword\":\"khoa*dai\",\"candidates\":[{\"name\":\"Khóa đai\","
                        + "\"startRow\":10,\"rowCount\":2}]}]}", MediaType.APPLICATION_JSON));

        PriceSearchApiResponse response = client.search(Collections.singletonList(
                new PriceSearchQuery("line-1", "khoa*dai")));

        assertTrue(response.isSuccess());
        assertEquals("Khóa đai", response.getResults().get(0).getCandidates().get(0).getName());
        server.verify();
    }

    @Test
    void rejectsUntrustedUrlBeforeSendingToken() {
        properties.setWebAppUrl("https://example.com/price");
        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> client.search(Collections.singletonList(new PriceSearchQuery("1", "test"))));
        assertTrue(error.getMessage().contains("script.google.com"));
    }

    @Test
    void explainsGoogleLoginRequirementOnUnauthorizedDeployment() {
        server.expect(requestTo(URL))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> client.search(Collections.singletonList(
                        new PriceSearchQuery("line-1", "test"))));

        assertTrue(error.getMessage().contains("yêu cầu đăng nhập Google"));
        assertTrue(error.getMessage().contains("/exec"));
    }

    @Test
    void readsFormattedPriceStringsReturnedByAppsScript() {
        String contentUrl = "https://script.googleusercontent.com/macros/echo?user_content_key=lookup";
        server.expect(requestTo(URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.action").value("lookupLatestPricesBatch"))
                .andRespond(withStatus(HttpStatus.FOUND).location(URI.create(contentUrl)));
        server.expect(requestTo(contentUrl)).andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"success\":true,\"results\":[{\"id\":\"item-1\","
                        + "\"groups\":["
                        + "{\"group\":\"Mien Bac\",\"price\":\"47,000.00\",\"found\":true},"
                        + "{\"group\":\"Mien Trung\",\"price\":\"47.000,00\",\"found\":true},"
                        + "{\"group\":\"Mien Nam\",\"price\":47000.50,\"found\":true}]}]}",
                        MediaType.APPLICATION_JSON));

        PriceLookupApiResponse response = client.lookup(Collections.singletonList(
                new PriceLookupQuery("item-1", "loc soi", Collections.singletonList(
                        new PriceSelectedCandidate("Loc soi", 10, 1)))));

        assertEquals(new BigDecimal("47000.00"), response.getResults().get(0).getGroups().get(0).getPrice());
        assertEquals(new BigDecimal("47000.00"), response.getResults().get(0).getGroups().get(1).getPrice());
        assertEquals(new BigDecimal("47000.50"), response.getResults().get(0).getGroups().get(2).getPrice());
        server.verify();
    }
}
