package com.smartlearning.core.infrastructure.http;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration(proxyBeanMethods = false)
public class CoreHttpClientConfig {

    @Bean
    @Qualifier("systemRestClient")
    RestClient systemRestClient(
            RestClient.Builder builder,
            @Value("${app.services.system.base-url}")
            String baseUrl
    ) {
        return builder
                .baseUrl(baseUrl)
                .build();
    }
}