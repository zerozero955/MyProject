import entity.User;
import exception.*;
import utils.FileUtil;
import entity.User;
import java.time.LocalDateTime;
import java.util.*;

public class SystemMain {

    /**Map升级为List<entity.User>*/
    private static final List<User> USERS_LIST = new ArrayList<>();

    public static void main(String[] args) {
        //启动先加载磁盘数据
        USERS_LIST.clear();
        USERS_LIST.addAll(FileUtil.readUsers());
        if (USERS_LIST.isEmpty()){
            USERS_LIST.add(new User("admin", "123456", "admin", LocalDateTime.now()));
            USERS_LIST.add(new User("student", "123456", "student", LocalDateTime.now()));
            FileUtil.writeUsers(USERS_LIST);
        }

        Scanner sc = new Scanner(System.in);
        while (true) {
            try{
                printMenu();
                String choice = sc.nextLine().trim();
                if("0".equals(choice)){
                    System.out.println("已退出，再见！");
                    return;
                }
                dispatch(choice, sc);
            } catch (BusinessException e) {
                System.out.println("[提示] " + e.getMessage());   // 只给中文提示，不打印堆栈
            } catch (Exception e) {
                System.out.println("[提示] 输入有误，请重新选择！");   // 兜底，屏蔽底层英文异常
            }
        }
    }

    private static void printMenu(){
        System.out.println("========== 校园资源预约系统 V1.1 ==========");
        System.out.println("1. 登录");
        System.out.println("2. 注册");
        System.out.println("0. 退出");
        System.out.print("请输入选项：");
    }

    private static void dispatch(String choice, Scanner sc){
        switch (choice) {
            case "1": login(sc);    break;
            case "2": register(sc); break;
            default:
                System.out.println("输入有误，请重新选择！");
        }
    }

    /** 注册主流程 */
    private static void register(Scanner sc) {
        System.out.println("---------- 用户注册 ----------");
        System.out.print("用户名：");
        String username = sc.nextLine().trim();
        System.out.print("密码（至少6位）：");
        String password = sc.nextLine().trim();

        if (username.isEmpty()) {
            throw new ParamEmptyException("用户名不能为空");
        }
        if (username.length() < 3 || username.length() > 16) {
            throw new BusinessException("用户名长度须在 3~16 个字符之间！");
        }
        if (password.length() < 6) {
            throw new BusinessException("密码长度不能少于 6 位！");
        }
        for (User u : USERS_LIST){
            if (u.getUsername().equals(username)) {
                throw new UserAlreadyExistsException("该用户名已被注册，请更换！");
            }
        }

        User user = new User(username, password, "student", LocalDateTime.now());
        USERS_LIST.add(user);
        FileUtil.writeUsers(USERS_LIST);
        System.out.println("注册成功: "+ user);
    }
    /** 登录：账号、密码分别抛不同异常*/
    private static User login(Scanner sc) {
        System.out.print("账号：");
        String username = sc.nextLine().trim();
        System.out.print("密码：");
        String password = sc.nextLine().trim();

        if (username.isEmpty() || password.isEmpty()) {
           throw new ParamEmptyException("账号或密码不能为空");
        }
        User target = null;
        for (User u : USERS_LIST) {
            if (u.getUsername().equals(username)) {
                target = u;
                break;
            }
        }
        if (target == null) {
            throw new UserNotFoundException("账号不存在，请先注册");
        }
        if (!target.getPassword().equals(password)){
            throw new PasswordErrorException();
        }
        String rolename = "admin".equals(target.getUsername()) ? "管理员" : "同学";
        System.out.println("登录成功，欢迎您，" + rolename + "！");
        return target;
    }
}
