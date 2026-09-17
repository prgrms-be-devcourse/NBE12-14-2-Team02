package com.prgms.backend.domain.user.service;

import com.prgms.backend.domain.user.entity.SecurityUser;
import com.prgms.backend.domain.user.entity.User;
import com.prgms.backend.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 우리가 만든 User을 UserDetaols로 변환
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String id) throws UsernameNotFoundException {

        User user = userRepository
                .findById(Long.parseLong(id))
                .orElseThrow(
                        ()-> new UsernameNotFoundException("존재하지 않는 사용자")
                );

        return new SecurityUser(user);
    }
}
