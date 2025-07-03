package com.exemple.testotp.service;


import com.exemple.testotp.entity.OtpCode;
import com.exemple.testotp.exception.InvalidOtpException;
import com.exemple.testotp.exception.SmsException;
import com.exemple.testotp.repository.OtpCodeRepository;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class OtpService {

    private final OtpCodeRepository otpCodeRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${twilio.phone.number}")
    private String twilioPhoneNumber;

    @Value("${otp.expiration.minutes}")
    private int otpExpirationMinutes;

    @Value("${otp.length}")
    private int otpLength;

    @Value("${twilio.whatsapp.sandbox.number}")
    private String twilioWhatsappNumber;

    @Value("${twilio.whatsapp.sandbox.enabled:true}")
    private boolean whatsappSandboxEnabled;

    @Value("${otp.sms.reminder.delay.minutes:5}")
    private int smsReminderDelayMinutes;

    @Transactional
    public void generateAndSendOtp(String phoneNumber) {
        // Supprimer les anciens codes OTP non utilisés
        otpCodeRepository.deleteByPhoneNumberAndUsed(phoneNumber, false);

        // Générer un nouveau code OTP
        String otpCode = generateOtpCode();

        // Créer l'entité OTP
        OtpCode otp = new OtpCode();
        otp.setPhoneNumber(phoneNumber);
        otp.setCode(otpCode);
        otp.setExpiresAt(LocalDateTime.now().plusMinutes(otpExpirationMinutes));
        otp.setUsed(false);
        otp.setWhatsappSentAt(LocalDateTime.now());

        // Sauvegarder en base
        otpCodeRepository.save(otp);

        // Envoyer le code via WhatsApp
        sendWhatsapp(phoneNumber, otpCode);

        log.info("OTP généré et envoyé par WhatsApp pour le numéro: {}", phoneNumber);
    }

    @Transactional
    public boolean verifyOtp(String phoneNumber, String otpCode) {
        Optional<OtpCode> otpOptional = otpCodeRepository.findValidOtpByPhoneNumber(phoneNumber, LocalDateTime.now());

        if (otpOptional.isEmpty()) {
            throw new InvalidOtpException("Code OTP invalide ou expiré");
        }

        OtpCode otp = otpOptional.get();

        if (!otp.getCode().equals(otpCode)) {
            throw new InvalidOtpException("Code OTP incorrect");
        }

        if (otp.isExpired()) {
            throw new InvalidOtpException("Code OTP expiré");
        }

        // Marquer le code comme utilisé
        otp.setUsed(true);
        otpCodeRepository.save(otp);

        log.info("OTP vérifié avec succès pour le numéro: {}", phoneNumber);
        return true;
    }

    private String generateOtpCode() {
        StringBuilder otp = new StringBuilder();
        for (int i = 0; i < otpLength; i++) {
            otp.append(secureRandom.nextInt(10));
        }
        return otp.toString();
    }

    private void sendWhatsapp(String phoneNumber, String otpCode) {
        try {
            String messageBody = String.format("Votre code de vérification est: %s. Il expire dans %d minutes.",
                    otpCode, otpExpirationMinutes);
            String to = "whatsapp:" + phoneNumber;
            String from = "whatsapp:" + twilioWhatsappNumber;
            Message message = Message.creator(
                    new PhoneNumber(to),
                    new PhoneNumber(from),
                    messageBody
            ).create();
            log.info("WhatsApp envoyé avec succès. SID: {}", message.getSid());
        } catch (Exception e) {
            log.error("Erreur lors de l'envoi du WhatsApp: {}", e.getMessage(), e);
            throw new SmsException("Impossible d'envoyer le message WhatsApp", e);
        }
    }

    @Transactional
    public void cleanupExpiredOtps() {
        otpCodeRepository.deleteByExpiresAtBefore(LocalDateTime.now());
        log.info("Nettoyage des codes OTP expirés terminé");
    }

    public void sendReminderSms(String phoneNumber, String otpCode) {
        try {
            String messageBody = "Vérifiez WhatsApp, le code de vérification a déjà été envoyé. Code: " + otpCode;
            Message message = Message.creator(
                    new PhoneNumber(phoneNumber),
                    new PhoneNumber(twilioPhoneNumber),
                    messageBody
            ).create();
            log.info("SMS de rappel envoyé avec succès. SID: {}", message.getSid());
        } catch (Exception e) {
            log.error("Erreur lors de l'envoi du SMS de rappel: {}", e.getMessage(), e);
            throw new SmsException("Impossible d'envoyer le SMS de rappel", e);
        }
    }

    @Transactional
    public void sendWhatsappReminderIfNotVerified() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime reminderTime = now.minusMinutes(smsReminderDelayMinutes);
        log.info("[DIAG] Début de la vérification des OTP WhatsApp à relancer (heure: {}, délai: {} min)", now, smsReminderDelayMinutes);
        List<OtpCode> otps = otpCodeRepository.findOtpForWhatsappReminder(now, reminderTime);
        log.info("[DIAG] Nombre d'OTP candidats à la relance SMS: {}", otps.size());
        for (OtpCode otp : otps) {
            log.info("[DIAG] OTP à relancer: id={}, phone={}, code={}, whatsappSentAt={}, smsReminderSentAt={}, used={}, expiresAt={}",
                otp.getId(), otp.getPhoneNumber(), otp.getCode(), otp.getWhatsappSentAt(), otp.getSmsReminderSentAt(), otp.isUsed(), otp.getExpiresAt());
            sendReminderSms(otp.getPhoneNumber(), otp.getCode());
            otp.setSmsReminderSentAt(LocalDateTime.now());
            otpCodeRepository.save(otp);
            log.info("SMS de rappel envoyé pour le numéro: {} (OTP: {})", otp.getPhoneNumber(), otp.getCode());
        }
    }
}
