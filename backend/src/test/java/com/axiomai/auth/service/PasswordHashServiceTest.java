package com.axiomai.auth.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordHashServiceTest {

    @Test
    void hashesAndVerifiesPasswords() {

        PasswordHashService service =
                new PasswordHashService();

        String firstHash =
                service.hash("ValidPass123");

        String secondHash =
                service.hash("ValidPass123");

        assertNotEquals(
                firstHash,
                secondHash
        );

        assertTrue(
                service.verify(
                        "ValidPass123",
                        firstHash
                )
        );

        assertFalse(
                service.verify(
                        "WrongPass123",
                        firstHash
                )
        );
    }
}
