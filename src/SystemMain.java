import java.util.Scanner;


public class SystemMain {

    // ===== 任务1：内置两组固定测试账号（任务2 将删除并改为集合存储）=====
    private static final String ADMIN_NAME = "admin";
    private static final String ADMIN_PWD  = "123456";
    private static final String STU_NAME   = "student";
    private static final String STU_PWD    = "123456";

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        while (true) {
            System.out.println("========== 校园资源预约系统 V1.0 ==========");
            System.out.println("1. 登录");
            System.out.println("0. 退出");
            System.out.print("请输入选项：");
            String choice = sc.nextLine().trim();

            if ("0".equals(choice)) {
                System.out.println("已退出，再见！");
                return;
            }
            if (!"1".equals(choice)) {
                System.out.println("输入有误，请重新选择！");
                continue;
            }
            login(sc);
        }
    }

    /** 登录：读取账号密码，输出三路结果 */
    private static void login(Scanner sc) {
        System.out.print("账号：");
        String username = sc.nextLine().trim();
        System.out.print("密码：");
        String password = sc.nextLine().trim();

        // 基础非空校验
        if (username.isEmpty() || password.isEmpty()) {
            System.out.println("账号和密码不能为空！");
            return;
        }

        if (ADMIN_NAME.equals(username) && ADMIN_PWD.equals(password)) {
            System.out.println("登录成功，欢迎您，管理员！");
        } else if (STU_NAME.equals(username) && STU_PWD.equals(password)) {
            System.out.println("登录成功，欢迎您，同学！");
        } else {
            System.out.println("登录失败：账号或密码错误！");
        }
    }
}
