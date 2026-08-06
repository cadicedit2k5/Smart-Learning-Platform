package com.smartlearning.system.auth.service;

import com.smartlearning.system.auth.dto.response.LoginResponse;
import com.smartlearning.system.auth.security.SecurityUser;
import com.smartlearning.system.configs.JwtProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtTokenServiceTest {

    @Mock
    private JwtEncoder jwtEncoder;

    @Test
    void generateToken_buildsExpectedHeaderAndClaims() {
        Duration timeToLive = Duration.ofMinutes(30);
        JwtProperties properties = new JwtProperties(
                "smart-learning",
                timeToLive,
                List.of("smart-learning-api"),
                "access-token-key",
                null,
                null
        );
        JwtTokenService tokenService =
                new JwtTokenService(jwtEncoder, properties);
        UUID userId = UUID.randomUUID();
        SecurityUser user = new SecurityUser(
                userId,
                "student@example.com",
                "encoded-password",
                List.of(
                        new SimpleGrantedAuthority("ROLE_STUDENT"),
                        new SimpleGrantedAuthority("COURSE_READ"),
                        new SimpleGrantedAuthority("COURSE_CREATE")
                )
        );
        Jwt encodedJwt = mock(Jwt.class);
        when(encodedJwt.getTokenValue()).thenReturn("signed-access-token");
        when(jwtEncoder.encode(org.mockito.ArgumentMatchers.any()))
                .thenReturn(encodedJwt);
        Instant beforeCall = Instant.now();

        LoginResponse result = tokenService.generateToken(user);

        Instant afterCall = Instant.now();
        assertThat(result.accessToken()).isEqualTo("signed-access-token");
        assertThat(result.expiresIn()).isEqualTo(timeToLive.toSeconds());

        ArgumentCaptor<JwtEncoderParameters> captor =
                ArgumentCaptor.forClass(JwtEncoderParameters.class);
        verify(jwtEncoder).encode(captor.capture());
        JwtEncoderParameters parameters = captor.getValue();

        assertThat(parameters.getJwsHeader().getAlgorithm())
                .isEqualTo(SignatureAlgorithm.RS256);
        assertThat(parameters.getJwsHeader().getKeyId())
                .isEqualTo("access-token-key");
        assertThat(parameters.getClaims().getClaimAsString("iss"))
                .isEqualTo("smart-learning");
        assertThat(parameters.getClaims().getSubject())
                .isEqualTo(userId.toString());
        assertThat(parameters.getClaims().getAudience())
                .containsExactly("smart-learning-api");
        assertThat(parameters.getClaims()
                .getClaimAsStringList("roles"))
                .containsExactly("STUDENT");
        assertThat(parameters.getClaims()
                .getClaimAsStringList("permissions"))
                .containsExactly("COURSE_READ", "COURSE_CREATE");
        assertThat(parameters.getClaims().getIssuedAt())
                .isBetween(beforeCall, afterCall);
        assertThat(parameters.getClaims().getExpiresAt())
                .isEqualTo(parameters.getClaims()
                        .getIssuedAt()
                        .plus(timeToLive));
    }
}
