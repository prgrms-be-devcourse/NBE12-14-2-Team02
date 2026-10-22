package com.prgms.backend.domain.user;

import com.prgms.backend.domain.user.controller.AuthController;
import com.prgms.backend.domain.user.dto.TokenPair;
import com.prgms.backend.domain.user.dto.request.LogInRequest;
import com.prgms.backend.domain.user.enums.UserStatus;
import com.prgms.backend.domain.user.repository.UserRepository;
import com.prgms.backend.domain.user.service.AuthService;
import com.prgms.backend.support.WebSecurityTestConfig;
import com.prgms.backend.global.exception.custom.user.LoginFailException;
import com.prgms.backend.global.exception.custom.user.ReissueFailException;
import com.prgms.backend.security.JwtTokenProvider;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// HTTP 매핑·JSON 형태·검증·refresh token 쿠키 속성
@WebMvcTest(
    controllers = AuthController.class,
    properties = {
        "custom.jwt.access-secret=0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef",
        "custom.jwt.refresh-secret=abcdef0123456789abcdef0123456789abcdef0123456789abcdef0123456789"
    }
)
@Import(WebSecurityTestConfig.class)
class AuthControllerTest {

    private static final String ACCESS_TOKEN = "access-token";

    @Autowired private MockMvc mvc;

    @MockitoBean private AuthService authService;
    @MockitoBean private JwtTokenProvider jwtTokenProvider;
    @MockitoBean private UserRepository userRepository;
    // @EnableJpaAuditing이 메인 클래스에 있어 웹 슬라이스에서도 필요
    @MockitoBean private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @Test
    @DisplayName("로그인에 성공하면 201과 access token을 응답하고 refresh token은 쿠키로만 내려준다")
    void login() throws Exception {

        // given
        when(authService.login(new LogInRequest("user@test.com", "Passw0rd!")))
            .thenReturn(new TokenPair("new-access-token", "new-refresh-token"));

        // when & then
        mvc.perform(post("/api/auth/log-in")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"email":"user@test.com","password":"Passw0rd!"}
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.code").value(201))
            .andExpect(jsonPath("$.message").value(""))
            .andExpect(jsonPath("$.data.accessToken").value("new-access-token"))
            .andExpect(jsonPath("$.data.refreshToken").doesNotExist())
            .andExpect(cookie().value("refreshToken", "new-refresh-token"))
            .andExpect(cookie().httpOnly("refreshToken", true))
            .andExpect(cookie().secure("refreshToken", true))
            .andExpect(cookie().sameSite("refreshToken", "Strict"))
            .andExpect(cookie().path("refreshToken", "/api/auth"))
            .andExpect(cookie().maxAge("refreshToken", 14 * 24 * 60 * 60));
    }

    @Test
    @DisplayName("이메일 형식이 아니면 로그인 요청은 400이다")
    void loginInvalidEmail() throws Exception {

        // when & then
        mvc.perform(post("/api/auth/log-in")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"email":"not-email","password":"Passw0rd!"}
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value(400));

        verify(authService, never()).login(any());
    }

    @Test
    @DisplayName("비밀번호 필드가 없으면 로그인 요청은 400이다")
    void loginMissingPassword() throws Exception {

        // when & then
        mvc.perform(post("/api/auth/log-in")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"email":"user@test.com"}
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value(400));

        verify(authService, never()).login(any());
    }

    @Test
    @DisplayName("로그인 실패 예외는 401과 공통 에러 포맷으로 응답한다")
    void loginFail() throws Exception {

        // given
        when(authService.login(any())).thenThrow(new LoginFailException());

        // when & then
        mvc.perform(post("/api/auth/log-in")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"email":"user@test.com","password":"wrong"}
                    """))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value(401))
            .andExpect(jsonPath("$.message").value("이메일 또는 비밀번호가 일치하지 않습니다."))
            .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    @DisplayName("refresh token 쿠키로 재발급하면 새 access token과 새 refresh token 쿠키를 내려준다")
    void reissue() throws Exception {

        // given
        when(authService.reissue("old-refresh-token"))
            .thenReturn(new TokenPair("new-access-token", "new-refresh-token"));

        // when & then
        mvc.perform(post("/api/auth/reissue")
                .cookie(new Cookie("refreshToken", "old-refresh-token")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.accessToken").value("new-access-token"))
            .andExpect(cookie().value("refreshToken", "new-refresh-token"))
            .andExpect(cookie().httpOnly("refreshToken", true))
            .andExpect(cookie().path("refreshToken", "/api/auth"));
    }

    @Test
    @DisplayName("refresh token 쿠키 없이 재발급을 요청하면 null을 넘겨 401로 응답한다")
    void reissueWithoutCookie() throws Exception {

        // given
        when(authService.reissue(null)).thenThrow(new ReissueFailException());

        // when & then
        mvc.perform(post("/api/auth/reissue"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value(401))
            .andExpect(jsonPath("$.message").value("reissue에 실패했습니다."));

        verify(authService).reissue(null);
    }

    @Test
    @DisplayName("로그아웃하면 토큰의 회원 id로 로그아웃하고 refresh token 쿠키를 만료시킨다")
    void logout() throws Exception {

        // given
        givenAuthenticatedUser(1L);

        // when & then
        mvc.perform(post("/api/auth/logout")
                .header("Authorization", "Bearer " + ACCESS_TOKEN)
                .cookie(new Cookie("refreshToken", "refresh-token")))
            .andExpect(status().isNoContent())
            .andExpect(content().string(""))
            .andExpect(cookie().value("refreshToken", ""))
            .andExpect(cookie().maxAge("refreshToken", 0))
            .andExpect(cookie().path("refreshToken", "/api/auth"));

        verify(authService).logout(1L, "refresh-token");
    }

    @Test
    @DisplayName("access token 없이 로그아웃하면 401이다")
    void logoutWithoutToken() throws Exception {

        // when & then
        mvc.perform(post("/api/auth/logout"))
            .andExpect(status().isUnauthorized());

        verify(authService, never()).logout(anyLong(), any());
    }

    private void givenAuthenticatedUser(Long userId) {
        when(jwtTokenProvider.validateAccessToken(ACCESS_TOKEN)).thenReturn(true);
        when(jwtTokenProvider.getAccessUserId(ACCESS_TOKEN)).thenReturn(userId);
        when(userRepository.existsByIdAndStatusAndDeletedAtIsNull(userId, UserStatus.ACTIVE))
            .thenReturn(true);
    }
}
