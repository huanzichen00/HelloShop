package com.huanzichen.springboothello.service;

import com.huanzichen.springboothello.common.ErrorCode;
import com.huanzichen.springboothello.common.PageResult;
import com.huanzichen.springboothello.common.UserContext;
import com.huanzichen.springboothello.dto.user.UserCreateDTO;
import com.huanzichen.springboothello.dto.user.UserProfileUpdateDTO;
import com.huanzichen.springboothello.dto.user.UserQueryDTO;
import com.huanzichen.springboothello.dto.user.UserUpdateDTO;
import com.huanzichen.springboothello.exception.BusinessException;
import com.huanzichen.springboothello.exception.CheckedBusinessException;
import com.huanzichen.springboothello.mapper.UserMapper;
import com.huanzichen.springboothello.model.CurrentUserVO;
import com.huanzichen.springboothello.model.UserInfo;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserService {

    private final UserMapper userMapper;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public UserService(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    public UserInfo createUser(UserCreateDTO userCreateDTO) {
        UserInfo existedUser = userMapper.findByUsername(userCreateDTO.getUsername());
        if (existedUser != null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "username already exists");
        }

        UserInfo userInfo = new UserInfo();
        userInfo.setUsername(userCreateDTO.getUsername());
        userInfo.setPassword(passwordEncoder.encode(userCreateDTO.getPassword()));
        userInfo.setName(userCreateDTO.getName());
        userInfo.setAge(userCreateDTO.getAge());

        userMapper.insertWithAuth(userInfo);
        return userInfo;
    }

    public List<UserInfo> listUsers() {
        return userMapper.findAll();
    }

    public UserInfo getUserById(Long id) {
        UserInfo userInfo = userMapper.findById(id);
        if (userInfo == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "user not found");
        }

        return userInfo;
    }

    public void deleteUserById(Long id) {
        int rows = userMapper.deleteById(id);
        if (rows == 0) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "user not found");
        }
    }

    public UserInfo updateUser(Long id, UserUpdateDTO userUpdateDTO) {

        UserInfo userInfo = new UserInfo();

        userInfo.setName(userUpdateDTO.getName());
        userInfo.setAge(userUpdateDTO.getAge());
        userInfo.setId(id);

        int rows = userMapper.update(userInfo);
        if (rows == 0) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "user not found");
        }

        return userInfo;
    }

    public List<UserInfo> searchUsers(String name) {

        validateName(name);

        return userMapper.searchByName(name);
    }

    public PageResult<UserInfo> listUsersByPage(Integer page, Integer size, String sort, String order) {
        validatePageParams(page, size);
        sort = normalizeSort(sort);
        order = normalizeOrder(order);
        int offset = (page - 1) * size;
        List<UserInfo> list = userMapper.findPageWithSort(offset, size, sort, order);
        long total = userMapper.countAll();
        return buildPageResult(list, total, page, size);
    }

    @Transactional
    public void createTwoUsersForTest() {
        UserInfo user1 = new UserInfo();
        user1.setName("transaction-user-1");
        user1.setAge(18);
        userMapper.insert(user1);

        throw new BusinessException(ErrorCode.BAD_REQUEST, "test transaction rollback");
    }

    public void callTransactionMethodInsideTheSameClass() {
        createTwoUsersForTest();
    }

    @Transactional(rollbackFor = Exception.class)
    public void createUserAndThrowCheckedBusinessException() throws CheckedBusinessException {
        UserInfo userInfo = new UserInfo();
        userInfo.setName("checked-exception-user");
        userInfo.setAge(20);
        userMapper.insert(userInfo);

        throw new CheckedBusinessException("checked exception test");
    }

    public PageResult<UserInfo> searchUsersByPage(UserQueryDTO userQueryDTO) {
        String name = userQueryDTO.getName();
        Integer minAge = userQueryDTO.getMinAge();
        Integer maxAge = userQueryDTO.getMaxAge();
        Integer page = userQueryDTO.getPage();
        Integer size = userQueryDTO.getSize();
        String sort = userQueryDTO.getSort();
        String order = userQueryDTO.getOrder();

        if (minAge != null && maxAge != null && minAge > maxAge) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "minAge cannot be greater than maxAge");
        }
        validatePageParams(page, size);
        sort = normalizeSort(sort);
        order = normalizeOrder(order);
        int offset = (page - 1) * size;
        long total = userMapper.countWithConditions(name, minAge, maxAge);
        List<UserInfo> list = userMapper.searchWithConditionsAndPage(name, minAge, maxAge, offset, size, sort, order);
        return buildPageResult(list, total, page, size);
    }

    public CurrentUserVO updateCurrentUser(UserProfileUpdateDTO userProfileUpdateDTO) {
        Long userId = UserContext.getCurrentUserId();
        UserInfo userInfo = getUserById(userId);
        userInfo.setName(userProfileUpdateDTO.getName());
        userInfo.setAge(userProfileUpdateDTO.getAge());
        userMapper.update(userInfo);
        CurrentUserVO currentUserVO = new CurrentUserVO();
        currentUserVO.setId(userId);
        currentUserVO.setName(userInfo.getName());
        currentUserVO.setAge(userInfo.getAge());
        currentUserVO.setUsername(userInfo.getUsername());
        return currentUserVO;
    }

    private static void validateName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "name cannot be blank");
        }
    }

    private static void validatePageParams(Integer page, Integer size) {
        if (page == null || page <= 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "page must be greater than zero");
        }
        if (size == null || size <= 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "size must be greater than zero");
        }
    }

    private String normalizeSort(String sort) {
        if (sort == null || sort.trim().isEmpty()) {
            return "id";
        }
        if (!"id".equals(sort) && !"age".equals(sort)) {
            return "id";
        }
        return sort;
    }

    private String normalizeOrder(String order) {
        if (order == null || order.trim().isEmpty()) {
            return "asc";
        }
        order = order.toLowerCase();
        if (!"asc".equals(order) && !"desc".equals(order)) {
            return "asc";
        }
        return order;
    }

    private PageResult<UserInfo> buildPageResult(List<UserInfo> list, Long total, Integer page, Integer size) {
        int totalPages = (int) ((total + size - 1) / size);
        return new PageResult<>(total, list, page, size, totalPages);
    }

    public void validateUserExists(Long id) {
        getUserById(id);
    }
}
