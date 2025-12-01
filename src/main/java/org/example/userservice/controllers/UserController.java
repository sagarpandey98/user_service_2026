package org.example.userservice.controllers;

import org.example.userservice.dtos.*;
import org.example.userservice.exception.InvalidRequestException;
import org.example.userservice.service.DccAuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("/dcc")
public class UserController {

    private final DccAuthService dccAuthService;

    public UserController(DccAuthService dccAuthService) {
        this.dccAuthService = dccAuthService;
    }

    @PostMapping("/sendotp")
    public ResponseEntity<GeneralResponseDto> sendOtp(@RequestBody SendOtpRequestDto loginRequestDto) {
        try {
            String identifier = loginRequestDto.getIdentifier();
            if (identifier == null || identifier.trim().isEmpty()) {
                throw new InvalidRequestException("Email is required");
            }

            String resolution = dccAuthService.sendOtp(loginRequestDto);
            if (Objects.equals(resolution, "OTP sent successfully")){
                GeneralResponseDto response = new GeneralResponseDto(true, "OTP sent successfully", null);
                return ResponseEntity.ok(response);
            }
            GeneralResponseDto response = new GeneralResponseDto(false, "Error sending OTP", null);
            return ResponseEntity.ok(response);

        } catch (InvalidRequestException ex) {
            GeneralResponseDto errorResponse = new GeneralResponseDto(false, ex.getMessage(), null);
            return ResponseEntity.badRequest().body(errorResponse);
        } catch (Exception ex) {
            GeneralResponseDto errorResponse = new GeneralResponseDto(false, "Failed to send OTP", null);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }



    @PostMapping("/verify-otp")
    public ResponseEntity<GeneralResponseDto> verifyOtp(@RequestBody VerifyOtpRequestDto requestDto) {
        try {
            Map<String, Object> response = dccAuthService.verifyOtpAndLogin(
                    requestDto.getEmail(),
                    requestDto.getOtp(),
                    requestDto.getClientId()
            );
            return ResponseEntity.ok(GeneralResponseDto.success("OTP verified successfully", response));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(GeneralResponseDto.error("Invalid OTP or login failed: " + e.getMessage()));
        }
    }


    @PostMapping("/signup")
    public ResponseEntity<GeneralResponseDto> signup(@RequestBody SignUpRequestDto request) {
        try {
            if (request.getEmail() == null || request.getEmail().isBlank() ||
                    request.getPassword() == null || request.getPassword().isBlank() ||
                    request.getName() == null || request.getName().isBlank() ||
                    request.getClientId() == null || request.getClientId().isBlank()) {
                throw new InvalidRequestException("All fields (name, email, password, clientId) are required");
            }

            Map<String, Object> result = dccAuthService.signup(request, request.getClientId());
            return ResponseEntity.ok(GeneralResponseDto.success("Signup successful", result));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(GeneralResponseDto.error("Signup failed: " + e.getMessage()));
        }
    }

    @GetMapping("/profile")
    public ResponseEntity<GeneralResponseDto> getUserProfile(@RequestHeader("Authorization") String authHeader) {
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                throw new InvalidRequestException("Authorization header is required with Bearer token");
            }

            String token = authHeader.substring(7); // Remove "Bearer " prefix
            UserDto userProfile = dccAuthService.getUserProfileFromToken(token);

            return ResponseEntity.ok(GeneralResponseDto.success("Profile retrieved successfully", userProfile));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(GeneralResponseDto.error("Failed to retrieve profile: " + e.getMessage()));
        }
    }
}
