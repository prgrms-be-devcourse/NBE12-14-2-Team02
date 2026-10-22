package com.prgms.backend.support;

import com.prgms.backend.global.config.SecurityConfig;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;

// Boot 4의 @WebMvcTest는 spring-boot-starter-security-test 없이는 보안 자동설정을 불러오지 않는다.
// 실제 앱과 같은 필터 순서(보안 필터 체인 -> JWT 필터)를 위해 SecurityFilterAutoConfiguration까지 가져온다.
@TestConfiguration
@ImportAutoConfiguration({
    SecurityAutoConfiguration.class,
    ServletWebSecurityAutoConfiguration.class,
    SecurityFilterAutoConfiguration.class
})
@Import(SecurityConfig.class)
public class WebSecurityTestConfig {
}
