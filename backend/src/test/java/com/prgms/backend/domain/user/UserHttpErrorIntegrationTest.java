package com.prgms.backend.domain.user;

import com.prgms.backend.support.MySqlContainerSupport;
import com.jayway.jsonpath.JsonPath;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class UserHttpErrorIntegrationTest extends MySqlContainerSupport {

    @Autowired private MockMvc mvc;

    @ParameterizedTest(name = "{0}")
    @MethodSource("badRequests")
    @DisplayName("잘못된 요청은 500 대신 400과 공통 오류 형식으로 응답한다")
    void badRequestUsesCommonErrorResponse(String description, RequestBuilder request) throws Exception {
        mvc.perform(request)
            .andExpect(status().isBadRequest())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value(400))
            .andExpect(jsonPath("$.message", not(emptyOrNullString())))
            .andExpect(jsonPath("$.data").doesNotExist());
    }

    static Stream<Arguments> badRequests() {
        return Stream.of(
            Arguments.of("이메일 중복 확인의 필수 파라미터 누락", get("/api/user/check-email")),
            Arguments.of("닉네임 중복 확인의 필수 파라미터 누락", get("/api/user/check-nickname")),
            Arguments.of("회원가입 JSON 문법 오류", post("/api/user/sign-up")
                .contentType(MediaType.APPLICATION_JSON).content("{")),
            Arguments.of("로그인 요청 본문 누락", post("/api/auth/log-in")
                .contentType(MediaType.APPLICATION_JSON)),
            Arguments.of("회원가입 본문에 객체 대신 배열 전달", post("/api/user/sign-up")
                .contentType(MediaType.APPLICATION_JSON).content("[]"))
        );
    }

    @Test
    @DisplayName("지원하지 않는 HTTP 메서드는 405와 공통 오류 형식 및 Allow 헤더로 응답한다")
    void methodNotAllowed() throws Exception {
        mvc.perform(post("/api/user/check-email").param("email", "user@test.com"))
            .andExpect(status().isMethodNotAllowed())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value(405))
            .andExpect(jsonPath("$.message", not(emptyOrNullString())))
            .andExpect(header().string(HttpHeaders.ALLOW, containsString("GET")));
    }

    @Test
    @DisplayName("지원하지 않는 Content-Type은 415와 공통 오류 형식으로 응답한다")
    void unsupportedMediaType() throws Exception {
        mvc.perform(post("/api/user/sign-up").contentType(MediaType.TEXT_PLAIN).content("text"))
            .andExpect(status().isUnsupportedMediaType())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value(415))
            .andExpect(jsonPath("$.message", not(emptyOrNullString())));
    }

    @Test
    @DisplayName("인증된 사용자가 존재하지 않는 API를 요청하면 404와 공통 오류 형식으로 응답한다")
    void missingEndpoint() throws Exception {
        String suffix = UUID.randomUUID().toString();
        String email = suffix + "@test.com";
        mvc.perform(post("/api/user/sign-up").contentType(MediaType.APPLICATION_JSON).content("""
                {"email":"%s","nickname":"%s","password":"Passw0rd!","confirmPassword":"Passw0rd!"}
                """.formatted(email, suffix)))
            .andExpect(status().isCreated());
        String loginBody = mvc.perform(post("/api/auth/log-in").contentType(MediaType.APPLICATION_JSON).content("""
                {"email":"%s","password":"Passw0rd!"}
                """.formatted(email)))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String accessToken = JsonPath.read(loginBody, "$.data.accessToken");
        mvc.perform(get("/api/user/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
            .andExpect(status().isOk());

        mvc.perform(get("/api/user/not-existing-endpoint")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value(404))
            .andExpect(jsonPath("$.message", not(emptyOrNullString())));
    }
}
