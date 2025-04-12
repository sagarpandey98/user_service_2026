package org.example.userservice.controllers;

import org.example.userservice.dtos.LoginRequestDto;
import org.example.userservice.dtos.SendOtpRequestDto;
import org.example.userservice.service.DccAuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/dcc")
public class UserController {

    private final DccAuthService dccAuthService;

    public UserController(DccAuthService dccAuthService) {
        this.dccAuthService = dccAuthService;
    }

    @PostMapping("/sendotp")
    public ResponseEntity<Map<String, Object>> sendOtp(@RequestBody SendOtpRequestDto loginRequestDto) {
        try {
            dccAuthService.sendOtp(loginRequestDto);
            return ResponseEntity.ok(Map.of("message", "OTP sent successfully"));
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to send OTP"));
        }
    }

    // 🔐 Username + Password login (existing)
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody SendOtpRequestDto loginRequestDto) {
        try {
            String jwt = dccAuthService.getToken(
                    loginRequestDto.getEmail(),
                    loginRequestDto.getPassword(),
                    "oidc-client"
            );

            return ResponseEntity.ok(Map.of(
                    "access_token", jwt,
                    "token_type", "Bearer",
                    "expires_in", 3600
            ));
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid credentials"));
        }
    }

    // 🔐 OTP-based login (new)
    @PostMapping("/token/otp")
    public ResponseEntity<Map<String, Object>> loginByOtp(@RequestBody SendOtpRequestDto requestDto) {
        try {
            String jwt = dccAuthService.getTokenByOtp(
                    requestDto.getEmail(),
                    requestDto.getOtp(),
                    "your-client-id"
            );

            return ResponseEntity.ok(Map.of(
                    "access_token", jwt,
                    "token_type", "Bearer",
                    "expires_in", 3600
            ));
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid OTP or user"));
        }
    }
}
