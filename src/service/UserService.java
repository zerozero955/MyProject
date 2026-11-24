package service;

import common.Result;
import dao.DaoFactory;
import dao.UserDao;
import entity.User;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 用户业务层：承接校验与Result组装；依赖UserDao，不依赖UI*/
public class UserService {
    /** 饿汉单例：保证全局只有一个用户服务实例 */
    private static final UserService INSTANCE = new UserService();

    public static UserService getInstance(){
        return INSTANCE;
    }
    /** DAO 由工厂提供，不再自己 new */
    private final UserDao userDao;
    private final List<User> users;

    private UserService() {
        this.userDao = DaoFactory.getUserDao();
        this.users = userDao.loadAll();
        if (users.isEmpty()){                // 首次运行写入初始账号
            users.add(new User("admin", "123456", "admin", LocalDateTime.now()));
            users.add(new User("user", "123456", "user", LocalDateTime.now()));
            userDao.saveAll(users);
        }
    }

    public Result<User> register(String username, String password){
        if (username == null || username.trim().isEmpty()
                || password == null || password.isEmpty()) {
            return Result.fail("用户名和密码不能为空");
        }
        username = username.trim();
        if (username.length() < 3 || username.length() > 16) {
            return Result.fail("用户名长度须在 3~16 个字符之间");
        }
        if (password.length() < 6) {
            return Result.fail("密码长度不能少于 6 位");
        }
        if (findByUsername(username).isPresent()) {
            return Result.fail("该用户名已被注册，请更换");
        }
        User user = new User(username, password, "student", LocalDateTime.now());
        users.add(user);
        userDao.saveAll(users);
        return Result.success("注册成功", user);
    }

    public Result<User> login(String username, String password){
        if (username == null || username.trim().isEmpty()
                || password == null || password.isEmpty()) {
            return Result.fail("账号和密码不能为空");
        }
        Optional<User> opt = findByUsername(username.trim());
        if (!opt.isPresent()) {
            return Result.fail("账号不存在，请先注册");
        }
        User user = opt.get();
        if (!user.getPassword().equals(password)) {
            return Result.fail("密码错误，请重新输入");
        }
        String roleName = "admin".equals(user.getRole()) ? "管理员" : "同学";
        return Result.success("登录成功，欢迎您，" + roleName + "！", user);

    }

    public Result<List<User>> queryAll(User operator){
        if (operator == null || !"admin".equals(operator.getRole())) {
            return Result.fail("该操作仅管理员可执行");
        }
        return Result.success("共 " + users.size() + " 个用户", users);
    }

    public Optional<User> findByUsername(String username){
        return userDao.findByUsername(username, users);
    }
}
