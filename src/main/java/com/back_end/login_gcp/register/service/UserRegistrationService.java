package com.back_end.login_gcp.register.service;

import com.back_end.login_gcp.register.dto.RegisterUserRequest;
import com.back_end.login_gcp.register.dto.RegisterUserResponse;
import com.google.cloud.firestore.FieldValue;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.UserRecord;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class UserRegistrationService {

    private static final Logger log = LoggerFactory.getLogger(UserRegistrationService.class);

    private final FirebaseApp firebaseApp;
    private final Firestore firestore;

    public UserRegistrationService(FirebaseApp firebaseApp, Firestore firestore) {
        this.firebaseApp = firebaseApp;
        this.firestore = firestore;
    }

    public RegisterUserResponse register(RegisterUserRequest request) {
        if (!request.password().equals(request.repeatPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Passwords do not match");
        }

        UserRecord user = createAuthenticationUser(request);

        try {
            firestore.collection("users")
                    .document(user.getUid())
                    .set(Map.of(
                            "email", user.getEmail(),
                            "createdAt", FieldValue.serverTimestamp()))
                    .get();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            deleteAuthenticationUser(user.getUid());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not save the user profile");
        } catch (ExecutionException exception) {
            deleteAuthenticationUser(user.getUid());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not save the user profile");
        }

        return new RegisterUserResponse(user.getUid(), user.getEmail());
    }

    private UserRecord createAuthenticationUser(RegisterUserRequest request) {
        try {
            return FirebaseAuth.getInstance(firebaseApp).createUser(new UserRecord.CreateRequest()
                    .setEmail(request.email())
                    .setPassword(request.password()));
        } catch (FirebaseAuthException exception) {
            log.warn("Firebase user creation failed: errorCode={}, message={}",
                    exception.getAuthErrorCode(), exception.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email is invalid or already registered");
        }
    }

    private void deleteAuthenticationUser(String uid) {
        try {
            FirebaseAuth.getInstance(firebaseApp).deleteUser(uid);
        } catch (FirebaseAuthException ignored) {
        }
    }
}