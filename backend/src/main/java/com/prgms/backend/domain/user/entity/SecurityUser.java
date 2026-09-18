package com.prgms.backend.domain.user.entity;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

// User 엔티티를 Security가 이해할 수 있는 UserDetails로 변환
public class SecurityUser implements UserDetails {
    public final User user;

    public SecurityUser(User user) {
        this.user = user;
    }

    @Override
    public String getUsername() {
        return user.getEmail();
    }

    @Override
    public String getPassword() {
        return user.getPassword();
    }

    @Override
    public Collection<? extends GrantedAuthority>
    getAuthorities() {
        return List.of(
                new SimpleGrantedAuthority("ROLE_USER")
        );
    }

    public Long getId() {
        return user.getId();
    }

    public String getRefreshToken() {
        return user.getRefreshToken();
    }
}
