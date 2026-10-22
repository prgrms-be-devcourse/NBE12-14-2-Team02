package com.prgms.backend.domain.user;

import com.prgms.backend.domain.user.entity.User;
import com.prgms.backend.domain.user.enums.UserStatus;
import com.prgms.backend.domain.user.repository.UserRepository;
import com.prgms.backend.support.MySqlContainerSupport;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.Optional;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.dao.DataIntegrityViolationException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

// 엔티티 매핑·기본값·파생 쿼리·제약 조건·프록시를 실제 MySQL로 확인
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class UserRepositoryTest extends MySqlContainerSupport {

    @Autowired private UserRepository userRepository;
    @Autowired private EntityManager entityManager;

    @Test
    @DisplayName("저장한 회원을 다시 조회하면 기본 상태와 생성·수정 시각이 채워져 있다")
    void saveAndFind() {

        // given
        User user = userRepository.save(new User("user@test.com", "회원", "encoded-password"));
        flushAndClear();

        // when
        User found = userRepository.findById(user.getId()).orElseThrow();

        // then
        assertEquals("user@test.com", found.getEmail());
        assertEquals("회원", found.getNickname());
        assertEquals("encoded-password", found.getPassword());
        assertEquals(UserStatus.ACTIVE, found.getStatus());
        assertNull(found.getDeletedAt());
        assertNull(found.getRefreshToken());
        assertNotNull(found.getCreatedAt());
        assertNotNull(found.getUpdatedAt());
    }

    @Test
    @DisplayName("회원 상태는 enum 이름 문자열로 저장한다")
    void statusIsStoredAsString() {

        // given
        User user = userRepository.save(new User("user@test.com", "회원", "encoded-password"));
        flushAndClear();

        // when
        Object status = entityManager
            .createNativeQuery("select status from users where id = :id")
            .setParameter("id", user.getId())
            .getSingleResult();

        // then
        assertEquals("ACTIVE", status);
    }

    @Test
    @DisplayName("탈퇴 처리한 상태 변경이 DB에 반영된다")
    void withdrawStateIsPersisted() {

        // given
        User user = userRepository.save(new User("user@test.com", "회원", "encoded-password"));
        user.updateRefreshToken("refresh-token");
        flushAndClear();

        // when
        User managed = userRepository.findById(user.getId()).orElseThrow();
        managed.updateStatus(UserStatus.WITHDRAWN);
        managed.updateDeletedAt(LocalDateTime.now());
        managed.updateRefreshToken(null);
        flushAndClear();

        // then
        User found = userRepository.findById(user.getId()).orElseThrow();
        assertEquals(UserStatus.WITHDRAWN, found.getStatus());
        assertNotNull(found.getDeletedAt());
        assertNull(found.getRefreshToken());
    }

    @Test
    @DisplayName("이메일과 닉네임 존재 여부를 확인할 수 있다")
    void existsByEmailAndNickname() {

        // given
        userRepository.save(new User("user@test.com", "회원", "encoded-password"));
        flushAndClear();

        // when & then
        assertTrue(userRepository.existsByEmail("user@test.com"));
        assertFalse(userRepository.existsByEmail("none@test.com"));
        assertTrue(userRepository.existsByNickname("회원"));
        assertFalse(userRepository.existsByNickname("없는회원"));
    }

    @Test
    @DisplayName("이메일로 회원을 조회하고 없으면 빈 값을 반환한다")
    void findByEmail() {

        // given
        User user = userRepository.save(new User("user@test.com", "회원", "encoded-password"));
        flushAndClear();

        // when
        Optional<User> found = userRepository.findByEmail("user@test.com");
        Optional<User> notFound = userRepository.findByEmail("none@test.com");

        // then
        assertEquals(user.getId(), found.orElseThrow().getId());
        assertTrue(notFound.isEmpty());
    }

    @Test
    @DisplayName("활성 상태이고 삭제 시각이 없는 회원만 활성 회원으로 판단한다")
    void existsActiveUser() {

        // given
        User active = userRepository.save(new User("active@test.com", "활성", "encoded-password"));

        User withdrawn = userRepository.save(new User("withdrawn@test.com", "탈퇴", "encoded-password"));
        withdrawn.updateStatus(UserStatus.WITHDRAWN);
        withdrawn.updateDeletedAt(LocalDateTime.now());

        // 상태는 ACTIVE지만 삭제 시각만 남은 비정상 데이터도 차단해야 한다
        User deletedOnly = userRepository.save(new User("deleted@test.com", "삭제", "encoded-password"));
        deletedOnly.updateDeletedAt(LocalDateTime.now());
        flushAndClear();

        // when & then
        assertTrue(userRepository.existsByIdAndStatusAndDeletedAtIsNull(active.getId(), UserStatus.ACTIVE));
        assertFalse(userRepository.existsByIdAndStatusAndDeletedAtIsNull(withdrawn.getId(), UserStatus.ACTIVE));
        assertFalse(userRepository.existsByIdAndStatusAndDeletedAtIsNull(deletedOnly.getId(), UserStatus.ACTIVE));
        assertFalse(userRepository.existsByIdAndStatusAndDeletedAtIsNull(-1L, UserStatus.ACTIVE));
    }

    @Test
    @DisplayName("이미 있는 이메일로는 저장할 수 없다")
    void duplicateEmail() {

        // given
        userRepository.saveAndFlush(new User("user@test.com", "회원", "encoded-password"));

        // when & then
        assertThrows(
            DataIntegrityViolationException.class,
            () -> userRepository.saveAndFlush(new User("user@test.com", "다른회원", "encoded-password"))
        );
    }

    @Test
    @DisplayName("이미 있는 닉네임으로는 저장할 수 없다")
    void duplicateNickname() {

        // given
        userRepository.saveAndFlush(new User("user@test.com", "회원", "encoded-password"));

        // when & then
        assertThrows(
            DataIntegrityViolationException.class,
            () -> userRepository.saveAndFlush(new User("other@test.com", "회원", "encoded-password"))
        );
    }

    @Test
    @DisplayName("회원 지연 로딩 프록시를 만들 수 있고 프록시로 필드를 읽을 수 있다")
    void lazyProxy() {

        // given
        // Meeting.host 같은 LAZY 연관관계는 User 프록시에 의존한다.
        // 코틀린에서 클래스나 getter가 final이면 프록시가 만들어지지 않거나 null을 읽게 된다.
        User user = userRepository.save(new User("user@test.com", "회원", "encoded-password"));
        flushAndClear();

        // when
        User reference = userRepository.getReferenceById(user.getId());

        // then
        assertFalse(Hibernate.isInitialized(reference));
        assertEquals(user.getId(), reference.getId());
        assertEquals("user@test.com", reference.getEmail());
        assertTrue(Hibernate.isInitialized(reference));
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }
}
