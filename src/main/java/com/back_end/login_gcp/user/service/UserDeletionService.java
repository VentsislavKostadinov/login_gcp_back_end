package com.back_end.login_gcp.user.service;

import com.google.cloud.firestore.Firestore;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import java.util.concurrent.ExecutionException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class UserDeletionService {

    private static final String BEARER_PREFIX = "Bearer ";

    private final FirebaseApp firebaseApp;
    private final Firestore firestore;

    public UserDeletionService(FirebaseApp firebaseApp, Firestore firestore) {
        this.firebaseApp = firebaseApp;
        this.firestore = firestore;
    }

    public void deleteCurrentUser(String authorizationHeader) {
        // The uid always comes from the verified token, never from client input, so a user can only delete their own account.
        String uid = verifyIdToken(extractToken(authorizationHeader));

        try {
            firestore.collection("users").document(uid).delete().get();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not delete the user profile");
        } catch (ExecutionException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not delete the user profile");
        }

        try {
            FirebaseAuth.getInstance(firebaseApp).deleteUser(uid);
        } catch (FirebaseAuthException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not delete the user account");
        }
    }

    private String extractToken(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith(BEARER_PREFIX)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing or invalid Authorization header");
        }
        return authorizationHeader.substring(BEARER_PREFIX.length());
    }

    private String verifyIdToken(String idToken) {
        try {
            FirebaseToken decoded = FirebaseAuth.getInstance(firebaseApp).verifyIdToken(idToken);
            return decoded.getUid();
        } catch (FirebaseAuthException exception) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or expired session");
        }
    }
}
