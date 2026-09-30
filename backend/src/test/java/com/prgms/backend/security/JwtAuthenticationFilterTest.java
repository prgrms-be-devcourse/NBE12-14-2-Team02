package com.prgms.backend.security;

import com.prgms.backend.domain.user.entity.SecurityUser;
import com.prgms.backend.domain.user.enums.UserStatus;
import com.prgms.backend.domain.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class JwtAuthenticationFilterTest {

    private JwtTokenProvider tokenProvider;
    private UserRepository userRepository;
    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        tokenProvider = mock(JwtTokenProvider.class);
        userRepository = mock(UserRepository.class);
        filter = new JwtAuthenticationFilter(tokenProvider, userRepository);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("유효한 토큰과 활성 회원이면 ID만 가진 Principal로 인증한다")
    void authenticatesActiveUser() throws Exception {
        givenValidToken();
        when(userRepository.existsByIdAndStatusAndDeletedAtIsNull(1L, UserStatus.ACTIVE))
                .thenReturn(true);

        filter.doFilter(requestWithToken(), new MockHttpServletResponse(), (request, response) -> {
            var authentication = SecurityContextHolder.getContext().getAuthentication();
            assertNotNull(authentication);
            assertTrue(authentication.isAuthenticated());
            SecurityUser principal = assertInstanceOf(SecurityUser.class, authentication.getPrincipal());
            assertEquals(1L, principal.getId());
        });

        verify(userRepository).existsByIdAndStatusAndDeletedAtIsNull(1L, UserStatus.ACTIVE);
        verifyNoMoreInteractions(userRepository);
    }

    @Test
    @DisplayName("탈퇴 또는 삭제되었거나 존재하지 않는 회원은 유효한 토큰이 있어도 인증하지 않는다")
    void rejectsInactiveUserAndClearsExistingAuthentication() throws Exception {
        givenValidToken();
        when(userRepository.existsByIdAndStatusAndDeletedAtIsNull(1L, UserStatus.ACTIVE))
                .thenReturn(false);
        SecurityUser principal = new SecurityUser(1L);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));

        filter.doFilter(requestWithToken(), new MockHttpServletResponse(), (request, response) ->
                assertNull(SecurityContextHolder.getContext().getAuthentication()));

        verify(userRepository).existsByIdAndStatusAndDeletedAtIsNull(1L, UserStatus.ACTIVE);
    }

    @Test
    @DisplayName("유효하지 않은 토큰은 회원 DB를 조회하지 않는다")
    void invalidTokenDoesNotQueryUser() throws Exception {
        when(tokenProvider.validateAccessToken("token")).thenReturn(false);

        filter.doFilter(requestWithToken(), new MockHttpServletResponse(), (request, response) ->
                assertNull(SecurityContextHolder.getContext().getAuthentication()));

        verifyNoInteractions(userRepository);
        verify(tokenProvider, never()).getAccessUserId(anyString());
    }

    @Test
    @DisplayName("토큰이 없는 공개 요청은 DB 조회 없이 다음 필터로 진행한다")
    void missingTokenDoesNotQueryUser() throws Exception {
        var request = new MockHttpServletRequest("POST", "/api/auth/log-in");

        filter.doFilter(request, new MockHttpServletResponse(), (req, response) ->
                assertNull(SecurityContextHolder.getContext().getAuthentication()));

        verifyNoInteractions(tokenProvider, userRepository);
    }

    private void givenValidToken() {
        when(tokenProvider.validateAccessToken("token")).thenReturn(true);
        when(tokenProvider.getAccessUserId("token")).thenReturn(1L);
    }

    private MockHttpServletRequest requestWithToken() {
        var request = new MockHttpServletRequest("GET", "/api/user/me");
        request.addHeader("Authorization", "Bearer token");
        return request;
    }
}
