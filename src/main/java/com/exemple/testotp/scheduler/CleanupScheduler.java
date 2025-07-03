package com.exemple.testotp.scheduler;

import com.exemple.testotp.service.OtpService;
import com.exemple.testotp.service.RegistrationSessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class CleanupScheduler {

    private final OtpService otpService;
    private final RegistrationSessionService sessionService;

    @Scheduled(fixedRate = 3600000) // Chaque heure
    public void cleanupExpiredData() {
        log.info("Démarrage du nettoyage des données expirées");

        try {
            otpService.cleanupExpiredOtps();
            sessionService.cleanupExpiredSessions();
            log.info("Nettoyage des données expirées terminé avec succès");
        } catch (Exception e) {
            log.error("Erreur lors du nettoyage des données expirées", e);
        }
    }

    @Scheduled(fixedRate = 60000) // Chaque minute
    public void sendWhatsappReminders() {
        try {
            otpService.sendWhatsappReminderIfNotVerified();
        } catch (Exception e) {
            log.error("Erreur lors de l'envoi des rappels WhatsApp/SMS", e);
        }
    }
}