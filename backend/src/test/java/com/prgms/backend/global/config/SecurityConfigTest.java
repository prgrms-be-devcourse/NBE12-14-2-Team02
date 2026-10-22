package com.prgms.backend.global.config;

import com.prgms.backend.domain.user.controller.AuthController;
import com.prgms.backend.domain.user.controller.UserController;
import com.prgms.backend.domain.user.dto.TokenPair;
import com.prgms.backend.domain.user.dto.response.ProfileResponse;
import com.prgms.backend.domain.user.enums.UserStatus;
import com.prgms.backend.domain.user.repository.UserRepository;
import com.prgms.backend.domain.user.service.AuthService;
import com.prgms.backend.domain.user.service.UserService;
import com.prgms.backend.domain.user.service.UserWithdrawService;
import com.prgms.backend.security.JwtTokenProvider;
import com.prgms.backend.support.WebSecurityTestConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// 공개/보호 경로, 인증 실패 응답 포맷, JWT 필터 연결
@WebMvcTest(
    controllers = {AuthController.class, UserController.class},
    properties = {
        "custom.jwt.access-secret=0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef",
        "custom.jwt.refresh-secret=abcdef0123456789abcdef0123456789abcdef0123456789abcdef0123456789"
    }
)
@Import(WebSecurityTestConfig.class)
class SecurityConfigTest {

    private static final String ACCESS_TOKEN = "access-token";

    @Autowired private MockMvc mvc;
    @Autowired private PasswordEncoder passwordEncoder;

    @MockitoBean private AuthService authService;
    @MockitoBean private UserService userService;
    @MockitoBean private UserWithdrawService userWithdrawService;
    @MockitoBean private JwtTokenProvider jwtTokenProvider;
    @MockitoBean private UserRepository userRepository;
    // @EnableJpaAuditing이 메인 클래스에 있어 웹 슬라이스에서도 필요
    @MockitoBean private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @Test
    @DisplayName("회원가입·로그인·재발급·중복 확인은 토큰 없이 호출할 수 있다")
    void publicEndpoints() throws Exception {

        // given
        when(authService.login(any())).thenReturn(new TokenPair("access", "refresh"));
        when(authService.reissue(any())).thenReturn(new TokenPair("access", "refresh"));
        when(authService.signup("user@test.com", "회원", "Passw0rd!", "Passw0rd!"))
            .thenReturn(1L);

        // when & then
        mvc.perform(get("/api/user/check-email").param("email", "user@test.com"))
            .andExpect(status().isOk());
        mvc.perform(get("/api/user/check-nickname").param("nickname", "회원"))
            .andExpect(status().isOk());
        mvc.perform(post("/api/user/sign-up")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"email":"user@test.com","nickname":"회원","password":"Passw0rd!","confirmPassword":"Passw0rd!"}
                    """))
            .andExpect(status().isCreated());
        mvc.perform(post("/api/auth/log-in")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"email":"user@test.com","password":"Passw0rd!"}
                    """))
            .andExpect(status().isCreated());
        mvc.perform(post("/api/auth/reissue"))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("보호된 API를 토큰 없이 호출하면 401과 공통 에러 포맷으로 응답한다")
    void protectedEndpointWithoutToken() throws Exception {

        // when & then
        mvc.perform(get("/api/user/me"))
            .andExpect(status().isUnauthorized())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value(401))
            .andExpect(jsonPath("$.message").value("인증이 필요합니다."));

        verifyNoInteractions(userService);
    }

    @Test
    @DisplayName("공개 목록에 없는 경로는 모두 인증이 필요하다")
    void unknownEndpointRequiresAuthentication() throws Exception {

        // when & then
        mvc.perform(get("/api/unknown"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("유효한 토큰과 활성 회원이면 보호된 API를 호출할 수 있다")
    void protectedEndpointWithValidToken() throws Exception {

        // given
        givenValidToken(1L);
        when(userRepository.existsByIdAndStatusAndDeletedAtIsNull(1L, UserStatus.ACTIVE))
            .thenReturn(true);
        when(userService.getProfile(1L)).thenReturn(new ProfileResponse("user@test.com", "회원"));

        // when & then
        mvc.perform(get("/api/user/me").header("Authorization", "Bearer " + ACCESS_TOKEN))
            .andExpect(status().isOk());

        verify(userService).getProfile(1L);
    }

    @Test
    @DisplayName("토큰 발급 이후 탈퇴한 회원은 유효한 토큰이 있어도 401이다")
    void withdrawnUserToken() throws Exception {

        // given
        givenValidToken(1L);
        when(userRepository.existsByIdAndStatusAndDeletedAtIsNull(1L, UserStatus.ACTIVE))
            .thenReturn(false);

        // when & then
        mvc.perform(get("/api/user/me").header("Authorization", "Bearer " + ACCESS_TOKEN))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value(401));

        verifyNoInteractions(userService);
    }

    @Test
    @DisplayName("유효하지 않은 토큰이면 401이다")
    void invalidToken() throws Exception {

        // given
        when(jwtTokenProvider.validateAccessToken("invalid-token")).thenReturn(false);

        // when & then
        mvc.perform(get("/api/user/me").header("Authorization", "Bearer invalid-token"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Bearer 접두사가 없는 Authorization 헤더는 토큰으로 인정하지 않는다")
    void tokenWithoutBearerPrefix() throws Exception {

        // when & then
        mvc.perform(get("/api/user/me").header("Authorization", ACCESS_TOKEN))
            .andExpect(status().isUnauthorized());

        verify(jwtTokenProvider, never()).validateAccessToken(anyString());
    }

    @Test
    @DisplayName("CSRF 토큰 없이도 인증된 POST 요청을 처리한다")
    void csrfDisabled() throws Exception {

        // given
        givenValidToken(1L);
        when(userRepository.existsByIdAndStatusAndDeletedAtIsNull(1L, UserStatus.ACTIVE))
            .thenReturn(true);

        // when & then
        mvc.perform(post("/api/auth/logout").header("Authorization", "Bearer " + ACCESS_TOKEN))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("같은 출처에서만 frame으로 띄울 수 있다")
    void frameOptionsSameOrigin() throws Exception {

        // when & then
        mvc.perform(get("/api/user/check-nickname").param("nickname", "회원"))
            .andExpect(header().string("X-Frame-Options", "SAMEORIGIN"));
    }

    @Test
    @DisplayName("비밀번호는 BCrypt로 암호화한다")
    void passwordEncoderIsBCrypt() {

        // when
        String encoded = passwordEncoder.encode("Passw0rd!");

        // then
        assertTrue(encoded.startsWith("$2"));
        assertTrue(passwordEncoder.matches("Passw0rd!", encoded));
    }

    private void givenValidToken(Long userId) {
        when(jwtTokenProvider.validateAccessToken(ACCESS_TOKEN)).thenReturn(true);
        when(jwtTokenProvider.getAccessUserId(ACCESS_TOKEN)).thenReturn(userId);
    }

}
