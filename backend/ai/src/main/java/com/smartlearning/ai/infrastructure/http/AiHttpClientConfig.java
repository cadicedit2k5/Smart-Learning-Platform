package com.smartlearning.ai.infrastructure.http;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;

@Configuration(proxyBeanMethods = false)
public class AiHttpClientConfig {

    @Bean
    @Qualifier("coreRestClient")
    RestClient coreRestClient(
            RestClient.Builder builder,
            @Value("${app.services.core.base-url}") String baseUrl
    ) {
        return builder.baseUrl(baseUrl).build();
    }

    @Bean
    @Qualifier("aiEngineRestClient")
    RestClient aiEngineRestClient(
            RestClient.Builder builder,
            @Value("${app.ai-engine.base-url}") String baseUrl
    ) {
        HttpClient httpClient = HttpClient.newBuilder()
                        .version(HttpClient.Version.HTTP_1_1).build();
        return builder.requestFactory(new JdkClientHttpRequestFactory(httpClient))
                .baseUrl(baseUrl).build();
    }
}