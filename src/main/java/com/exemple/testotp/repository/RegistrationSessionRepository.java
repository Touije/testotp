package com.exemple.testotp.repository;

import com.exemple.testotp.entity.RegistrationSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface RegistrationSessionRepository extends JpaRepository<RegistrationSession, Long> {
    Optional<RegistrationSession> findBySessionId(String sessionId);
    void deleteByExpiresAtBefore(LocalDateTime dateTime);
    void deleteByCompleted(boolean completed);
}
