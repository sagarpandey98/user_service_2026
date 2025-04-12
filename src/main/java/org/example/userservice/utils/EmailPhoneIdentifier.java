package org.example.userservice.utils;

import java.util.regex.Pattern;

public class EmailPhoneIdentifier {

    // Regular expression for email validation
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$"
    );

    // Regular expression for phone number validation (country code + phone number)
    private static final Pattern PHONE_PATTERN = Pattern.compile(
            "^\\+\\d{1,3}\\d{10}$"
    );

    public static String identify(String input) {
        if (EMAIL_PATTERN.matcher(input).matches()) {
            return "0";
        } else if (PHONE_PATTERN.matcher(input).matches()) {
            return "1";
        } else {
            return "-1";
        }
    }
}