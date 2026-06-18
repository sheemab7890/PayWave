package com.sheemab.shardedsagawallet.controller;

import com.sheemab.shardedsagawallet.dtos.UserResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.sheemab.shardedsagawallet.dtos.LoginRequestDto;
import com.sheemab.shardedsagawallet.dtos.LoginResponseDto;
import com.sheemab.shardedsagawallet.dtos.UserRequestDto;
import com.sheemab.shardedsagawallet.services.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;


    @PostMapping("/createUser")
    public ResponseEntity<UserResponseDto> createUser(
            @RequestBody UserRequestDto request
    ) {
        UserResponseDto userDto = authService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(userDto);
    }

    /**
     * POST /auth/login
     * Body: { "email": "s@s.com", "password": "secret123" }
     * Returns: { "accessToken": "eyJhbGc..." }
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponseDto> login(
            @RequestBody LoginRequestDto request
    ) {
        LoginResponseDto response = authService.login(request);
        return ResponseEntity.ok(response);
    }
}

