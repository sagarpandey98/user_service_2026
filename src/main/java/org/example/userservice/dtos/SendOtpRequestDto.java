package org.example.userservice.dtos;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class SendOtpRequestDto {
    private String identifier;
}
