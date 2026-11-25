package controller;

import common.BusinessException;
import common.Result;
import entity.User;
import service.UserService;

import java.util.List;


/**
 * 用户控制器：接收 View 传来的参数，做非空校验，调用 Service，把 Result 原样返回
 * 约束：不写业务规则、不碰 DAO、不打印任何内容
 */
public class UserController {
    private final UserService userService = UserService.getInstance();

    public Result<User> login(String username, String password){
        if (isEmpty(username) || isEmpty(password)) {
            return Result.fail("账号和密码不能为空");
        }
        try {
            return userService.login(username.trim(), password.trim());
        } catch (BusinessException e){
            return Result.fail(e.getMessage());          // 业务异常转 Result
        } catch (Exception e) {
            return Result.fail("登录失败，请稍后再试");     // 兜底，不外泄英文堆栈
        }
    }

    public Result<User> register(String username, String password) {
        if (isEmpty(username)) {
            return Result.fail("用户名不能为空");
        }
        if (password == null || password.length() < 6) {
            return Result.fail("密码长度不能少于 6 位");
        }
        try {
            return userService.register(username.trim(), password);
        } catch (BusinessException e) {
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            return Result.fail("注册失败，请稍后再试");
        }
    }

    public Result<List<User>> queryAll(User operator){
        return userService.queryAll(operator);
    }

    private boolean isEmpty(String s){
        return s == null || s.trim().isEmpty();
    }
}
