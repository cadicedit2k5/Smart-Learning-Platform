package com.smartlearning.system.auth.security;

import com.smartlearning.system.auth.entity.Permission;
import com.smartlearning.system.auth.entity.User;
import com.smartlearning.system.auth.entity.enums.UserStatus;
import com.smartlearning.system.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

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

        Set<GrantedAuthority> authorities = new HashSet<>();

        authorities.add(
                new SimpleGrantedAuthority(
                        "ROLE_" + user.getRole()
                                .getCode()
                                .toUpperCase(Locale.ROOT)
                )
        );

        user.getRole().getPermissions()
                .stream()
                .map(Permission::getCode)
                .map(SimpleGrantedAuthority::new)
                .forEach(authorities::add);

        return new SecurityUser(
                user.getId(),
                user.getEmail(),
                user.getPassword(),
                authorities
        );
    }
}
