package org.example.userservice.utils;

import java.security.SecureRandom;

public class OtpGenerator {
    private static final SecureRandom random = new SecureRandom();

    public static String generateOtp() {
        int otp = 1000 + random.nextInt(9000); // Generates a random 4-digit number
        return String.valueOf(otp);
    }
}