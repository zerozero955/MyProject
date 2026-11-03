import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class SystemMain {

    private static final Map<String, String> USERS = new HashMap<>();

    public static void main(String[] args) {
        // 保留一组初始账号，避免全部依赖注册产生
        USERS.put("admin", "123456");
        USERS.put("student", "123456");

        Scanner sc = new Scanner(System.in);
        while (true) {
            System.out.println("========== 校园资源预约系统 V1.1 ==========");
            System.out.println("1. 登录");
            System.out.println("2. 注册");
            System.out.println("0. 退出");
            System.out.print("请输入选项：");
            String choice = sc.nextLine().trim();
            switch (choice) {
                case "1": login(sc);    break;
                case "2": register(sc); break;
                case "0":
                    System.out.println("已退出，再见！");
                    return;
                default:
                    System.out.println("输入有误，请重新选择！");
            }
        }
    }

    /** 注册主流程 */
    private static void register(Scanner sc) {
        System.out.println("---------- 用户注册 ----------");
        System.out.print("用户名：");
        String username = sc.nextLine().trim();
        System.out.print("密码（至少6位）：");
        String password = sc.nextLine().trim();

        if (!checkUsername(username)) {
            return;
        }
        if (!checkPassword(password)) {
            return;
        }
        USERS.put(username, password);
        System.out.println("注册成功，请返回首页登录！");
    }

    /** 账号校验*/
    private static boolean checkUsername(String username) {
        if (username.isEmpty()) {
            System.out.println("用户名不能为空！");
            return false;
        }
        if (username.length() < 3 || username.length() > 16) {
            System.out.println("用户名长度须在 3~16 个字符之间！");
            return false;
        }
        if (USERS.containsKey(username)) {
            System.out.println("该用户名已被注册，请更换！");
            return false;
        }
        return true;
    }

    /** 密码校验*/
    private static boolean checkPassword(String password) {
        if (password.isEmpty()) {
            System.out.println("密码不能为空！");
            return false;
        }
        if (password.length() < 6) {
            System.out.println("密码长度不能少于 6 位！");
            return false;
        }
        return true;
    }

    /** 登录 */
    private static void login(Scanner sc) {
        System.out.print("账号：");
        String username = sc.nextLine().trim();
        System.out.print("密码：");
        String password = sc.nextLine().trim();

        if (username.isEmpty() || password.isEmpty()) {
            System.out.println("账号和密码不能为空！");
            return;
        }
        String savedPwd = USERS.get(username);
        if (savedPwd == null || !savedPwd.equals(password)) {
            System.out.println("登录失败：账号或密码错误！");
            return;
        }
        if ("admin".equals(username)) {
            System.out.println("登录成功，欢迎您，管理员！");
        } else {
            System.out.println("登录成功，欢迎您，同学！");
        }
    }
}
