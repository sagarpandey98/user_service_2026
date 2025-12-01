# User Service Documentation

## Overview

The User Service is a Spring Boot-based microservice that provides comprehensive user authentication and management capabilities for the Activity Tracker application. It implements OAuth2 authorization server functionality with JWT token-based authentication, email verification via OTP, and user profile management.

## What This Service Does

### Core Functionalities

1. **User Registration & Authentication**
   - Email-based user registration with OTP verification
   - Password-based login with JWT token generation
   - Secure password hashing using BCrypt

2. **OAuth2 Authorization Server**
   - Issues JWT access tokens with comprehensive user claims
   - Manages OAuth2 authorizations and registered clients
   - Token validation and user profile extraction

3. **Email Verification System**
   - Generates and sends OTP via email for user verification
   - Validates OTP for secure user onboarding
   - Kafka-based email event publishing

4. **User Profile Management**
   - User profile creation and updates
   - Token-based profile retrieval
   - User data persistence with UUID-based identification

## Architecture

### High-Level Architecture

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Client Apps   │───▶│   User Service  │───▶│    Database     │
│  (Web/Mobile)   │    │  (Spring Boot)  │    │   (JPA/H2)      │
└─────────────────┘    └─────────────────┘    └─────────────────┘
                              │
                              ▼
                       ┌─────────────────┐
                       │  Kafka Broker   │
                       │ (Email Events)  │
                       └─────────────────┘
```

### Technology Stack

- **Framework**: Spring Boot 3.x
- **Security**: Spring Security with OAuth2 Authorization Server
- **Authentication**: JWT (JSON Web Tokens)
- **Database**: JPA with Hibernate ORM
- **Messaging**: Apache Kafka for email events
- **Password Encryption**: BCrypt
- **Build Tool**: Maven

### Package Structure

```
org.example.userservice/
├── config/                    # Configuration classes
├── ControllerAdvices/         # Global exception handling
├── controllers/               # REST API endpoints
├── dtos/                     # Data Transfer Objects
├── exception/                # Custom exceptions
├── model/                    # JPA entities
├── Producer/                 # Kafka producers
├── repository/               # Data access layer
├── security/                 # Security configurations
├── service/                  # Business logic
└── utils/                    # Utility classes
```

## Detailed Architecture Components

### 1. Data Layer

#### Models
- **BaseModel**: Abstract class with UUID primary key
- **User**: Main user entity with authentication details
- **Role**: User role management (future enhancement)

#### Repositories
- **UserRepository**: JPA repository for user data operations
- Extends `JpaRepository<User, UUID>`
- Custom query methods for finding users by email/username

### 2. Security Layer

#### Components
- **SecurityConfig**: Spring Security configuration
- **CustomUserDetails**: Custom user details implementation
- **JWT Integration**: Token generation, validation, and claims extraction

#### Authentication Flow
1. User provides credentials (email/password)
2. Spring Security authenticates via `AuthenticationManager`
3. On success, JWT token is generated with user claims
4. Token includes: UUID, username, email, roles, client_id

### 3. Service Layer

#### DccAuthService
Main business logic component handling:

**Token Management**
```java
public String getToken(String username, String password, String clientId)
```
- Authenticates user credentials
- Fetches complete user entity
- Generates JWT token with comprehensive claims

**OTP Operations**
```java
public String sendOtp(SendOtpRequestDto request)
public Map<String, Object> verifyOtpAndLogin(String email, String otp, String clientId)
```
- Generates 6-digit OTP
- Sends email via Kafka event
- Verifies OTP and issues token

**User Registration**
```java
public Map<String, Object> signup(SignUpRequestDto request, String clientId)
```
- Completes user registration after OTP verification
- Encrypts and stores password
- Issues authentication token

### 4. Controller Layer

#### UserController (Implied)
REST API endpoints for:
- User registration (`POST /signup`)
- User login (`POST /login`)
- OTP operations (`POST /send-otp`, `POST /verify-otp`)
- Profile management (`GET /profile`)

### 5. Integration Layer

#### Email System
- **EmailEventProducer**: Kafka producer for email events
- **SendEmailEventDto**: Email event data structure
- Asynchronous email processing via message broker

#### OAuth2 Integration
- **RegisteredClientRepository**: OAuth2 client management
- **OAuth2AuthorizationService**: Authorization persistence
- Support for multiple client applications

## JWT Token Structure

Generated tokens include the following claims:

```json
{
  "iss": "http://localhost:8082",
  "iat": 1725638400,
  "exp": 1725642000,
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "username": "user@example.com",
  "email": "user@example.com",
  "name": "John Doe",
  "roles": ["ROLE_USER"],
  "client_id": "activity-tracker-client"
}
```

## API Endpoints

### Authentication Endpoints
```
POST /api/auth/login
POST /api/auth/signup
POST /api/auth/send-otp
POST /api/auth/verify-otp
```

### Profile Endpoints
```
GET /api/users/profile
PUT /api/users/profile
```

## Configuration

### Application Properties
```properties
# Server Configuration
server.port=8082

# Database Configuration
spring.datasource.url=jdbc:h2:mem:userdb
spring.jpa.hibernate.ddl-auto=update

# Kafka Configuration
spring.kafka.bootstrap-servers=localhost:9092
```

## Security Features

### Password Security
- BCrypt hashing with strength 17
- Secure password storage
- No plain text password handling

### Token Security
- JWT with RSA signing
- 1-hour token expiration
- Comprehensive user claims
- Client-specific token validation

### OTP Security
- 6-digit numeric OTP
- Time-based expiration
- Single-use verification
- Email-only delivery

## Error Handling

### Custom Exceptions
- **OtpException**: OTP-related errors
- **UserNotFoundException**: User lookup failures
- **InvalidRequestException**: Request validation errors
- **TokenNotFoundException**: Token validation failures

### Global Exception Handler
- **ExceptionHandler**: Centralized error processing
- Consistent error response format
- Proper HTTP status codes

## Monitoring and Logging

### Logging Strategy
- SLF4J with Logback
- Structured logging for authentication events
- Error logging with stack traces
- Request/response logging for debugging

## Future Enhancements

### Planned Features
1. **Role-Based Access Control (RBAC)**
   - Multiple user roles
   - Permission-based authorization
   - Admin user management

2. **Multi-Factor Authentication**
   - SMS OTP support
   - TOTP integration
   - Backup codes

3. **Social Authentication**
   - Google OAuth integration
   - GitHub authentication
   - Facebook login

4. **User Profile Extensions**
   - Profile pictures
   - User preferences
   - Activity categories

## Deployment Considerations

### Environment Configuration
- Separate configurations for dev/test/prod
- Environment-specific database connections
- Kafka cluster configuration

### Security Hardening
- HTTPS enforcement
- Rate limiting implementation
- Input validation enhancement
- Security headers configuration

## Integration Points

### With Other Services
- **Activity Service**: User authentication via JWT
- **Email Service**: OTP delivery via Kafka
- **API Gateway**: Token validation and routing
- **Frontend Applications**: Authentication flows

### External Dependencies
- **Database**: User data persistence
- **Kafka**: Asynchronous messaging
- **SMTP Server**: Email delivery (via Email Service)

## Performance Characteristics

### Scalability
- Stateless design for horizontal scaling
- Database connection pooling
- Async email processing
- JWT for distributed authentication

### Caching Strategy
- User session caching (future)
- Token validation caching
- Database query optimization

---

*This documentation reflects the current state of the User Service as of September 2025. For the latest updates and API specifications, refer to the source code and test files.*
