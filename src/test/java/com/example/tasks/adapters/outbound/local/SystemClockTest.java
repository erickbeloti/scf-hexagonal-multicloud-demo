// Using JUnit 4 for tests in this file (fallback if Jupiter not detected).
package com.example.tasks.adapters.outbound.local;

/*
 Note: Detected testing framework: JUnit 4 (fallback).
 We use org.junit.Assert assertions and Spring's AnnotationConfigApplicationContext for profile-based bean tests.
*/

import com.example.tasks.application.port.outbound.ClockPort;
import org.junit.Test;

import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Arrays;

import static org.junit.Assert.*;

public class SystemClockTest {

    @Test
    public void nowReturnsTimeWithinBounds() {
        SystemClock clock = new SystemClock();

        LocalDateTime before = LocalDateTime.now();
        LocalDateTime value = clock.now();
        LocalDateTime after = LocalDateTime.now();

        assertNotNull("now() should not return null", value);
        assertFalse("now() should not be before the 'before' instant", value.isBefore(before));
        assertFalse("now() should not be after the 'after' instant", value.isAfter(after));
    }

    @Test
    public void implementsClockPortAndNonDecreasing() {
        ClockPort clockPort = new SystemClock();

        LocalDateTime first = clockPort.now();
        LocalDateTime second = clockPort.now();

        assertTrue("Second now() call should be equal or after the first call", !second.isBefore(first));
    }

    @Test
    public void hasExpectedAnnotations() {
        assertTrue("@Component annotation expected on SystemClock",
                SystemClock.class.isAnnotationPresent(Component.class));

        Profile profile = SystemClock.class.getAnnotation(Profile.class);
        assertNotNull("@Profile annotation expected on SystemClock", profile);
        assertTrue("Expected @Profile to include \"local\"", Arrays.asList(profile.value()).contains("local"));
    }

    @Test
    public void beanAvailableWhenLocalProfileActive() {
        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
        try {
            ctx.getEnvironment().setActiveProfiles("local");
            ctx.scan("com.example.tasks.adapters.outbound.local");
            ctx.refresh();

            ClockPort bean = ctx.getBean(ClockPort.class);
            assertNotNull("ClockPort bean should be present when 'local' profile is active", bean);
            assertTrue("ClockPort bean should be an instance of SystemClock", bean instanceof SystemClock);
        } finally {
            ctx.close();
        }
    }

    @Test
    public void beanNotAvailableWhenNonLocalProfileActive() {
        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
        try {
            ctx.getEnvironment().setActiveProfiles("prod", "test");
            ctx.scan("com.example.tasks.adapters.outbound.local");
            ctx.refresh();

            try {
                ctx.getBean(ClockPort.class);
                fail("ClockPort bean should NOT be present when 'local' profile is not active");
            } catch (NoSuchBeanDefinitionException expected) {
                // expected
            }
        } finally {
            ctx.close();
        }
    }
}