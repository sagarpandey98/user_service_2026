package org.example.userservice.service;

import org.example.userservice.Producer.EmailEventProducer;
import org.example.userservice.dtos.SendEmailEventDto;
import org.example.userservice.dtos.SendOtpRequestDto;
import org.example.userservice.dtos.SignUpRequestDto;
import org.example.userservice.dtos.UserDto;
import org.example.userservice.exception.OtpException;
import org.example.userservice.model.User;
import org.example.userservice.repository.UserRepository;
import org.example.userservice.utils.EmailPhoneIdentifier;
import org.example.userservice.utils.OtpGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DccAuthService {

    private static final Logger log = LoggerFactory.getLogger(DccAuthService.class);

    @Value("${jwt.issuer.url}")
    private String jwtIssuerUrl;

    private final AuthenticationManager authenticationManager;
    private final JwtEncoder jwtEncoder;
    private final JwtDecoder jwtDecoder;
    private final OAuth2AuthorizationService authorizationService;
    private final RegisteredClientRepository registeredClientRepository;
    private final UserRepository userRepository;
    private final EmailEventProducer emailEventProducer;

    public DccAuthService(AuthenticationManager authenticationManager,
                          JwtEncoder jwtEncoder,
                          JwtDecoder jwtDecoder,
                          OAuth2AuthorizationService authorizationService,
                          RegisteredClientRepository registeredClientRepository,
                          UserRepository userRepository,
                          EmailEventProducer emailEventProducer) {
        this.authenticationManager = authenticationManager;
        this.jwtEncoder = jwtEncoder;
        this.jwtDecoder = jwtDecoder;
        this.authorizationService = authorizationService;
        this.registeredClientRepository = registeredClientRepository;
        this.userRepository = userRepository;
        this.emailEventProducer = emailEventProducer;
    }

    public String getToken(String username, String password, String clientId) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, password)
            );

            var userDetails = (org.example.userservice.security.models.CustomUserDetails) authentication.getPrincipal();
            
            // Fetch the complete user entity to get all required details
            Optional<User> userOpt = userRepository.findByEmail(username);
            if (userOpt.isEmpty()) {
                userOpt = userRepository.findByUsername(username);
            }
            
            if (userOpt.isEmpty()) {
                throw new RuntimeException("User not found");
            }
            
            User user = userOpt.get();
            Set<String> roles = userDetails.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(Collectors.toSet());

            return issueJwtToken(user, clientId, roles);

        } catch (Exception ex) {
            log.error("Token generation error: {}", ex.getMessage(), ex);
            throw new RuntimeException("Authentication or token issuance failed: " + ex.getMessage(), ex);
        }
    }

    private String issueJwtToken(User user, String clientId, Set<String> roles) {
        RegisteredClient registeredClient = registeredClientRepository.findByClientId(clientId);
        if (registeredClient == null) {
            throw new IllegalArgumentException("Invalid client ID: " + clientId);
        }

        Instant now = Instant.now();
        long expirySeconds = 3600;

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(jwtIssuerUrl)
                .issuedAt(now)
                .expiresAt(now.plusSeconds(expirySeconds))
                .claim("id", user.getId().toString()) // UUID as string
                .claim("username", user.getUsername())
                .claim("email", user.getEmail()) // Add email to token
                .claim("name", user.getName())
                .claim("roles", roles)
                .claim("client_id", clientId) // Client ID
                .build();

        String jwt = jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();

        OAuth2AccessToken accessToken = new OAuth2AccessToken(
                OAuth2AccessToken.TokenType.BEARER,
                jwt,
                now,
                now.plusSeconds(expirySeconds)
        );

        OAuth2Authorization authorization = OAuth2Authorization.withRegisteredClient(registeredClient)
                .principalName(user.getUsername())
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                .token(accessToken)
                .build();

        authorizationService.save(authorization);
        return jwt;
    }

    public String sendOtp(SendOtpRequestDto request) {
        String identifier = request.getIdentifier();

        try {
            String identifierType = EmailPhoneIdentifier.identify(identifier);
            if (!"0".equals(identifierType)) {
                throw new UnsupportedOperationException("Only email-based OTP is supported");
            }

            Optional<User> userOptional = userRepository.findByEmail(identifier);
            String otp = OtpGenerator.generateOtp();

            User user = userOptional.orElseGet(() -> {
                User newUser = new User();
                newUser.setName("New User");
                newUser.setEmail(identifier);
                newUser.setUsername(identifier);
                newUser.setHashedPassword("$2a$17$Mjl0gwvuKs9o/2.5bXcL2.O9ZfTwsmBhTnmBwZQTh.KC0HN6Ny/3i"); // Temp
                return newUser;
            });

            user.setOtp(otp);
            user.setOtpGeneratedTime(LocalDateTime.now());
            user.setOtpVerified(false);
            userRepository.save(user);

            SendEmailEventDto email = new SendEmailEventDto();
            email.setTo(identifier);
            email.setFrom("sagarbvmdelhi@gmail.com");
            email.setSubject("OTP for Email Verification");
            email.setBody(otpTemplate(otp));

            emailEventProducer.sendEmailEvent(email);
            return "OTP sent successfully";

        } catch (UnsupportedOperationException e) {
            log.warn("Unsupported identifier [{}]: {}", identifier, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Failed to send OTP to [{}]: {}", identifier, e.getMessage(), e);
            throw new RuntimeException("Failed to send OTP");
        }
    }

    public Map<String, Object> verifyOtpAndLogin(String email, String otp, String clientId) {
        if (email == null || email.trim().isEmpty()) {
            throw new OtpException("Email must not be empty");
        }

        if (otp == null || otp.trim().isEmpty()) {
            throw new OtpException("OTP must not be empty");
        }

        if (clientId == null || clientId.trim().isEmpty()) {
            throw new OtpException("Client ID must not be empty");
        }

        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            throw new OtpException("User not found");
        }

        User user = userOpt.get();

        if (!otp.equals(user.getOtp())) {
            throw new OtpException("Invalid OTP");
        }

        user.setOtpVerified(true);
        userRepository.save(user);

        Set<String> roles = new HashSet<>();
        roles.add("ROLE_USER"); // default role

        String token = issueJwtToken(user, clientId, roles);
        boolean isSignedUp = user.isSignedUp();

        Map<String, Object> response = new HashMap<>();
        response.put("isSignedUp", isSignedUp);
        response.put("token", token);
        return response;
    }

    public Map<String, Object> signup(SignUpRequestDto request, String clientId) {
        Optional<User> optionalUser = userRepository.findByEmail(request.getEmail());

        if (optionalUser.isEmpty()) {
            throw new RuntimeException("User not found. Please verify your email first.");
        }

        User user = optionalUser.get();

        if (!user.isOtpVerified()) {
            throw new RuntimeException("Email is not verified. Please verify OTP first.");
        }

        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(17);
        String hashedPassword = encoder.encode(request.getPassword());

        user.setName(request.getName());
        user.setSignedUp(true);
        user.setHashedPassword(hashedPassword);
        userRepository.save(user);

        String token = getToken(user.getEmail(), request.getPassword(), clientId);

        Map<String, Object> response = new HashMap<>();
        response.put("token", token);
        return response;
    }

    private String otpTemplate(String otp) {
        return "<html><body><h3>Your OTP for login is: <b>" + otp + "</b></h3></body></html>";
    }

    public UserDto getUserProfileFromToken(String token) {
        try {
            Jwt jwt = jwtDecoder.decode(token);
            String userId = jwt.getClaimAsString("id");

            Optional<User> userOptional = userRepository.findById(UUID.fromString(userId));
            if (userOptional.isPresent()) {
                User user = userOptional.get();
                return new UserDto(user.getId(), user.getUsername(), user.getEmail(), user.getName());
            } else {
                throw new RuntimeException("User not found");
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to decode token or find user: " + e.getMessage(), e);
        }
    }
}
