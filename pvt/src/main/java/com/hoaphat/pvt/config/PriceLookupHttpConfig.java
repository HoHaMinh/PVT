package com.hoaphat.pvt.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
public class PriceLookupHttpConfig {
    @Bean(name = "priceLookupRestTemplate")
    public RestTemplate priceLookupRestTemplate(RestTemplateBuilder builder,
                                                 PriceLookupProperties properties) {
        return builder
                .setConnectTimeout(Duration.ofSeconds(Math.max(1, properties.getConnectTimeoutSeconds())))
                .setReadTimeout(Duration.ofSeconds(Math.max(10, properties.getReadTimeoutSeconds())))
                .build();
    }
}
