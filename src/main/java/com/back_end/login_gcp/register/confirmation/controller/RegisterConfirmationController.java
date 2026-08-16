package com.back_end.login_gcp.register.confirmation.controller;

import com.back_end.login_gcp.register.confirmation.dto.ConfirmRegistrationRequest;
import com.back_end.login_gcp.register.confirmation.service.RegisterConfirmationService;
import com.back_end.login_gcp.register.dto.RegisterUserResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users/register")
public class RegisterConfirmationController {

    private final RegisterConfirmationService registerConfirmationService;

    public RegisterConfirmationController(RegisterConfirmationService registerConfirmationService) {
        this.registerConfirmationService = registerConfirmationService;
    }

    @PostMapping("/confirm")
    @ResponseStatus(HttpStatus.CREATED)
    public RegisterUserResponse confirm(@Valid @RequestBody ConfirmRegistrationRequest request) {
        return registerConfirmationService.confirmRegistration(request);
    }
}
