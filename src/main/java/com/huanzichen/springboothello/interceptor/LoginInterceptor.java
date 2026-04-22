package com.huanzichen.springboothello.interceptor;

import com.huanzichen.springboothello.common.ErrorCode;
import com.huanzichen.springboothello.common.JwtUtil;
import com.huanzichen.springboothello.common.UserContext;
import com.huanzichen.springboothello.exception.BusinessException;
import com.huanzichen.springboothello.service.AuthService;
import io.jsonwebtoken.Jwt;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class LoginInterceptor implements HandlerInterceptor {

    private final AuthService authService;

    public LoginInterceptor(AuthService authService) {
        this.authService = authService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || authHeader.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "please login first");
        }

        if (!authHeader.startsWith("Bearer ")) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "invalid token");
        }

        String token = authHeader.substring(7);

        if (authService.isBlacklisted(token)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "invalid token");
        }

        try {
            Long userId = JwtUtil.parseUserId(token);
            UserContext.setCurrentUserId(userId);
            return true;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "invalid token");
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContext.clear();
    }
}
