package com.back_end.login_gcp.login.service;

import com.back_end.login_gcp.login.dto.LoginUserRequest;
import com.back_end.login_gcp.login.dto.LoginUserResponse;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

@Service
public class UserLoginService {

    private static final Logger log = LoggerFactory.getLogger(UserLoginService.class);
    private static final String SIGN_IN_PATH = "/accounts:signInWithPassword";

    private final RestClient restClient;
    private final String apiKey;

    public UserLoginService(RestClient.Builder restClientBuilder, @Value("${firebase.api-key}") String apiKey) {
        this.restClient = restClientBuilder.baseUrl("https://identitytoolkit.googleapis.com/v1").build();
        this.apiKey = apiKey;
    }

    public LoginUserResponse login(LoginUserRequest request) {
        SignInRequest body = new SignInRequest(request.email(), request.password(), true);

        SignInResponse response;
        try {
            response = restClient.post()
                    .uri(uriBuilder -> uriBuilder.path(SIGN_IN_PATH).queryParam("key", apiKey).build())
                    .body(body)
                    .retrieve()
                    .body(SignInResponse.class);
        } catch (HttpClientErrorException exception) {
            // Identity Toolkit returns INVALID_LOGIN_CREDENTIALS / USER_DISABLED etc. in the body.
            log.warn("Firebase sign-in failed: status={}, body={}", exception.getStatusCode(),
                    exception.getResponseBodyAsString());
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }

        if (response == null) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not sign in");
        }

        return new LoginUserResponse(
                response.localId(),
                response.email(),
                response.idToken(),
                response.refreshToken(),
                Long.parseLong(response.expiresIn()));
    }

    private record SignInRequest(String email, String password, @JsonProperty("returnSecureToken") boolean returnSecureToken) {
    }

    private record SignInResponse(String localId, String email, String idToken, String refreshToken, String expiresIn) {
    }
}
