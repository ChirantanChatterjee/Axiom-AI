package com.axiomai.auth.service;

import org.springframework.stereotype.Service;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.Base64;

@Service
public class PasswordHashService {

    private static final String ALGORITHM =
            "PBKDF2WithHmacSHA256";

    private static final int ITERATIONS =
            185000;

    private static final int KEY_LENGTH =
            256;

    private final SecureRandom secureRandom =
            new SecureRandom();

    public String hash(
            String password
    ) {

        byte[] salt =
                new byte[16];

        secureRandom.nextBytes(salt);

        return "pbkdf2$"
                + ITERATIONS
                + "$"
                + Base64.getEncoder().encodeToString(salt)
                + "$"
                + Base64.getEncoder().encodeToString(
                        derive(
                                password,
                                salt,
                                ITERATIONS
                        )
                );
    }

    public boolean verify(
            String password,
            String storedHash
    ) {

        if (
                password == null
                        ||
                        storedHash == null
                        ||
                        !storedHash.startsWith("pbkdf2$")
        ) {

            return false;
        }

        String[] parts =
                storedHash.split("\\$");

        if (parts.length != 4) {
            return false;
        }

        int iterations =
                Integer.parseInt(parts[1]);

        byte[] salt =
                Base64.getDecoder().decode(parts[2]);

        byte[] expected =
                Base64.getDecoder().decode(parts[3]);

        byte[] actual =
                derive(
                        password,
                        salt,
                        iterations
                );

        if (actual.length != expected.length) {
            return false;
        }

        int difference =
                0;

        for (int i = 0; i < actual.length; i++) {
            difference |= actual[i] ^ expected[i];
        }

        return difference == 0;
    }

    private byte[] derive(
            String password,
            byte[] salt,
            int iterations
    ) {

        try {
            KeySpec spec =
                    new PBEKeySpec(
                            password == null
                                    ? new char[0]
                                    : password.toCharArray(),
                            salt,
                            iterations,
                            KEY_LENGTH
                    );

            return SecretKeyFactory
                    .getInstance(ALGORITHM)
                    .generateSecret(spec)
                    .getEncoded();
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Unable to hash password.",
                    e
            );
        }
    }
}
