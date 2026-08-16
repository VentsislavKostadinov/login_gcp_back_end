package com.back_end.login_gcp.register.confirmation.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ConfirmRegistrationRequest(@NotBlank @Email String email, @NotBlank String code) {
}
