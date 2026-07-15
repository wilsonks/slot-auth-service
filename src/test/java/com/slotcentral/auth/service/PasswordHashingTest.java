package com.slotcentral.auth.service;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;

class PasswordHashingTest {

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Test
    void shouldHashAndVerifyPassword() {
        String raw = "SecurePass123!";
        String hash = passwordEncoder.encode(raw);
        assertNotNull(hash);
        assertNotEquals(raw, hash);
        assertTrue(passwordEncoder.matches(raw, hash));
    }

    @Test
    void shouldNotMatchWrongPassword() {
        String hash = passwordEncoder.encode("correct");
        assertFalse(passwordEncoder.matches("wrong", hash));
    }

    @Test
    void shouldProduceDifferentHashesForSameInput() {
        String raw = "samePassword";
        String hash1 = passwordEncoder.encode(raw);
        String hash2 = passwordEncoder.encode(raw);
        assertNotEquals(hash1, hash2);
        assertTrue(passwordEncoder.matches(raw, hash1));
        assertTrue(passwordEncoder.matches(raw, hash2));
    }
}
