package com.huanzichen.springboothello.controller;

import com.huanzichen.springboothello.common.Result;
import com.huanzichen.springboothello.exception.CheckedBusinessException;
import com.huanzichen.springboothello.service.UserService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/demo")
public class DemoController {

    private final UserService userService;

    public DemoController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/transaction-test")
    public Result<String> transactionTest() {
        userService.createTwoUsersForTest();
        return Result.success("success");
    }

    @PostMapping("/transaction-self-call-test")
    public Result<String> transactionSelfCallTest() {
        userService.callTransactionMethodInsideTheSameClass();
        return Result.success("success");
    }

    @PostMapping("/transaction-checked-exception-test")
    public Result<String> transactionCheckedExceptionTest() throws CheckedBusinessException {
        userService.createUserAndThrowCheckedBusinessException();
        return Result.success("success");
    }
}
