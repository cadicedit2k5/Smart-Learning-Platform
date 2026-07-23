package com.smartlearning.system.auth.security;

import com.smartlearning.system.auth.entity.User;
import com.smartlearning.system.auth.entity.enums.UserStatus;
import com.smartlearning.system.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class DatabaseUserDetailsService
        implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email)
            throws UsernameNotFoundException {

        String normalizedEmail = email
                .trim()
                .toLowerCase(Locale.ROOT);

        User user = userRepository
                .findByEmailIgnoreCaseAndStatus(normalizedEmail, UserStatus.ACTIVE)
                .orElseThrow(() ->
                        new UsernameNotFoundException(
                                "Thông tin đăng nhập không hợp lệ"
                        )
                );

        String authority = "ROLE_" +
                user.getRole()
                        .getCode()
                        .toUpperCase(Locale.ROOT);

        return new SecurityUser(
                user.getId(),
                user.getEmail(),
                user.getPassword(),
                List.of(
                        new SimpleGrantedAuthority(authority)
                )
        );
    }
}
