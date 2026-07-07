package com.inkpulse.features.auth.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.inkpulse.features.auth.dto.GoogleUserPayload;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;

@Service
public class GoogleService {

    private final GoogleIdTokenVerifier verifier;

    public GoogleService(@Value("${google.client-id}") String clientId) {
        this.verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), GsonFactory.getDefaultInstance())
                .setAudience(Collections.singletonList(clientId))
                .build();
    }

    public GoogleUserPayload validateToken(String idTokenString) throws GeneralSecurityException, IOException {
        GoogleIdToken idToken = verifier.verify(idTokenString);
        if (idToken == null) {
            throw new IllegalArgumentException("Invalid Google ID Token");
        }

        GoogleIdToken.Payload payload = idToken.getPayload();

        return GoogleUserPayload.builder()
                .googleUserId(payload.getSubject())
                .email(payload.getEmail())
                .emailVerified(payload.getEmailVerified())
                .name((String) payload.get("name"))
                .givenName((String) payload.get("given_name"))
                .familyName((String) payload.get("family_name"))
                .picture((String) payload.get("picture"))
                .build();
    }
}
