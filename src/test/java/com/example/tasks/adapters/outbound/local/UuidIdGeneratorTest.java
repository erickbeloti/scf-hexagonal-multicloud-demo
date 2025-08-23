package com.example.tasks.adapters.outbound.local;

// Tests use: JUnit Jupiter (JUnit 5) with core assertions.
// Focus: Validate UUID format, canonical representation, uniqueness, annotations, and interface contract.

import com.example.tasks.application.port.outbound.IdGeneratorPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class UuidIdGeneratorTest {

    private final UuidIdGenerator generator = new UuidIdGenerator();

    @Test
    @DisplayName("generateId returns a well-formed UUID string parseable by UUID.fromString")
    void generateId_returnsValidUuidString() {
        String id = generator.generateId();
        assertNotNull(id, "ID should not be null");
        assertFalse(id.trim().isEmpty(), "ID should not be empty or blank");
        assertDoesNotThrow(() -> UUID.fromString(id), "ID should be a valid UUID string");
    }

    @Test
    @DisplayName("generateId returns canonical UUID string (36 chars, 4 hyphens) and round-trips via UUID.toString")
    void generateId_returnsCanonicalUuidString() {
        String id = generator.generateId();
        assertEquals(36, id.length(), "Canonical UUID string must be 36 characters long");
        long hyphenCount = id.chars().filter(ch -> ch == '-').count();
        assertEquals(4L, hyphenCount, "Canonical UUID string must contain 4 hyphens");
        UUID parsed = assertDoesNotThrow(() -> UUID.fromString(id), "Should parse as UUID");
        assertEquals(id.toLowerCase(), parsed.toString(), "Parsed UUID.toString() should match canonical lower-case form");
    }

    @Test
    @DisplayName("generateId returns RFC 4122 UUID version 4 (variant 2)")
    void generateId_returnsRfc4122V4() {
        UUID uuid = UUID.fromString(generator.generateId());
        assertEquals(4, uuid.getVersion(), "UUID version should be 4");
        assertEquals(2, uuid.getVariant(), "UUID variant should be IETF RFC 4122 (2)");
    }

    @Test
    @DisplayName("generateId produces unique values across multiple invocations")
    void generateId_isUniqueAcrossMultipleCalls() {
        int count = 200;
        Set<String> ids = new HashSet<>(count);
        for (int i = 0; i < count; i++) {
            String id = generator.generateId();
            assertTrue(ids.add(id), "Duplicate ID detected at iteration " + i + ": " + id);
        }
        assertEquals(count, ids.size(), "All generated IDs should be unique");
    }

    @Test
    @DisplayName("Class has Spring @Component and @Profile(\"local\") annotations")
    void annotations_arePresentAndCorrect() {
        Component component = UuidIdGenerator.class.getAnnotation(Component.class);
        assertNotNull(component, "@Component must be present on UuidIdGenerator");

        Profile profile = UuidIdGenerator.class.getAnnotation(Profile.class);
        assertNotNull(profile, "@Profile must be present on UuidIdGenerator");
        assertTrue(Arrays.asList(profile.value()).contains("local"), "@Profile should include 'local'");
    }

    @Test
    @DisplayName("UuidIdGenerator implements IdGeneratorPort and honors its contract")
    void implementsIdGeneratorPortContract() {
        IdGeneratorPort port = generator; // compile-time contract check
        String id = port.generateId();
        assertNotNull(id, "Contract: generateId should return a non-null value");
        assertDoesNotThrow(() -> UUID.fromString(id), "Contract: returned ID should be a valid UUID");
    }

    @Test
    @DisplayName("generateId is safe under light concurrent access (uniqueness preserved)")
    void generateId_threadSafetyLightConcurrency() throws Exception {
        int threads = 8;
        int perThread = 50;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        try {
            Set<String> ids = ConcurrentHashMap.newKeySet();
            Callable<Void> task = () -> {
                for (int i = 0; i < perThread; i++) {
                    String id = generator.generateId();
                    assertTrue(ids.add(id), "Duplicate ID produced under concurrency: " + id);
                }
                return null;
            };

            Future<?>[] futures = new Future<?>[threads];
            for (int t = 0; t < threads; t++) {
                futures[t] = pool.submit(task);
            }
            for (Future<?> f : futures) {
                f.get(5, TimeUnit.SECONDS);
            }

            assertEquals(threads * perThread, ids.size(), "All concurrently generated IDs should be unique");
        } finally {
            pool.shutdownNow();
        }
    }
}