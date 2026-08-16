package com.back_end.login_gcp.register.confirmation.dto;

public record ConfirmRegistrationResponse(String email, long expiresInMinutes) {
}
