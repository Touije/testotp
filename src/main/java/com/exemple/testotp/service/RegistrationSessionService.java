package com.exemple.testotp.service;

import com.exemple.testotp.entity.RegistrationSession;
import com.exemple.testotp.exception.SessionExpiredException;
import com.exemple.testotp.exception.SessionNotFoundException;
import com.exemple.testotp.repository.RegistrationSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class RegistrationSessionService {

    private final RegistrationSessionRepository sessionRepository;
    private static final int SESSION_EXPIRATION_HOURS = 2;

    @Transactional
    public RegistrationSession createSession(String firstName, String lastName, String email, String phoneNumber) {
        RegistrationSession session = new RegistrationSession();
        session.setSessionId(UUID.randomUUID().toString());
        session.setFirstName(firstName);
        session.setLastName(lastName);
        session.setEmail(email);
        session.setPhoneNumber(phoneNumber);
        session.setOtpVerified(false);
        session.setCompleted(false);
        session.setExpiresAt(LocalDateTime.now().plusHours(SESSION_EXPIRATION_HOURS));

        RegistrationSession savedSession = sessionRepository.save(session);
        log.info("Session d'inscription créée: {}", savedSession.getSessionId());
        return savedSession;
    }

    public RegistrationSession getSession(String sessionId) {
        Optional<RegistrationSession> sessionOptional = sessionRepository.findBySessionId(sessionId);

        if (sessionOptional.isEmpty()) {
            throw new SessionNotFoundException("Session non trouvée");
        }

        RegistrationSession session = sessionOptional.get();

        if (session.isExpired()) {
            throw new SessionExpiredException("Session expirée");
        }

        return session;
    }

    @Transactional
    public void markOtpVerified(String sessionId) {
        RegistrationSession session = getSession(sessionId);
        session.setOtpVerified(true);
        sessionRepository.save(session);
        log.info("OTP marqué comme vérifié pour la session: {}", sessionId);
    }

    @Transactional
    public void markCompleted(String sessionId) {
        RegistrationSession session = getSession(sessionId);
        session.setCompleted(true);
        sessionRepository.save(session);
        log.info("Session marquée comme terminée: {}", sessionId);
    }

    @Transactional
    public void cleanupExpiredSessions() {
        sessionRepository.deleteByExpiresAtBefore(LocalDateTime.now());
        sessionRepository.deleteByCompleted(true);
        log.info("Nettoyage des sessions expirées terminé");
    }
}
