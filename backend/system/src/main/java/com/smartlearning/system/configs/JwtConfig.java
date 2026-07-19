package com.smartlearning.system.configs;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.*;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.*;
import org.springframework.security.converter.RsaKeyConverters;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.*;

import java.io.IOException;
import java.security.interfaces.*;
import java.util.List;

@Configuration
@EnableConfigurationProperties(JwtProperties.class)
public class JwtConfig {

    @Bean
    RSAPublicKey jwtPublicKey(
            JwtProperties properties
    ) throws IOException {
        var resource = properties.publicKeyLocation();

        if (resource == null || !resource.exists()) {
            throw new IllegalStateException(
                    "Không tìm thấy JWT public key: " + resource
            );
        }

        try (var inputStream = properties
                .publicKeyLocation()
                .getInputStream()) {

            return RsaKeyConverters
                    .x509()
                    .convert(inputStream);
        }
    }

    @Bean
    RSAPrivateKey jwtPrivateKey(
            JwtProperties properties
    ) throws IOException {
        var resource = properties.privateKeyLocation();

        if (resource == null || !resource.exists()) {
            throw new IllegalStateException(
                    "Không tìm thấy JWT private key: " + resource
            );
        }

        try (var inputStream = properties
                .privateKeyLocation()
                .getInputStream()) {

            return RsaKeyConverters
                    .pkcs8()
                    .convert(inputStream);
        }
    }

    @Bean
    JwtEncoder jwtEncoder(
            RSAPublicKey publicKey,
            RSAPrivateKey privateKey,
            JwtProperties properties
    ) {
        RSAKey rsaKey = new RSAKey.Builder(publicKey)
                .privateKey(privateKey)
                .keyID(properties.keyId())
                .build();

        JWKSource<SecurityContext> jwkSource =
                new ImmutableJWKSet<>(
                        new JWKSet(rsaKey)
                );

        return new NimbusJwtEncoder(jwkSource);
    }

    @Bean
    JwtDecoder jwtDecoder(
            RSAPublicKey publicKey,
            JwtProperties properties
    ) {
        NimbusJwtDecoder decoder =
                NimbusJwtDecoder
                        .withPublicKey(publicKey)
                        .signatureAlgorithm(
                                SignatureAlgorithm.RS256
                        )
                        .build();

        OAuth2TokenValidator<Jwt> issuerValidator =
                JwtValidators.createDefaultWithIssuer(
                        properties.issuer()
                );

        OAuth2TokenValidator<Jwt> audienceValidator =
                new JwtClaimValidator<List<String>>(
                        "aud",
                        audiences ->
                                audiences != null &&
                                        audiences.contains("system-api")
                );

        decoder.setJwtValidator(
                new DelegatingOAuth2TokenValidator<>(
                        issuerValidator,
                        audienceValidator
                )
        );

        return decoder;
    }
}