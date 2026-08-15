package com.back_end.login_gcp.login.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginUserRequest(
        @NotBlank @Email String email,
        @NotBlank String password) {
}
