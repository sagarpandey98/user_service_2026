package org.example.userservice.service;

import org.example.userservice.dtos.LoginRequestDto;
import org.example.userservice.dtos.SendOtpRequestDto;
import org.example.userservice.model.User;
import org.example.userservice.repository.UserRepository;
import org.example.userservice.security.models.CustomUserDetails;
import org.example.userservice.utils.EmailPhoneIdentifier;
import org.example.userservice.utils.OtpGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class DccAuthService {

    private static final Logger log = LoggerFactory.getLogger(DccAuthService.class);

    private final AuthenticationManager authenticationManager;
    private final JwtEncoder jwtEncoder;
    private final OAuth2AuthorizationService authorizationService;
    private final RegisteredClientRepository registeredClientRepository;
    private final UserRepository userRepository;

    public DccAuthService(AuthenticationManager authenticationManager,
                          JwtEncoder jwtEncoder,
                          OAuth2AuthorizationService authorizationService,
                          RegisteredClientRepository registeredClientRepository,
                          UserRepository userRepository) {

        this.authenticationManager = authenticationManager;
        this.jwtEncoder = jwtEncoder;
        this.authorizationService = authorizationService;
        this.registeredClientRepository = registeredClientRepository;
        this.userRepository = userRepository;
    }

    public String getToken(String username, String password, String clientId) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, password)
            );

            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

            RegisteredClient registeredClient = registeredClientRepository.findByClientId(clientId);
            if (registeredClient == null) {
                log.warn("Client ID not found: {}", clientId);
                throw new IllegalArgumentException("Invalid client ID: " + clientId);
            }

            Instant now = Instant.now();
            long expirySeconds = 3600;

            Set<String> roles = userDetails.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(Collectors.toSet());

            JwtClaimsSet claims = JwtClaimsSet.builder()
                    .issuer("amoga-auth")
                    .issuedAt(now)
                    .expiresAt(now.plusSeconds(expirySeconds))
                    .subject(userDetails.getUsername())
                    .claim("name", userDetails.getName())
                    .claim("roles", roles)
                    .build();

            String jwt = jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();

            if (jwt == null || jwt.isBlank()) {
                log.error("JWT generation failed for user [{}]", userDetails.getUsername());
                throw new RuntimeException("Token generation failed");
            }

            OAuth2AccessToken accessToken = new OAuth2AccessToken(
                    OAuth2AccessToken.TokenType.BEARER,
                    jwt,
                    now,
                    now.plusSeconds(expirySeconds)
            );

            OAuth2Authorization authorization = OAuth2Authorization.withRegisteredClient(registeredClient)
                    .principalName(userDetails.getUsername())
                    .authorizationGrantType(AuthorizationGrantType.PASSWORD)
                    .token(accessToken)
                    .build();

            authorizationService.save(authorization);

            log.info("Token issued successfully for user [{}] and client [{}]", userDetails.getUsername(), clientId);

            return jwt;

        } catch (Exception ex) {
            log.error("Error during token generation for user [{}] and client [{}]: {}", username, clientId, ex.getMessage(), ex);
            throw new RuntimeException("Authentication or token issuance failed: " + ex.getMessage(), ex);
        }
    }

    // 🔐 OTP login (to be implemented)
    public String getTokenByOtp(String email, String otp, String clientId) {
        // TODO: Lookup user by email
        // TODO: Validate OTP against stored value (DB or Redis)
        // TODO: Build JWT using same logic as getToken
        // TODO: Save to authorizationService and return token
        return null;
    }

    // 🔍 For manual validation/debug
    public String testing(String email, String otp, String clientId) {
        RegisteredClient registeredClient = registeredClientRepository.findByClientId(clientId);
        if (registeredClient == null) {
            throw new IllegalArgumentException("Invalid client ID: " + clientId);
        }
        return registeredClient.getClientId();
    }


    public String sendOtp(SendOtpRequestDto request) {
        String identifier = request.getIdentifier();
        try {
            String identifierType = EmailPhoneIdentifier.identify(identifier);
            System.out.println("Identifier Type is " + identifierType);

            if (identifierType.equals("0")) {
                // Call the email OTP service
                System.out.println("Email OTP service called");
                Optional<User> userOptional = userRepository.findByEmail(identifier);
                if (userOptional.isPresent()) {
                    System.out.println("User Already Exist");
                } else {
                    System.out.println("Creating New User");
                    String OTP = OtpGenerator.generateOtp();
                    User user = new User();
                    user.setEmail(identifier);
                    user.setUsername(identifier);
                    user.setOtp(OTP);
                    user.setStatus(UserStatus.UNVERIFIED);
                    userRepository.save(user);

                    // Define the HTML template
                    String htmlTemplate = "<!DOCTYPE html>\n" +
                            "<html>\n" +
                            "<head>\n" +
                            "    <meta charset=\"UTF-8\">\n" +
                            "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
                            "    <title>Welcome to Activity Tracker</title>\n" +
                            "    <style>\n" +
                            "        body {\n" +
                            "            font-family: Arial, sans-serif;\n" +
                            "            background-color: #f4f4f4;\n" +
                            "            margin: 0;\n" +
                            "            padding: 0;\n" +
                            "        }\n" +
                            "        .container {\n" +
                            "            width: 100%;\n" +
                            "            max-width: 600px;\n" +
                            "            margin: 20px auto;\n" +
                            "            background: #ffffff;\n" +
                            "            padding: 20px;\n" +
                            "            border-radius: 10px;\n" +
                            "            box-shadow: 0px 0px 10px rgba(0, 0, 0, 0.1);\n" +
                            "        }\n" +
                            "        .header {\n" +
                            "            text-align: center;\n" +
                            "            background: #007BFF;\n" +
                            "            padding: 20px;\n" +
                            "            color: #ffffff;\n" +
                            "            border-radius: 10px 10px 0 0;\n" +
                            "        }\n" +
                            "        .content {\n" +
                            "            padding: 20px;\n" +
                            "            text-align: center;\n" +
                            "        }\n" +
                            "        .otp {\n" +
                            "            font-size: 24px;\n" +
                            "            font-weight: bold;\n" +
                            "            color: #007BFF;\n" +
                            "            background: #f0f8ff;\n" +
                            "            display: inline-block;\n" +
                            "            padding: 10px 20px;\n" +
                            "            border-radius: 5px;\n" +
                            "            margin-top: 10px;\n" +
                            "        }\n" +
                            "        .footer {\n" +
                            "            text-align: center;\n" +
                            "            padding: 10px;\n" +
                            "            font-size: 12px;\n" +
                            "            color: #777;\n" +
                            "        }\n" +
                            "    </style>\n" +
                            "</head>\n" +
                            "<body>\n" +
                            "    <div class=\"container\">\n" +
                            "        <div class=\"header\">\n" +
                            "            <h2>Welcome to Activity Tracker!</h2>\n" +
                            "        </div>\n" +
                            "        <div class=\"content\">\n" +
                            "            <p>Hi there,</p>\n" +
                            "            <p>Thank you for joining <strong>Activity Tracker</strong>. We are excited to have you on board!</p>\n" +
                            "            <p>Your OTP for login verification is:</p>\n" +
                            "            <p class=\"otp\">{OTP}</p>\n" +
                            "            <p>Please enter this OTP in the app to complete your login.</p>\n" +
                            "            <p>If you didn't request this, please ignore this email.</p>\n" +
                            "        </div>\n" +
                            "        <div class=\"footer\">\n" +
                            "            <p>&copy; 2025 Activity Tracker. All rights reserved.</p>\n" +
                            "        </div>\n" +
                            "    </div>\n" +
                            "</body>\n" +
                            "</html>";

                    // Replace the {OTP} placeholder with the actual OTP
                    String emailBody = htmlTemplate.replace("{OTP}", OTP);

                    SendEmailEventDto sendEmailEventDto = new SendEmailEventDto();
                    sendEmailEventDto.setTo(identifier);
                    sendEmailEventDto.setFrom("sagarbvmdelhi@gmail.com");
                    sendEmailEventDto.setSubject("OTP for Email Verification");
                    sendEmailEventDto.setBody(emailBody);
                    emailEventProducer.sendEmailEvent(sendEmailEventDto);
                }
            } else if (identifierType.equals("Phone Number")) {
                // Call the phone OTP service
                System.out.println("Phone OTP service called");
                sendOtpToPhone(identifier);
            } else {
                throw new OtpException("Invalid Identifier: " + identifier);
            }

        } catch (OtpException e) {
            // Handle specific OTP exceptions
            System.err.println("OTP Error: " + e.getMessage());
            return null; // Or throw a custom response
        } catch (Exception e) {
            // Handle unexpected exceptions
            System.err.println("Unexpected error: " + e.getMessage());
            e.printStackTrace(); // Log full stack trace
            return null; // Or handle accordingly
        }
    }
}
