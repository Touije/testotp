package com.exemple.testotp.controller;

import com.exemple.testotp.dto.OtpVerificationDto;
import com.exemple.testotp.dto.PasswordSetupDto;
import com.exemple.testotp.dto.RegistrationResponseDto;
import com.exemple.testotp.dto.UserRegistrationDto;
import com.exemple.testotp.service.UserRegistrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Authentication", description = "API d'authentification et d'inscription")
public class AuthController {

    private final UserRegistrationService userRegistrationService;

    @PostMapping("/register/start")
    @Operation(summary = "Démarrer l'inscription", description = "Commence le processus d'inscription en envoyant un OTP par SMS")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "OTP envoyé avec succès"),
            @ApiResponse(responseCode = "409", description = "L'utilisateur existe déjà"),
            @ApiResponse(responseCode = "400", description = "Données invalides")
    })
    public ResponseEntity<com.exemple.testotp.dto.ApiResponse<RegistrationResponseDto>> startRegistration(
            @Valid @RequestBody UserRegistrationDto registrationDto) {

        log.info("Démarrage de l'inscription pour: {}", registrationDto.getEmail());

        RegistrationResponseDto response = userRegistrationService.startRegistration(registrationDto);

        return ResponseEntity.ok(
                com.exemple.testotp.dto.ApiResponse.success("Processus d'inscription démarré", response)
        );
    }

    @PostMapping("/register/verify-otp")
    @Operation(summary = "Vérifier l'OTP", description = "Vérifie le code OTP reçu par SMS")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "OTP vérifié avec succès"),
            @ApiResponse(responseCode = "400", description = "Code OTP invalide"),
            @ApiResponse(responseCode = "404", description = "Session non trouvée"),
            @ApiResponse(responseCode = "410", description = "Session expirée")
    })
    public ResponseEntity<com.exemple.testotp.dto.ApiResponse<RegistrationResponseDto>> verifyOtp(
            @Valid @RequestBody OtpVerificationDto otpDto) {

        log.info("Vérification OTP pour la session: {}", otpDto.getSessionId());

        RegistrationResponseDto response = userRegistrationService.verifyOtp(
                otpDto.getSessionId(),
                otpDto.getOtpCode()
        );

        return ResponseEntity.ok(
                com.exemple.testotp.dto.ApiResponse.success("OTP vérifié avec succès", response)
        );
    }

    @PostMapping("/register/complete")
    @Operation(summary = "Terminer l'inscription", description = "Définit le mot de passe et termine l'inscription")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Compte créé avec succès"),
            @ApiResponse(responseCode = "400", description = "Données invalides ou mots de passe différents"),
            @ApiResponse(responseCode = "404", description = "Session non trouvée"),
            @ApiResponse(responseCode = "500", description = "Erreur lors de la création du compte")
    })
    public ResponseEntity<com.exemple.testotp.dto.ApiResponse<RegistrationResponseDto>> completeRegistration(
            @Valid @RequestBody PasswordSetupDto passwordDto) {

        log.info("Finalisation de l'inscription pour la session: {}", passwordDto.getSessionId());

        RegistrationResponseDto response = userRegistrationService.completeRegistration(passwordDto);

        return ResponseEntity.status(HttpStatus.CREATED).body(
                com.exemple.testotp.dto.ApiResponse.success("Compte créé avec succès", response)
        );
    }
}
