package com.prgms.backend.domain.user.repository;

import com.prgms.backend.domain.user.entity.User;
import com.prgms.backend.domain.user.enums.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsByEmail(String email);
    boolean existsByNickname(String nickname);
    boolean existsByIdAndStatusAndDeletedAtIsNull(Long id, UserStatus status);
    Optional<User> findByEmail(String email);
}
