package com.smartlearning.common.config;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.HandlerTypePredicate;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

public class ApiVersionConfig implements WebMvcConfigurer {

    private final String apiVersion;

    public ApiVersionConfig(String apiVersion) {
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