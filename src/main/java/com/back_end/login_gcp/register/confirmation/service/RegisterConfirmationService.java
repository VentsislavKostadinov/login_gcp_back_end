package com.back_end.login_gcp.register.confirmation.service;

import com.back_end.login_gcp.register.confirmation.dto.ConfirmRegistrationRequest;
import com.back_end.login_gcp.register.confirmation.dto.ConfirmRegistrationResponse;
import com.back_end.login_gcp.register.dto.RegisterUserResponse;
import com.google.cloud.Timestamp;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.FieldValue;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.UserRecord;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class RegisterConfirmationService {

    private static final Logger log = LoggerFactory.getLogger(RegisterConfirmationService.class);
    private static final String COLLECTION = "email_confirmations";
    private static final long CODE_TTL_MINUTES = 10;

    private final SecureRandom secureRandom = new SecureRandom();

    private final Firestore firestore;
    private final JavaMailSender mailSender;
    private final FirebaseApp firebaseApp;
    private final String fromAddress;

    public RegisterConfirmationService(Firestore firestore, JavaMailSender mailSender, FirebaseApp firebaseApp,
            @Value("${mail.from}") String fromAddress) {
        this.firestore = firestore;
        this.mailSender = mailSender;
        this.firebaseApp = firebaseApp;
        this.fromAddress = fromAddress;
    }

    public ConfirmRegistrationResponse sendConfirmationCode(String email, String password) {
        String code = generateCode();
        storePendingRegistration(email, password, code);

        try {
            sendEmail(email, code);
        } catch (MailException exception) {
            log.warn("Could not send confirmation email to {}: {}", email, exception.getMessage());
            deletePendingRegistration(email);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Could not send the confirmation email");
        }

        return new ConfirmRegistrationResponse(email, CODE_TTL_MINUTES);
    }

    public RegisterUserResponse confirmRegistration(ConfirmRegistrationRequest request) {
        DocumentSnapshot snapshot = getPendingRegistration(request.email());
        if (!snapshot.exists()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No pending registration found for this email");
        }

        Timestamp expiresAt = snapshot.getTimestamp("expiresAt");
        if (expiresAt == null || Instant.now().isAfter(expiresAt.toDate().toInstant())) {
            deletePendingRegistration(request.email());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Confirmation code has expired");
        }

        String storedCode = snapshot.getString("code");
        if (storedCode == null || !storedCode.equals(request.code())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid confirmation code");
        }

        UserRecord user = createAuthenticationUser(request.email(), snapshot.getString("password"));

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

        deletePendingRegistration(request.email());
        return new RegisterUserResponse(user.getUid(), user.getEmail());
    }

    private String generateCode() {
        return String.format("%06d", secureRandom.nextInt(1_000_000));
    }

    private void storePendingRegistration(String email, String password, String code) {
        Instant expiresAt = Instant.now().plus(CODE_TTL_MINUTES, ChronoUnit.MINUTES);
        try {
            firestore.collection(COLLECTION)
                    .document(email)
                    .set(Map.of(
                            "password", password,
                            "code", code,
                            "createdAt", FieldValue.serverTimestamp(),
                            "expiresAt", Timestamp.of(Date.from(expiresAt))))
                    .get();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not create the confirmation code");
        } catch (ExecutionException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not create the confirmation code");
        }
    }

    private DocumentSnapshot getPendingRegistration(String email) {
        try {
            return firestore.collection(COLLECTION).document(email).get().get();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not verify the confirmation code");
        } catch (ExecutionException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not verify the confirmation code");
        }
    }

    private void deletePendingRegistration(String email) {
        try {
            firestore.collection(COLLECTION).document(email).delete().get();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        } catch (ExecutionException ignored) {
        }
    }

    private void sendEmail(String email, String code) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(email);
        message.setSubject("Your confirmation code");
        message.setText("Your confirmation code is " + code + ". It expires in " + CODE_TTL_MINUTES + " minutes.");
        mailSender.send(message);
    }

    private UserRecord createAuthenticationUser(String email, String password) {
        try {
            return FirebaseAuth.getInstance(firebaseApp).createUser(new UserRecord.CreateRequest()
                    .setEmail(email)
                    .setPassword(password));
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
