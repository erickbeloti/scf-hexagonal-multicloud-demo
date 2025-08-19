package com.example.tasks.config;

import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile(Profiles.GCP)
public class FirestoreConfig {

    @Bean
    public Firestore firestore() {
        return FirestoreOptions.getDefaultInstance().getService();
    }
}
