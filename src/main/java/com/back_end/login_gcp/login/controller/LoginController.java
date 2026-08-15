package com.back_end.login_gcp.login.controller;

import com.back_end.login_gcp.login.dto.LoginUserRequest;
import com.back_end.login_gcp.login.dto.LoginUserResponse;
import com.back_end.login_gcp.login.service.UserLoginService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
public class LoginController {

    private final UserLoginService userLoginService;

    public LoginController(UserLoginService userLoginService) {
        this.userLoginService = userLoginService;
    }

    @PostMapping("/login")
    @ResponseStatus(HttpStatus.OK)
    public LoginUserResponse login(@Valid @RequestBody LoginUserRequest request) {
        return userLoginService.login(request);
    }
}
