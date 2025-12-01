package org.example.userservice.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "users")
public class User extends BaseModel{

    private String email;
    private String phone;
    private String username;

    private String hashedPassword; // used for Spring Security authentication

    private String otp;
    private LocalDateTime otpGeneratedTime;
    private boolean isOtpVerified;

    private boolean enabled = true;
    private boolean accountNonExpired = true;
    private boolean accountNonLocked = true;
    private boolean credentialsNonExpired = true;
    private boolean isSignedUp = false;

    private String name; // used for displaying name in JWT claims

    private String roles = "ROLE_USER"; // single role as string; for multi-role use ManyToMany


    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singleton(() -> roles); // simple authority
    }

    public boolean isAccountNonExpired() { return accountNonExpired; }
    public boolean isAccountNonLocked() { return accountNonLocked; }
    public boolean isCredentialsNonExpired() { return credentialsNonExpired; }
    public boolean isEnabled() { return enabled; }
}
