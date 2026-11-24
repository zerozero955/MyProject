package ui.console;

import common.Result;
import entity.User;
import service.ReserveRecordService;
import service.ResourceService;
import service.UserService;

import java.util.Scanner;

/**
 * 登录注册视图：只做输入输出与结果展示，不含业务规则
 */

public class LoginMenu {
    private final Scanner sc = new Scanner(System.in);
    private final UserService userService;
    private final ResourceService resourceService;
    private final ReserveRecordService reserveService;

    public LoginMenu(UserService userService, ResourceService resourceService,
                     ReserveRecordService reserveService) {
        this.userService = userService;
        this.resourceService = resourceService;
        this.reserveService = reserveService;
    }

    public void start() {
        while (true) {
            System.out.println("========== 校园资源预约系统 V3.3 ==========");
            System.out.println("1. 登录");
            System.out.println("2. 注册");
            System.out.println("0. 退出");
            System.out.print("请输入选项：");
            String choice = sc.nextLine().trim();
            switch (choice) {
                case "1":
                    doLogin();
                    break;
                case "2":
                    doRegister();
                    break;
                case "0":
                    System.out.println("已退出，再见！");
                    return;
                default:
                    System.out.println("输入有误，请重新选择！");
            }
        }
    }

    private void doLogin() {
        System.out.print("账号：");
        String username = sc.nextLine().trim();
        System.out.print("密码：");
        String password = sc.nextLine().trim();
        Result<User> result = userService.login(username, password);
        printResult(result);
        if (!result.isSuccess()) {
            return;
        }
        User user = result.getData();
        if ("admin".equals(user.getRole())) {
            new AdminMenu(sc, user, userService, resourceService, reserveService).start();
        } else {
            new StudentMenu(sc, user, resourceService, reserveService).start();
        }
    }

    private void doRegister() {
        System.out.print("用户名：");
        String username = sc.nextLine().trim();
        System.out.print("密码（至少6位）：");
        String password = sc.nextLine().trim();
        printResult(userService.register(username, password));
    }

    /** 视图层统一的结果打印：成功带数据，失败只给提示 */
    private void printResult(Result<?> result) {
        if (result == null) {
            System.out.println("[失败] 操作无响应，请重试");
            return;
        }
        if (!result.isSuccess()) {
            System.out.println("[失败] " + result.getMsg());
            return;
        }
        System.out.println("[成功] " + result.getMsg());
        Object data = result.getData();
        if (data instanceof java.util.List) {
            for (Object o : (java.util.List<?>) data) {
                System.out.println(o);
            }
        } else if (data != null) {
            System.out.println(data);
        }
    }

}
