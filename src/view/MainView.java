package view;

import common.Result;
import controller.UserController;
import entity.User;

import java.util.List;
import java.util.Scanner;

/**
 * 主视图：登录 / 注册菜单
 * 约束：只读输入、只打结果、只调 Controller；不写业务判断、不碰 Service 与 DAO
 */
public class MainView {
    private final Scanner sc = new Scanner(System.in);
    private final UserController userController = new UserController();

    public void start(){
        while (true) {
            printMenu();
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

    private void printMenu() {
        System.out.println("========== 校园资源预约系统 V4.0 ==========");
        System.out.println("1. 登录");
        System.out.println("2. 注册");
        System.out.println("0. 退出");
        System.out.print("请输入选项：");
    }

    private void doLogin() {
        System.out.print("账号：");
        String username = sc.nextLine().trim();
        System.out.print("密码：");
        String password = sc.nextLine().trim();
        Result<User> result = userController.login(username, password);
        printResult(result);
        if (!result.isSuccess()) {
            return;
        }
        User user = result.getData();
        if ("admin".equals(user.getRole())) {
            new AdminView(user).start();
        } else {
            new StudentView(user).start();
        }
    }

    private void doRegister() {
        System.out.print("用户名：");
        String username = sc.nextLine().trim();
        System.out.print("密码（至少6位）：");
        String password = sc.nextLine().trim();
        printResult(userController.register(username, password));
    }

    /** 视图层统一的结果展示：成功带数据，失败只给一行中文提示 */
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
        if (data instanceof List) {
            List<?> list = (List<?>) data;
            if (list.isEmpty()) {
                System.out.println("（暂无数据）");
            } else {
                for (Object o : list) {
                    System.out.println(o);
                }
            }
        } else if (data != null) {
            System.out.println(data);
        }
    }

}
