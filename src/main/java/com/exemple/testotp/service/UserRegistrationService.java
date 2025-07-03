package com.exemple.testotp.service;

import com.exemple.testotp.dto.PasswordSetupDto;
import com.exemple.testotp.dto.RegistrationResponseDto;
import com.exemple.testotp.dto.UserRegistrationDto;
import com.exemple.testotp.entity.RegistrationSession;
import com.exemple.testotp.entity.User;
import com.exemple.testotp.exception.UserAlreadyExistsException;
import com.exemple.testotp.mapper.UserMapper;
import com.exemple.testotp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserRegistrationService {

    private final UserRepository userRepository;
    private final RegistrationSessionService sessionService;
    private final OtpService otpService;
    private final KeycloakService keycloakService;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public RegistrationResponseDto startRegistration(UserRegistrationDto registrationDto) {
        // Vérifier si l'utilisateur existe déjà
        if (userRepository.existsByEmail(registrationDto.getEmail())) {
            throw new UserAlreadyExistsException("Un utilisateur avec cet email existe déjà");
        }

        if (userRepository.existsByPhoneNumber(registrationDto.getPhoneNumber())) {
            throw new UserAlreadyExistsException("Un utilisateur avec ce numéro de téléphone existe déjà");
        }

        // Créer une session d'inscription
        RegistrationSession session = sessionService.createSession(
                registrationDto.getFirstName(),
                registrationDto.getLastName(),
                registrationDto.getEmail(),
                registrationDto.getPhoneNumber()
        );

        // Générer et envoyer l'OTP
        otpService.generateAndSendOtp(registrationDto.getPhoneNumber());

        log.info("Processus d'inscription démarré pour: {}", registrationDto.getEmail());

        return new RegistrationResponseDto(
                session.getSessionId(),
                "Code OTP envoyé par SMS",
                "VERIFY_OTP"
        );
    }

    @Transactional
    public RegistrationResponseDto verifyOtp(String sessionId, String otpCode) {
        RegistrationSession session = sessionService.getSession(sessionId);

        // Vérifier l'OTP
        otpService.verifyOtp(session.getPhoneNumber(), otpCode);

        // Marquer l'OTP comme vérifié
        sessionService.markOtpVerified(sessionId);

        log.info("OTP vérifié avec succès pour la session: {}", sessionId);

        return new RegistrationResponseDto(
                sessionId,
                "Numéro de téléphone vérifié avec succès",
                "SET_PASSWORD"
        );
    }

    @Transactional
    public RegistrationResponseDto completeRegistration(PasswordSetupDto passwordDto) {
        RegistrationSession session = sessionService.getSession(passwordDto.getSessionId());

        // Vérifier que l'OTP a été vérifié
        if (!session.isOtpVerified()) {
            throw new IllegalStateException("Le numéro de téléphone doit être vérifié avant de définir le mot de passe");
        }

        // Vérifier que les mots de passe correspondent
        if (!passwordDto.getPassword().equals(passwordDto.getConfirmPassword())) {
            throw new IllegalArgumentException("Les mots de passe ne correspondent pas");
        }

        // Créer l'utilisateur dans Keycloak
        String keycloakUserId = keycloakService.createUser(
                session.getFirstName(),
                session.getLastName(),
                session.getEmail(),
                session.getPhoneNumber(),
                passwordDto.getPassword()
        );

        // Créer l'utilisateur en base de données
        User user = new User();
        user.setFirstName(session.getFirstName());
        user.setLastName(session.getLastName());
        user.setEmail(session.getEmail());
        user.setPhoneNumber(session.getPhoneNumber());
        user.setPassword(passwordEncoder.encode(passwordDto.getPassword()));
        user.setPhoneVerified(true);
        user.setActive(true);
        user.setKeycloakUserId(keycloakUserId);

        userRepository.save(user);

        // Marquer la session comme terminée
        sessionService.markCompleted(passwordDto.getSessionId());

        log.info("Inscription terminée avec succès pour: {}", session.getEmail());

        return new RegistrationResponseDto(
                null,
                "Compte créé avec succès",
                "COMPLETED"
        );
    }
}
