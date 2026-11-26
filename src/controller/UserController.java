package controller;

import common.BusinessException;
import common.LogAspect;
import common.Result;
import entity.User;
import service.UserService;

import java.util.List;


/**
 * 用户控制器：写操作统一交给 LogAspect.invoke()，读操作直接调 Service
 * 约束：不写业务规则、不碰 DAO、不打印任何内容
 */
public class UserController {
    private final UserService userService = UserService.getInstance();

    public Result<User> login(String username, String password){
        if (isEmpty(username) || isEmpty(password)) {
            return Result.fail("账号和密码不能为空");
        }
        return (Result<User>) LogAspect.invoke(userService, "login",
                new Class<?>[]{String.class, String.class},
                new Object[]{username.trim(), password.trim()}, username.trim());

    }

    public Result<User> register(String username, String password) {
        if (isEmpty(username)) {
            return Result.fail("用户名不能为空");
        }
        if (password == null || password.length() < 6) {
            return Result.fail("密码长度不能少于 6 位");
        }
        return (Result<User>) LogAspect.invoke(userService, "register",
                new Class<?>[]{String.class, String.class},
                new Object[]{username.trim(), password}, username.trim());

    }

    public Result<List<User>> queryAll(User operator){
        return userService.queryAll(operator);
    }

    private boolean isEmpty(String s){
        return s == null || s.trim().isEmpty();
    }
}
