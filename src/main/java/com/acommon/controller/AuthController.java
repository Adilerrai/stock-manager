package com.acommon.controller;



import com.acommon.persistant.dto.JwtAuthenticationResponse;
import com.acommon.persistant.dto.UserLoginRequest;
import com.acommon.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }



    @PostMapping("/login")
    public ResponseEntity<JwtAuthenticationResponse> authenticateUserByUsername(@RequestBody UserLoginRequest request) {
        JwtAuthenticationResponse response = authService.authenticateByUsername(request);
        return ResponseEntity.ok(response);
    }
}
