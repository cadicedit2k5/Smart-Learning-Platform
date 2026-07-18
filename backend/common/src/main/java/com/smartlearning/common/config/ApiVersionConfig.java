package com.smartlearning.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.HandlerTypePredicate;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class ApiVersionConfig implements WebMvcConfigurer {

    private final String apiVersion;

    public ApiVersionConfig(@Value("${api.version:v1}") String apiVersion) {
        this.apiVersion = apiVersion;
    }

    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        configurer.addPathPrefix(
                "/api/" + apiVersion,
                HandlerTypePredicate.forAnnotation(RestController.class)
        );
    }
}