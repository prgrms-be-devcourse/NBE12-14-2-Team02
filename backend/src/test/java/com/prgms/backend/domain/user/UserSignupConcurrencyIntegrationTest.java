package com.prgms.backend.domain.user;

import com.prgms.backend.domain.user.repository.UserRepository;
import com.jayway.jsonpath.JsonPath;
import com.prgms.backend.domain.user.entity.User;
import org.hibernate.exception.ConstraintViolationException;
import java.sql.SQLException;
import com.prgms.backend.support.MySqlContainerSupport;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class UserSignupConcurrencyIntegrationTest extends MySqlContainerSupport {

    @Autowired private MockMvc mvc;
    @MockitoSpyBean private UserRepository users;
    @Autowired private JdbcTemplate jdbc;

    @Test
    @DisplayName("같은 이메일로 동시 가입하면 한 요청은 201, 나머지는 공통 오류 형식의 409다")
    void sameEmailIsStoredOnce() throws Exception {
        String suffix = UUID.randomUUID().toString();
        assertSingleRegistration(List.of(
            new Registration(suffix + "@test.com", "first-" + suffix),
            new Registration(suffix + "@test.com", "second-" + suffix)));
    }

    @Test
    @DisplayName("같은 닉네임으로 동시 가입하면 한 요청은 201, 나머지는 공통 오류 형식의 409다")
    void sameNicknameIsStoredOnce() throws Exception {
        String suffix = UUID.randomUUID().toString();
        assertSingleRegistration(List.of(
            new Registration("first-" + suffix + "@test.com", suffix),
            new Registration("second-" + suffix + "@test.com", suffix)));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {
        "fk_unknown_user", "UK2ty1xmrrgtn89xt7kyxx6ta7h_other",
        "other_table.UK6dotkott2kjsp8vw4d0m25fb7"
    })
    @DisplayName("이메일·닉네임 중복으로 식별되지 않는 DB 오류는 409로 변환하지 않는다")
    void unrelatedDatabaseFailureRemainsServerError(String constraintName) throws Exception {
        doThrow(new DataIntegrityViolationException("내부 DB 오류 상세",
            new ConstraintViolationException("내부 제약 오류", new SQLException("내부 SQL 오류"), constraintName)))
            .when(users).save(any(User.class));
        String suffix = UUID.randomUUID().toString();

        mvc.perform(post("/api/user/sign-up").contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"email":"%s@test.com","nickname":"%s","password":"Passw0rd!","confirmPassword":"Passw0rd!"}
                    """.formatted(suffix, suffix)))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value(500))
            .andExpect(jsonPath("$.message").value("서버 오류가 발생했습니다."))
            .andExpect(jsonPath("$.data").doesNotExist());
    }

    private void assertSingleRegistration(List<Registration> requests) throws Exception {
        CyclicBarrier bothChecksPassed = new CyclicBarrier(requests.size());
        doAnswer(invocation -> {
            // Spring Data의 파생 쿼리는 인터페이스 메서드라 callRealMethod 대신 실제 SQL로 조회한다.
            String nickname = invocation.getArgument(0);
            boolean exists = jdbc.queryForObject("SELECT COUNT(*) FROM users WHERE nickname = ?",
                Integer.class, nickname) > 0;
            assertFalse(exists, "두 요청 모두 INSERT 전에 실제 중복 검사를 통과해야 한다.");
            bothChecksPassed.await(10, TimeUnit.SECONDS);
            return exists;
        }).when(users).existsByNickname(anyString());

        var executor = Executors.newFixedThreadPool(requests.size());
        try {
            List<Future<MvcResult>> futures = requests.stream().map(request -> executor.submit(() ->
                mvc.perform(post("/api/user/sign-up").contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {"email":"%s","nickname":"%s","password":"Passw0rd!","confirmPassword":"Passw0rd!"}
                        """.formatted(request.email(), request.nickname()))).andReturn())).toList();
            var results = new java.util.ArrayList<MvcResult>();
            for (Future<MvcResult> future : futures) {
                results.add(future.get(20, TimeUnit.SECONDS));
            }
            Registration first = requests.get(0);
            Registration second = requests.get(1);
            assertEquals(1, jdbc.queryForObject("""
                SELECT COUNT(*) FROM users WHERE email IN (?, ?) OR nickname IN (?, ?)
                """, Integer.class, first.email(), second.email(), first.nickname(), second.nickname()));
            assertEquals(List.of(201, 409), results.stream()
                .map(result -> result.getResponse().getStatus()).sorted().toList());
            for (MvcResult result : results) {
                String body = result.getResponse().getContentAsString();
                int status = result.getResponse().getStatus();
                assertEquals(status, ((Number) JsonPath.read(body, "$.code")).intValue());
                assertEquals(status == 201, (boolean) JsonPath.read(body, "$.success"));
                if (status == 409) {
                    assertEquals("이미 존재하는 이메일 또는 닉네임이 사용되었습니다.", JsonPath.read(body, "$.message"));
                    assertNull(JsonPath.read(body, "$.data"));
                } else {
                    assertNotNull(JsonPath.read(body, "$.data.userId"));
                }
            }
        } finally {
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
        }
    }

    private record Registration(String email, String nickname) {}
}
