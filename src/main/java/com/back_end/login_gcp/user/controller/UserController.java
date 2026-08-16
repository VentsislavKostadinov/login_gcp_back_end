package com.back_end.login_gcp.user.controller;

import com.back_end.login_gcp.user.service.UserDeletionService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserDeletionService userDeletionService;

    public UserController(UserDeletionService userDeletionService) {
        this.userDeletionService = userDeletionService;
    }

    @DeleteMapping("/me")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCurrentUser(@RequestHeader("Authorization") String authorizationHeader) {
        userDeletionService.deleteCurrentUser(authorizationHeader);
    }
}
