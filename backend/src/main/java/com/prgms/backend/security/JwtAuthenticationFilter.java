package com.prgms.backend.security;

import com.prgms.backend.domain.user.entity.SecurityUser;
import com.prgms.backend.domain.user.enums.UserStatus;
import com.prgms.backend.domain.user.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;

    // 토큰 분리
    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String token = resolveToken(request);

        if (token != null && jwtTokenProvider.validateAccessToken(token)) {

            // 토큰이 유효한 경우 인증 정보를 가져온다.
            Long userId = jwtTokenProvider.getAccessUserId(token);

            // 발급 이후 탈퇴한 회원도 차단하도록 현재 상태를 확인한다.
            boolean activeUser = userRepository.existsByIdAndStatusAndDeletedAtIsNull(
                    userId, UserStatus.ACTIVE);

            if (activeUser) {
                SecurityUser securityUser = new SecurityUser(userId);

                Authentication authentication = new UsernamePasswordAuthenticationToken(
                        securityUser, null, securityUser.getAuthorities());

                SecurityContextHolder.getContext().setAuthentication(authentication);

            } else {
                // 보호된 API의 401 응답은 SecurityConfig의 인증 진입점에서 처리한다.
                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
    }
}
