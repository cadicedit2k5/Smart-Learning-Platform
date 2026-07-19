package com.smartlearning.system.auth.service;

import com.smartlearning.system.configs.JwtProperties;
import com.smartlearning.system.auth.dto.response.LoginResponse;
import com.smartlearning.system.auth.security.SecurityUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class JwtTokenService {

    private final JwtEncoder jwtEncoder;
    private final JwtProperties jwtProperties;

    public LoginResponse generateToken(
            SecurityUser user
    ) {
        Instant issuedAt = Instant.now();

        Instant expiresAt = issuedAt.plus(
                jwtProperties.accessTokenTtl()
        );

        List<String> roles = user
                .getAuthorities()
                .stream()
                .map(authority ->
                        authority.getAuthority()
                                .replaceFirst("^ROLE_", "")
                )
                .toList();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(jwtProperties.issuer())
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .subject(user.id().toString())
                .audience(jwtProperties.audiences())
                .claim("roles", roles)
                .build();

        JwsHeader header = JwsHeader
                .with(SignatureAlgorithm.RS256)
                .keyId(jwtProperties.keyId())
                .build();

        String accessToken = jwtEncoder
                .encode(
                        JwtEncoderParameters.from(
                                header,
                                claims
                        )
                )
                .getTokenValue();

        return new LoginResponse(
                accessToken,
                jwtProperties
                        .accessTokenTtl()
                        .toSeconds()
        );
    }
}