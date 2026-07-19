package com.smartlearning.system.configs;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.Resource;

import java.time.Duration;
import java.util.List;

@ConfigurationProperties(prefix = "app.security.jwt")
public record JwtProperties(
        String issuer,
        Duration accessTokenTtl,
        List<String> audiences,
        String keyId,
        Resource publicKeyLocation,
        Resource privateKeyLocation
) {
}