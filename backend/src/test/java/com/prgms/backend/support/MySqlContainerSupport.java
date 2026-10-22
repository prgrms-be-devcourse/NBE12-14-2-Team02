package com.prgms.backend.support;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

// 여러 테스트 클래스가 컨테이너 하나를 같이 쓰도록 static으로 한 번만 띄운다.
// ddl-auto=validate(application.yml)와 Flyway를 그대로 써서 엔티티와 실제 스키마가 맞는지도 함께 검증한다.
@Testcontainers(disabledWithoutDocker = true)
public abstract class MySqlContainerSupport {

    static final MySQLContainer MYSQL = new MySQLContainer("mysql:8.0")
        .withDatabaseName("backend_test");

    static {
        MYSQL.start();
    }

    @DynamicPropertySource
    static void mysqlProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("custom.jwt.access-secret",
            () -> "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef");
        registry.add("custom.jwt.refresh-secret",
            () -> "abcdef0123456789abcdef0123456789abcdef0123456789abcdef0123456789");
    }
}
