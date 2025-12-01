package org.example.userservice.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GeneralResponseDto {
    private boolean success;
    private String message;
    private Object data;

    public static GeneralResponseDto success(String message, Object data) {
        return new GeneralResponseDto(true, message, data);
    }

    public static GeneralResponseDto success(String message) {
        return new GeneralResponseDto(true, message, null);
    }

    public static GeneralResponseDto error(String message) {
        return new GeneralResponseDto(false, message, null);
    }
}
