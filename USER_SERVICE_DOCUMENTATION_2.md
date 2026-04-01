Optimizing tool selection...I'll analyze the entire project to generate a comprehensive technical summary. Let me start by reading the key files.   

Searched codebase for "REST API endpoints controller methods request response", 55 results

Read [](file:///Users/sagar/Desktop/Personal/Activity-Tracker/User-Service%202/pom.xml#1-1), lines 1 to 100

Read [](file:///Users/sagar/Desktop/Personal/Activity-Tracker/User-Service%202/src/main/resources/application.properties)

Now let me read the complete pom.xml to ensure I have all dependency versions: 

Read [](file:///Users/sagar/Desktop/Personal/Activity-Tracker/User-Service%202/pom.xml)

Based on my thorough analysis of the project, here is the complete technical summary:

---

USER SERVICE - TECHNICAL SUMMARY

1. WHAT THIS SERVICE DOES

The User Service is a Spring Boot-based OAuth2 Authorization Server that provides user authentication, registration, and profile management. It handles email-based OTP verification for secure user onboarding, JWT token generation for distributed authentication, and integrates with Kafka for asynchronous email delivery. The service acts as the central authentication hub for the Activity Tracker application ecosystem.

---

2. ALL REST API ENDPOINTS

POST /dcc/sendotp
Request Body: { "identifier": "user@example.com" }
Response Body: { "success": true, "message": "OTP sent successfully", "data": null }
Description: Sends a 4-digit OTP to the provided email address

POST /dcc/verify-otp
Request Body: { "email": "user@example.com", "otp": "1234", "clientId": "Activity-Tracker" }
Response Body: { "success": true, "message": "OTP verified successfully", "data": { "isSignedUp": false, "token": "<JWT_TOKEN>" } }
Description: Verifies OTP and returns JWT token with signup status

POST /dcc/signup
Request Body: { "name": "John Doe", "email": "user@example.com", "password": "secure123", "phone": "1234567890", "clientId": "Activity-Tracker" }
Response Body: { "success": true, "message": "Signup successful", "data": { "token": "<JWT_TOKEN>" } }
Description: Completes user registration after OTP verification, creates account with encrypted password

GET /dcc/profile
Headers: Authorization: Bearer <JWT_TOKEN>
Request Body: None
Response Body: { "success": true, "message": "Profile retrieved successfully", "data": { "id": "550e8400-e29b-41d4-a716-446655440000", "username": "user@example.com", "email": "user@example.com", "name": "John Doe", "roles": null, "isEmailVerified": true } }
Description: Retrieves authenticated user profile from JWT token

POST /oauth2/token (Implied OAuth2 endpoint)
Request: OAuth2 client credentials or authorization code
Response: { "access_token": "<JWT>", "token_type": "Bearer", "expires_in": 3600 }
Description: OAuth2 compliant token endpoint for client applications

---

3. ALL DEPENDENCIES IN POM.XML

Spring Boot 3.2.4 (parent)
org.springframework.boot:spring-boot-starter-actuator (version managed by parent)
org.springframework.boot:spring-boot-starter-data-jpa (version managed by parent)
org.springframework.boot:spring-boot-starter-web (version managed by parent)
org.springframework.kafka:spring-kafka (version managed by parent)
org.springframework.boot:spring-boot-devtools (version managed by parent, runtime, optional)
org.postgresql:postgresql (version managed by parent)
org.springframework.boot:spring-boot-configuration-processor (version managed by parent, optional)
org.projectlombok:lombok (version managed by parent, optional)
org.springframework.boot:spring-boot-starter-test (version managed by parent, test scope)
com.h2database:h2 (version managed by parent, test scope)
org.springframework.boot:spring-boot-starter-security (explicit version 3.2.4)
org.apache.commons:commons-lang3 (version 3.14.0)
org.springframework.boot:spring-boot-starter-oauth2-authorization-server (version managed by parent)
org.springframework.kafka:spring-kafka (explicit version 3.1.4)
org.springframework.security:spring-security-oauth2-client (version 6.4.4)

Build Tool: Maven 3.9.5 (via Maven Wrapper)
Java Version: 21

---

4. APPLICATION.PROPERTIES CONFIGURATION

Server Configuration:
  server.port=8082

PostgreSQL Database Configuration:
  spring.datasource.url=jdbc:postgresql://localhost:5432/userService2
  spring.datasource.username=postgres
  spring.datasource.password=Sagar@134
  spring.datasource.driver-class-name=org.postgresql.Driver
  spring.jpa.hibernate.ddl-auto=update
  spring.jpa.show-sql=true

Hibernate Configuration:
  spring.jpa.properties.hibernate.format_sql=true
  spring.jpa.properties.hibernate.type=trace

Kafka Configuration:
  spring.kafka.bootstrap-servers=localhost:9092
  spring.kafka.consumer.group-id=user_service

Logging Configuration:
  logging.level.org.springframework.security=TRACE
  logging.level.org.springframework.security.oauth2=TRACE
  logging.level.org.springframework.security.oauth2.jwt=TRACE
  logging.level.org.springframework.security.oauth2.server=TRACE
  logging.level.org.springframework.security.oauth2.server.resource=TRACE
  logging.level.com.nimbusds=TRACE
  logging.level.org.springframework.security.web.FilterChainProxy=TRACE
  logging.level.org.springframework.security.*=trace
  logging.level.org.springframework.web.*=TRACE
  logging.level.org.hibernate.type.descriptor.sql.BasicBinder=TRACE

Application Name:
  spring.application.name=userservice

---

5. KAFKA USAGE

Producer Classes:
  org.example.userservice.Producer.EmailEventProducer
    - Method: sendEmailEvent(SendEmailEventDto emailDto)
    - Kafka Template: KafkaTemplate<String, SendEmailEventDto>
    - Serialization: JsonSerializer for SendEmailEventDto

Topics Used:
  sendEmail (single topic for all email events)

Messages Sent/Received:
  Topic: sendEmail
  Message Type: SendEmailEventDto
  Fields: to, from, subject, body
  Usage: Sends OTP emails during user registration and verification flows

Kafka Configuration:
  Bootstrap Servers: localhost:9092
  Consumer Group: user_service
  Key Serializer: StringSerializer
  Value Serializer: JsonSerializer

Message Flow:
  1. DccAuthService.sendOtp() generates OTP
  2. Creates SendEmailEventDto with HTML-formatted OTP template
  3. Calls EmailEventProducer.sendEmailEvent()
  4. Message pushed to 'sendEmail' topic
  5. Email Service consumes and sends email via SMTP

---

6. EMAIL SENDING

Email Provider/Library:
  Apache Kafka for asynchronous event publishing (not direct SMTP sending)
  This service produces email events; actual SMTP delivery is handled by a separate Email Service

Email Configuration:
  From Address: sagarbvmdelhi@gmail.com (hardcoded in DccAuthService)
  Subject: OTP for Email Verification
  Content Type: HTML (HTML email template used)

Email Template:
  Subject: "OTP for Email Verification"
  Body HTML: "<html><body><h3>Your OTP for login is: <b>[OTP]</b></h3></body></html>"
  Where [OTP] is replaced with a 4-digit number generated by OtpGenerator

Triggering Points:
  1. /dcc/sendotp endpoint called
  2. OTP generated via OtpGenerator.generateOtp() (1000-9999 range)
  3. OTP stored in User entity with timestamp
  4. SendEmailEventDto created
  5. EmailEventProducer.sendEmailEvent() publishes to Kafka
  6. Downstream Email Service consumes and sends actual email

Email Events Sent:
  - OTP verification email during registration
  - Single email type with OTP content

---

7. ALL JAVA CLASSES WITH DESCRIPTIONS

Model Classes:
  org.example.userservice.model.BaseModel - Abstract base entity with UUID primary key
  org.example.userservice.model.User - JPA entity representing user with authentication details, OTP, roles
  org.example.userservice.model.Role - JPA entity for user roles (future use)

DTO Classes:
  org.example.userservice.dtos.GeneralResponseDto - Standard response wrapper with success flag, message, data
  org.example.userservice.dtos.LoginRequestDto - Request DTO for login with email and password
  org.example.userservice.dtos.SignUpRequestDto - Request DTO for signup with name, email, password, phone, clientId
  org.example.userservice.dtos.SendOtpRequestDto - Request DTO for OTP sending with email identifier
  org.example.userservice.dtos.VerifyOtpRequestDto - Request DTO for OTP verification with email, otp, clientId
  org.example.userservice.dtos.LogoutRequestDto - Request DTO for logout with token
  org.example.userservice.dtos.SendEmailEventDto - Event DTO for Kafka email messages with to, from, subject, body
  org.example.userservice.dtos.UserDto - Response DTO for user profile with id, username, email, name, roles

Controller Classes:
  org.example.userservice.controllers.UserController - REST controller handling /dcc endpoints for auth, signup, OTP, profile

Service Classes:
  org.example.userservice.service.DccAuthService - Core business logic for authentication, token generation, OTP, signup
  org.example.userservice.security.service.CustomUserDetailsService - Implements UserDetailsService for Spring Security
  org.example.userservice.security.service.JpaOAuth2AuthorizationService - OAuth2 authorization persistence
  org.example.userservice.security.service.JpaOAuth2AuthorizationConsentService - OAuth2 authorization consent management

Repository Classes:
  org.example.userservice.repository.UserRepository - JPA repository for User entity queries by email/username
  org.example.userservice.security.repository.ClientRepository - JPA repository for OAuth2 registered clients
  org.example.userservice.security.repository.AuthorizationRepository - JPA repository for OAuth2 authorizations
  org.example.userservice.security.repository.AuthorizationConsentRepository - JPA repository for authorization consents
  org.example.userservice.security.repository.JpaRegisteredClientRepository - Implements RegisteredClientRepository for OAuth2

Configuration Classes:
  org.example.userservice.config.Configuration - Spring configuration class (minimal)
  org.example.userservice.config.KafkaProducerConfig - Kafka producer factory and template bean configuration
  org.example.userservice.security.SecurityConfig - Spring Security and OAuth2 server configuration

Producer Classes:
  org.example.userservice.Producer.EmailEventProducer - Kafka producer for email events

Exception Classes:
  org.example.userservice.exception.OtpException - Runtime exception for OTP validation errors
  org.example.userservice.exception.UserNotFoundException - Exception when user is not found
  org.example.userservice.exception.InvalidRequestException - Exception for malformed requests
  org.example.userservice.exception.TokenNotFoundException - Exception when token is invalid/missing
  org.example.userservice.exception.MailAlreadyExistException - Exception when email already registered
  org.example.userservice.exception.IncorrectPasswordException - Exception for wrong password

Controller Advice Classes:
  org.example.userservice.ControllerAdvices.ExceptionHandler - Global exception handler for custom exceptions

Security Model Classes:
  org.example.userservice.security.models.CustomUserDetails - UserDetails implementation wrapping User entity
  org.example.userservice.security.models.CustomGrantedAuthorities - GrantedAuthority implementation for roles
  org.example.userservice.security.models.Client - JPA entity storing OAuth2 registered client configuration
  org.example.userservice.security.models.Authorization - JPA entity storing OAuth2 authorization sessions
  org.example.userservice.security.models.AuthorizationConsent - JPA entity storing OAuth2 authorization consents

Utility Classes:
  org.example.userservice.utils.OtpGenerator - Generates random 4-digit OTP numbers
  org.example.userservice.utils.EmailPhoneIdentifier - Validates and identifies email vs phone numbers

Application Bootstrap:
  org.example.userservice.UserserviceApplication - Spring Boot application main class

Test Classes:
  org.example.userservice.UserserviceApplicationTests - JUnit test class with client registration test

---

8. HOW THIS SERVICE CONNECTS TO OTHER SERVICES

Direct Integrations:

1. Email Service (via Kafka)
   - This service publishes SendEmailEventDto to 'sendEmail' topic
   - Email Service consumes events and sends SMTP emails
   - Asynchronous, event-driven architecture

2. Activity Tracker Service (or any client application)
   - Location: Not in this codebase
   - Connection: Receives JWT tokens from User Service
   - Method: HTTP REST API calls to /dcc endpoints
   - Uses token from /dcc/verify-otp or /dcc/signup for authentication
   - Validates JWT against /dcc/profile endpoint for user info

3. Authentication/Authorization Consumers
   - Validates JWT tokens issued by this service
   - Uses JwtDecoder to verify tokens
   - Extracts user claims: id, username, email, name, roles

Indirect Integrations:

1. OAuth2 Client Applications (Activity Tracker UI, mobile app, etc.)
   - Registered via SecurityConfig.storeRegisteredClientIntoDb()
   - Client: Activity-Tracker (clientId), secret-based
   - Authentication Grant Types: PASSWORD, REFRESH_TOKEN
   - Scopes: ADMIN
   - Receives access tokens for API interactions

2. Database (PostgreSQL)
   - All user data, credentials, OTPs stored here
   - OAuth2 registrations, authorizations, consents persisted
   - Connection: localhost:5432/userService2

3. Kafka Cluster
   - Event publishing for email notifications
   - Bootstrap Servers: localhost:9092
   - Consumer Group: user_service

Dependency Chain for User Registration:
User -> /dcc/sendotp -> EmailEventProducer -> Kafka 'sendEmail' -> Email Service -> SMTP -> User Email Provider

Dependency Chain for Login:
User -> /dcc/verify-otp -> DccAuthService -> JWT Generation -> Activity Tracker Service (via token)

Dependency Chain for Profile Access:
Activity Tracker Service -> /dcc/profile with Bearer token -> User Service -> JWT Decode -> Return UserDto

---

End of Technical Summary