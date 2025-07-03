package com.exemple.testotp.service;

import com.exemple.testotp.exception.KeycloakException;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
@RequiredArgsConstructor
@Slf4j
public class KeycloakService {

    private final Keycloak keycloak;

    @Value("${keycloak.realm}")
    private String realm;

    public String createUser(String firstName, String lastName, String email, String phoneNumber, String password) {
        try {
            RealmResource realmResource = keycloak.realm(realm);
            UsersResource usersResource = realmResource.users();

            // Créer la représentation utilisateur
            UserRepresentation user = new UserRepresentation();
            user.setUsername(email);
            user.setEmail(email);
            user.setFirstName(firstName);
            user.setLastName(lastName);
            user.setEnabled(true);
            user.setEmailVerified(true);

            // Ajouter le numéro de téléphone comme attribut
            user.setAttributes(Collections.singletonMap("phoneNumber", Collections.singletonList(phoneNumber)));

            // Créer l'utilisateur
            Response response = usersResource.create(user);

            if (response.getStatus() == 201) {
                // Récupérer l'ID de l'utilisateur créé
                String userId = extractUserIdFromResponse(response);

                // Définir le mot de passe
                setUserPassword(usersResource, userId, password);

                log.info("Utilisateur créé avec succès dans Keycloak: {}", email);
                return userId;
            } else {
                throw new KeycloakException("Échec de la création de l'utilisateur. Code: " + response.getStatus());
            }
        } catch (Exception e) {
            log.error("Erreur lors de la création de l'utilisateur dans Keycloak: {}", e.getMessage(), e);
            throw new KeycloakException("Erreur lors de la création de l'utilisateur", e);
        }
    }

    private String extractUserIdFromResponse(Response response) {
        String location = response.getHeaderString("Location");
        return location.substring(location.lastIndexOf('/') + 1);
    }

    private void setUserPassword(UsersResource usersResource, String userId, String password) {
        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue(password);
        credential.setTemporary(false);

        usersResource.get(userId).resetPassword(credential);
    }
}