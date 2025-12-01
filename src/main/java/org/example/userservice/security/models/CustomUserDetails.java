package org.example.userservice.security.models;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Getter;
import lombok.Setter;
import org.example.userservice.model.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@JsonDeserialize
public class CustomUserDetails implements UserDetails {

    private List<? extends GrantedAuthority> authorities;
    private String hashedPassword;;
    private String username;
    @Setter
    @Getter
    private String name;

    private boolean accountNonExpired;
    private boolean accountNonLocked;
    private boolean credentialsNonExpired;
    private boolean enabled;

    public CustomUserDetails() {}

    public CustomUserDetails(User user) {
        this.username = user.getUsername(); // or user.getEmail()
        this.hashedPassword = user.getHashedPassword();
        this.name = user.getName();
        this.authorities = new ArrayList<>(user.getAuthorities());

        this.accountNonExpired = user.isAccountNonExpired();
        this.accountNonLocked = user.isAccountNonLocked();
        this.credentialsNonExpired = user.isCredentialsNonExpired();
        this.enabled = user.isEnabled();
    }

    @Override public Collection<? extends GrantedAuthority> getAuthorities() { return authorities; }
    @Override public String getPassword() { return hashedPassword; }
    @Override public String getUsername() { return username; }
    @Override public boolean isAccountNonExpired() { return accountNonExpired; }
    @Override public boolean isAccountNonLocked() { return accountNonLocked; }
    @Override public boolean isCredentialsNonExpired() { return credentialsNonExpired; }
    @Override public boolean isEnabled() { return enabled; }
}
