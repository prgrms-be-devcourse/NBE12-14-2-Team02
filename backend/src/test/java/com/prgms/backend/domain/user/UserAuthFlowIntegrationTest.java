package com.prgms.backend.domain.user;

import com.jayway.jsonpath.JsonPath;
import com.prgms.backend.domain.user.entity.User;
import com.prgms.backend.domain.user.enums.UserStatus;
import com.prgms.backend.domain.user.repository.UserRepository;
import com.prgms.backend.support.MySqlContainerSupport;
import jakarta.servlet.http.Cookie;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// 실제 빈(BCrypt·JWT·필터·트랜잭션·DB)으로 회원가입부터 탈퇴까지의 흐름 확인
@SpringBootTest
@AutoConfigureMockMvc
class UserAuthFlowIntegrationTest extends MySqlContainerSupport {

    private static final String PASSWORD = "Passw0rd!";

    @Autowired private MockMvc mvc;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private String email;
    private String nickname;

    @BeforeEach
    void setUp() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        email = "user-" + suffix + "@test.com";
        nickname = "회원-" + suffix;
    }

    @Test
    @DisplayName("회원가입하면 비밀번호를 BCrypt로 암호화해 활성 회원으로 저장한다")
    void signUpStoresEncodedPassword() throws Exception {

        // when
        Long userId = signUp();

        // then
        User user = userRepository.findById(userId).orElseThrow();
        assertEquals(email, user.getEmail());
        assertNotEquals(PASSWORD, user.getPassword());
        assertTrue(passwordEncoder.matches(PASSWORD, user.getPassword()));
        assertEquals(UserStatus.ACTIVE, user.getStatus());
    }

    @Test
    @DisplayName("가입 후 이메일과 닉네임 중복 확인은 사용 불가로 응답한다")
    void checkDuplicateAfterSignUp() throws Exception {

        // given
        signUp();

        // when & then
        mvc.perform(get("/api/user/check-email").param("email", email))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.available").value(false));
        mvc.perform(get("/api/user/check-nickname").param("nickname", nickname))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.available").value(false));
    }

    @Test
    @DisplayName("로그인으로 받은 access token으로 내 프로필을 조회하고 DB에 refresh token을 저장한다")
    void loginAndGetProfile() throws Exception {

        // given
        Long userId = signUp();

        // when
        MvcResult login = login(email, PASSWORD);

        // then
        String accessToken = accessToken(login);
        Cookie refreshCookie = login.getResponse().getCookie("refreshToken");
        assertNotNull(refreshCookie);
        assertEquals(refreshCookie.getValue(), userRepository.findById(userId).orElseThrow().getRefreshToken());

        mvc.perform(get("/api/user/me").header("Authorization", "Bearer " + accessToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.email").value(email))
            .andExpect(jsonPath("$.data.nickname").value(nickname));
    }

    @Test
    @DisplayName("refresh token 쿠키로 access token을 재발급받는다")
    void reissue() throws Exception {

        // given
        signUp();
        Cookie refreshCookie = login(email, PASSWORD).getResponse().getCookie("refreshToken");

        // when
        MvcResult reissue = mvc.perform(post("/api/auth/reissue").cookie(refreshCookie))
            .andExpect(status().isOk())
            .andReturn();

        // then
        String newAccessToken = accessToken(reissue);
        assertNotNull(reissue.getResponse().getCookie("refreshToken"));

        mvc.perform(get("/api/user/me").header("Authorization", "Bearer " + newAccessToken))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("access token을 refresh token 자리에 넣으면 재발급받을 수 없다")
    void reissueWithAccessToken() throws Exception {

        // given
        signUp();
        String accessToken = accessToken(login(email, PASSWORD));

        // when & then
        mvc.perform(post("/api/auth/reissue").cookie(new Cookie("refreshToken", accessToken)))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    @DisplayName("로그아웃하면 DB의 refresh token이 지워져 같은 쿠키로 재발급받을 수 없다")
    void logout() throws Exception {

        // given
        Long userId = signUp();
        MvcResult login = login(email, PASSWORD);
        String accessToken = accessToken(login);
        Cookie refreshCookie = login.getResponse().getCookie("refreshToken");

        // when
        mvc.perform(post("/api/auth/logout")
                .header("Authorization", "Bearer " + accessToken)
                .cookie(refreshCookie))
            .andExpect(status().isNoContent());

        // then
        assertNull(userRepository.findById(userId).orElseThrow().getRefreshToken());
        mvc.perform(post("/api/auth/reissue").cookie(refreshCookie))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("비밀번호를 변경하면 새 비밀번호로만 로그인할 수 있다")
    void updatePassword() throws Exception {

        // given
        signUp();
        String accessToken = accessToken(login(email, PASSWORD));

        // when
        mvc.perform(patch("/api/user/me/password")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"password":"%s","newPassword":"NewPassw0rd!"}
                    """.formatted(PASSWORD)))
            .andExpect(status().isOk());

        // then
        mvc.perform(post("/api/auth/log-in")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginJson(email, PASSWORD)))
            .andExpect(status().isUnauthorized());
        login(email, "NewPassw0rd!");
    }

    @Test
    @DisplayName("닉네임을 변경하면 DB에 반영된다")
    void updateNickname() throws Exception {

        // given
        Long userId = signUp();
        String accessToken = accessToken(login(email, PASSWORD));
        String newNickname = nickname + "-new";

        // when
        mvc.perform(patch("/api/user/me/nickname")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"nickname":"%s"}
                    """.formatted(newNickname)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.nickname").value(newNickname));

        // then
        assertEquals(newNickname, userRepository.findById(userId).orElseThrow().getNickname());
    }

    @Test
    @DisplayName("탈퇴하면 기존 access token과 refresh token을 쓸 수 없고 다시 로그인할 수 없다")
    void withdraw() throws Exception {

        // given
        Long userId = signUp();
        MvcResult login = login(email, PASSWORD);
        String accessToken = accessToken(login);
        Cookie refreshCookie = login.getResponse().getCookie("refreshToken");

        // when
        mvc.perform(delete("/api/user").header("Authorization", "Bearer " + accessToken))
            .andExpect(status().isOk());

        // then
        User user = userRepository.findById(userId).orElseThrow();
        assertEquals(UserStatus.WITHDRAWN, user.getStatus());
        assertNotNull(user.getDeletedAt());
        assertNull(user.getRefreshToken());

        mvc.perform(get("/api/user/me").header("Authorization", "Bearer " + accessToken))
            .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/reissue").cookie(refreshCookie))
            .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/log-in")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginJson(email, PASSWORD)))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.message").value("탈퇴한 회원입니다."));
    }

    private Long signUp() throws Exception {
        MvcResult result = mvc.perform(post("/api/user/sign-up")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"email":"%s","nickname":"%s","password":"%s","confirmPassword":"%s"}
                    """.formatted(email, nickname, PASSWORD, PASSWORD)))
            .andExpect(status().isCreated())
            .andReturn();

        Number userId = JsonPath.read(result.getResponse().getContentAsString(), "$.data.userId");
        return userId.longValue();
    }

    private MvcResult login(String email, String password) throws Exception {
        return mvc.perform(post("/api/auth/log-in")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginJson(email, password)))
            .andExpect(status().isCreated())
            .andReturn();
    }

    private String loginJson(String email, String password) {
        return """
            {"email":"%s","password":"%s"}
            """.formatted(email, password);
    }

    private String accessToken(MvcResult result) throws Exception {
        return JsonPath.read(result.getResponse().getContentAsString(), "$.data.accessToken");
    }
}
