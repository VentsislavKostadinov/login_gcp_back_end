package com.back_end.login_gcp.register.service;

import com.back_end.login_gcp.register.confirmation.dto.ConfirmRegistrationResponse;
import com.back_end.login_gcp.register.confirmation.service.RegisterConfirmationService;
import com.back_end.login_gcp.register.dto.RegisterUserRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class UserRegistrationService {

    private final RegisterConfirmationService registerConfirmationService;

    public UserRegistrationService(RegisterConfirmationService registerConfirmationService) {
        this.registerConfirmationService = registerConfirmationService;
    }

    public ConfirmRegistrationResponse register(RegisterUserRequest request) {
        if (!request.password().equals(request.repeatPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Passwords do not match");
        }

        return registerConfirmationService.sendConfirmationCode(request.email(), request.password());
    }
}