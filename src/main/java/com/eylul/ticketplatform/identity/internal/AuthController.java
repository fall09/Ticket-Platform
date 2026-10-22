package com.eylul.ticketplatform.identity.internal;


import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;


@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

    private final RegistrationService registrationService;
    private final LoginService loginService;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public RegisterResponse register(@Valid @RequestBody RegisterRequest registerRequest) {
        AppUser newUser = registrationService.register(registerRequest.email(), registerRequest.password());
        return new RegisterResponse(newUser.getId(), newUser.getEmail(), newUser.getRole());
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest loginRequest) {
        return loginService.login(loginRequest.email(), loginRequest.password());
    }

}
