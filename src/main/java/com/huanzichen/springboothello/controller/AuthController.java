package com.huanzichen.springboothello.controller;

import com.huanzichen.springboothello.common.ErrorCode;
import com.huanzichen.springboothello.common.Result;
import com.huanzichen.springboothello.dto.auth.LoginDTO;
import com.huanzichen.springboothello.dto.auth.RegisterDTO;
import com.huanzichen.springboothello.exception.BusinessException;
import com.huanzichen.springboothello.model.CurrentUserVO;
import com.huanzichen.springboothello.model.UserInfo;
import com.huanzichen.springboothello.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public Result<String> login(@RequestBody @Valid LoginDTO loginDTO) {
        return Result.success(authService.login(loginDTO));
    }

    @PostMapping("/register")
    public Result<UserInfo> register(@RequestBody @Valid RegisterDTO registerDTO) {
        return Result.success(authService.register(registerDTO));
    }

    @GetMapping("/me")
    public Result<CurrentUserVO> me() {
        return Result.success(authService.getCurrentUser());
    }

    @PostMapping("/logout")
    public Result<String> logout(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "please login first");
        }
        String token = authHeader.substring(7);
        authService.logout(token);
        return Result.success("logout successfully");
    }
}
