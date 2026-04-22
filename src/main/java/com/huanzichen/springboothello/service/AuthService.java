package com.huanzichen.springboothello.service;

import com.huanzichen.springboothello.common.ErrorCode;
import com.huanzichen.springboothello.common.JwtUtil;
import com.huanzichen.springboothello.common.UserContext;
import com.huanzichen.springboothello.dto.auth.LoginDTO;
import com.huanzichen.springboothello.dto.auth.RegisterDTO;
import com.huanzichen.springboothello.exception.BusinessException;
import com.huanzichen.springboothello.mapper.UserMapper;
import com.huanzichen.springboothello.model.CurrentUserVO;
import com.huanzichen.springboothello.model.UserInfo;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Service
public class AuthService {

    private final UserService userService;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final Map<String, Long> tokenStore = new ConcurrentHashMap<>();
    private final UserMapper userMapper;
    private final StringRedisTemplate stringRedisTemplate;

    private static final String TOKEN_BLACKLIST_PREFIX = "token:blacklist:";

    public AuthService(UserService userService, UserMapper userMapper, StringRedisTemplate stringRedisTemplate) {
        this.userService = userService;
        this.userMapper = userMapper;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    public String login(LoginDTO loginDTO) {
        UserInfo userInfo = userMapper.findByUsername(loginDTO.getUsername());
        if (userInfo == null || !passwordEncoder.matches(loginDTO.getPassword(), userInfo.getPassword())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "username or password is incorrect");
        }
        return JwtUtil.generateToken(userInfo.getId());
    }

    public Long getUserIdByToken(String token) {
        return tokenStore.get(token);
    }

    public boolean isValidToken(String token) {
        return tokenStore.containsKey(token);
    }

    public UserInfo register(RegisterDTO registerDTO) {
        UserInfo existedUser = userMapper.findByUsername(registerDTO.getUsername());
        if (existedUser != null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "username already exists");
        }
        UserInfo userInfo = new UserInfo();
        userInfo.setUsername(registerDTO.getUsername());
        userInfo.setPassword(passwordEncoder.encode(registerDTO.getPassword()));
        userInfo.setAge(registerDTO.getAge());
        userInfo.setName(registerDTO.getName());
        userMapper.insertWithAuth(userInfo);
        return userInfo;
    }

    public CurrentUserVO getCurrentUser() {
        Long userId = UserContext.getCurrentUserId();
        UserInfo userInfo = userService.getUserById(userId);

        CurrentUserVO currentUserVO = new CurrentUserVO();
        currentUserVO.setId(userInfo.getId());
        currentUserVO.setUsername(userInfo.getUsername());
        currentUserVO.setName(userInfo.getName());
        currentUserVO.setAge(userInfo.getAge());

        return currentUserVO;
    }

    public void logout(String token) {
        stringRedisTemplate.opsForValue().set(
                TOKEN_BLACKLIST_PREFIX + token,
                "1",
                24,
                TimeUnit.HOURS
        );
    }

    public boolean isBlacklisted(String token) {
        Boolean exists = stringRedisTemplate.hasKey(TOKEN_BLACKLIST_PREFIX + token);
        return Boolean.TRUE.equals(exists);
    }
}
