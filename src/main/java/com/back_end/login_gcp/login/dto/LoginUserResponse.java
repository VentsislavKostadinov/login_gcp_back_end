package com.back_end.login_gcp.login.dto;

public record LoginUserResponse(String uid, String email, String idToken, String refreshToken, long expiresIn) {
}
