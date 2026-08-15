package com.back_end.login_gcp;

import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.cloud.FirestoreClient;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FirebaseConfiguration {

    @Value("${firebase.project-id}")
    private String projectId;

    @Bean
    FirebaseApp firebaseApp() throws IOException {
        if (!FirebaseApp.getApps().isEmpty()) {
            return FirebaseApp.getInstance();
        }

        // Credentials come from GOOGLE_APPLICATION_CREDENTIALS (local) or the
        // attached service account (Cloud Run) - never bundled in the repo.
        FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.getApplicationDefault())
                .setProjectId(projectId)
                // firebase-admin 9.x defaults to an HTTP/2 Apache transport that returns
                // empty 400 bodies for Identity Toolkit errors on some networks; force HTTP/1.1.
                .setHttpTransport(new NetHttpTransport())
                .build();

        return FirebaseApp.initializeApp(options);
    }

    @Bean
    Firestore firestore(FirebaseApp firebaseApp) {
        return FirestoreClient.getFirestore(firebaseApp, "users");
    }
}