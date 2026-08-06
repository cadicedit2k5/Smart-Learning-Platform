package com.smartlearning.system.configs;

import com.smartlearning.system.auth.security.DatabaseUserDetailsService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableMethodSecurity(securedEnabled = true)
public class SystemSecurityConfigs {
    @Value("${api.version:v1}")
    private String apiVersion;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthenticationConverter jwtAuthenticationConverter) throws Exception {
        String apiPrefix = "/api/" + apiVersion;
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                apiPrefix + "/auth/register",
                                apiPrefix + "/auth/login",
                                "/actuator/health").permitAll()
                        .requestMatchers(apiPrefix + "/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated()
                );

        http.oauth2ResourceServer(resourceServer ->
                resourceServer.jwt(jwt ->
                        jwt.jwtAuthenticationConverter(
                                jwtAuthenticationConverter
                        )
                )
        );

        return http.build();
    }

    @Bean
    DaoAuthenticationProvider authenticationProvider(
            DatabaseUserDetailsService userDetailsService,
            PasswordEncoder passwordEncoder
    ) {
        DaoAuthenticationProvider provider =
                new DaoAuthenticationProvider(
                        userDetailsService
                );

        provider.setPasswordEncoder(
                passwordEncoder
        );

        return provider;
    }

    @Bean
    AuthenticationManager authenticationManager(
            DaoAuthenticationProvider provider
    ) {
        return new ProviderManager(provider);
    }

//    @Bean
//    public CorsConfigurationSource corsConfigurationSource() {
//        CorsConfiguration config = new CorsConfiguration();
//
//        config.setAllowedOrigins(List.of(
//        ));
//
//        config.setAllowedMethods(List.of(
//                "GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"
//        ));
//
//        config.setAllowedHeaders(List.of(
//                "Authorization",
//                "Content-Type",
//                "Accept",
//                "Origin",
//                "X-Requested-With"
//        ));
//
//        config.setExposedHeaders(List.of("Authorization"));
//        config.setAllowCredentials(true);
//
//        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
//        source.registerCorsConfiguration("/api/**", config);
//
//        return source;
//    }
}
