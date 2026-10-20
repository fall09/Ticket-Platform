package com.eylul.ticketplatform.identity.internal;


import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;


@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

    private final RegistrationService registrationService;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public RegisterResponse register(@RequestBody RegisterRequest registerRequest) {
        AppUser newUser = registrationService.register(registerRequest.email(), registerRequest.password());
        return new RegisterResponse(newUser.getId(), newUser.getEmail(), newUser.getRole());
    }




}
