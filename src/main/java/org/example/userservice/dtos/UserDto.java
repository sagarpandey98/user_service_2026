package org.example.userservice.dtos;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.example.userservice.model.Role;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {
    private UUID id;
    private String username;
    private String email;
    private String name;
    private List<Role> roles;
    private boolean isEmailVerified;

    // Constructor for basic profile data
    public UserDto(UUID id, String username, String email, String name) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.name = name;
        this.isEmailVerified = true; // Since we're getting from authenticated token
    }
}
