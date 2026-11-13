import entity.CampusResource;
import entity.ResourceStatus;
import entity.User;
import exception.*;
import utils.FileUtil;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;

public class SystemMain {

    private static final List<User> USERS_LIST = new ArrayList<>();
    private static final List<CampusResource> RESOURCE_LIST = new ArrayList<>();
    private static final String RESOURCE_FILE = "resource.txt";

    /** 当前登录用户，null 表示未登录（本阶段用静态变量传递登录态） */
    private static User currentUser;

    public static void main(String[] args) {
        //启动先加载磁盘数据
        USERS_LIST.clear();
        USERS_LIST.addAll(FileUtil.readUsers());
        if (USERS_LIST.isEmpty()){
            USERS_LIST.add(new User("admin", "123456", "admin", LocalDateTime.now()));
            USERS_LIST.add(new User("student", "123456", "student", LocalDateTime.now()));
            FileUtil.writeUsers(USERS_LIST);
        }
        loadResources();

        Scanner sc = new Scanner(System.in);
        while (true) {
            try{
                if (currentUser == null) {
                    printLogMenu();
                    String choice = sc.nextLine().trim();
                    if ("0".equals(choice)) {
                        System.out.println("已退出，再见！");
                        return;
                    }
                    dispatchLogin(choice, sc);
                }else if("admin".equals(currentUser.getRole())) {
                    printAdminMenu();
                    String choice = sc.nextLine().trim();
                    if ("0".equals(choice)){
                        currentUser = null;
                        System.out.println("已退出登录，欢迎下次使用！");
                        continue;
                    }
                    dispatchAdmin(choice, sc);
                }else {
                    printStudentMenu();
                    String choice = sc.nextLine().trim();
                    if ("0".equals(choice)){
                        currentUser = null;
                        System.out.println("已退出登录，欢迎下次使用！");
                        continue;
                    }
                    dispatchStudent(choice, sc);
                }
            } catch (BusinessException e) {
                System.out.println("[提示] " + e.getMessage());   // 只给中文提示，不打印堆栈
            } catch (Exception e) {
                System.out.println("[提示] 输入有误，请重新选择！");   // 兜底，屏蔽底层英文异常
            }
        }
    }

    /*===============菜单==================*/
    private static void printLogMenu(){
        System.out.println("========== 校园资源预约系统 V2.0 ==========");
        System.out.println("1. 登录");
        System.out.println("2. 注册");
        System.out.println("0. 退出");
        System.out.print("请输入选项：");
    }

    private static void printAdminMenu(){
        System.out.println("========== 管理员菜单 (" + currentUser.getUsername() + ")==========");
        System.out.println("1. 新增校园资源");
        System.out.println("2. 查询全部资源");
        System.out.println("3. 修改资源信息");
        System.out.println("4. 删除资源");
        System.out.println("5. 查看全部用户");
        System.out.println("0. 退出登录");
        System.out.print("请输入选项：");

    }

    private static void printStudentMenu(){
        System.out.println("========== 学生菜单（" + currentUser.getUsername() + "）==========");
        System.out.println("1. 查看可预约资源");
        System.out.println("0. 退出登录");
        System.out.print("请输入选项：");

    }
    private static void dispatchLogin(String choice, Scanner sc){
        switch (choice) {
            case "1":
                currentUser = login(sc);
                break;
            case "2": register(sc); break;
            default:
                System.out.println("输入有误，请重新选择！");
        }
    }
    private static void dispatchAdmin(String choice, Scanner sc){
        switch (choice){
            case "1": addResource(sc);        break;
            case "2": queryAllResources();    break;
            case "3": updateResource(sc);     break;
            case "4": deleteResource(sc);     break;
            case "5": queryAllUsers();        break;
            default:  System.out.println("输入有误，请重新选择！");

        }
    }
    private static void dispatchStudent(String choice, Scanner sc) {
        switch (choice) {
            case "1": listAvailableResources(); break;
            default:  System.out.println("输入有误，请重新选择！");
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
    /** 管理员：查看全部已注册用户 */
    private static void queryAllUsers() {
        if (USERS_LIST.isEmpty()) {
            throw new BusinessException("暂无用户数据");
        }
        System.out.printf("%-12s %-8s %-20s%n", "用户名", "角色", "注册时间");
        for (User u : USERS_LIST) {
            System.out.printf("%-12s %-8s %-20s%n",
                    u.getUsername(), u.getRole(), u.getRegisterTime());
        }
    }

    /* ==================== 资源模块 ==================== */
    /**管理员：新增资源*/
    private static void addResource(Scanner sc) {
        checkAdmin();
        System.out.print("资源名称：");
        String name = sc.nextLine().trim();
        if (name.isEmpty()){
            throw new ParamEmptyException("资源名称不能为空");
        }
        for (CampusResource r : RESOURCE_LIST){
            if (r.getName().equals(name)){
                System.out.println("提示：已存在同名资源");
                break;
            }
        }
        System.out.print("资源类型（教室/实验室/设备/场地）：");
        String type = sc.nextLine().trim();
        System.out.print("位置：");
        String location = sc.nextLine().trim();
        System.out.print("描述（可空）：");
        String desc = sc.nextLine().trim();
        CampusResource res = new CampusResource(generateResourceId(), name, type,
                ResourceStatus.AVAILABLE, location, desc);
        RESOURCE_LIST.add(res);
        saveResources();
        System.out.println("资源新增成功：" + res);
    }

    /**管理员：查询全部资源*/
    private static void queryAllResources() {
        checkAdmin();
        if (RESOURCE_LIST.isEmpty()) {
            System.out.println("暂无资源数据");
            return;
        }
        System.out.printf("%-6s %-14s %-8s %-8s %-12s%n", "ID", "名称", "类型", "状态", "位置");
        for (CampusResource r : RESOURCE_LIST){
            System.out.printf("%-6s %-14s %-8s %-8s %-12s%n",
                    r.getId(), r.getName(), r.getType(), r.getStatus().getDesc(), r.getLocation());

        }
    }

    /**管理员：修改资源信息*/
    private static void updateResource(Scanner sc){
        checkAdmin();
        System.out.print("请输入要修改的资源ID： ");
        String id = sc.nextLine().trim();
        CampusResource target = requireResource(id);

        System.out.print("请输入新的资源名称： ");
        String name = sc.nextLine().trim();
        if (!name.isEmpty()){
            target.setName(name);
        }
        System.out.print("请输入新的资源类型： ");
        String location = sc.nextLine().trim();
        if (!location.isEmpty()){
            target.setLocation(location);
        }
        System.out.print("请输入新的资源位置： ");
        String status = sc.nextLine().trim();
        if (!status.isEmpty()){
            target.setStatus(ResourceStatus.of(status));
        }
        saveResources();
        System.out.println("资源修改成功：" + target);
    }

    /**管理员：删除资源（存在预约记录时禁止删除）*/
    private static void deleteResource(Scanner sc){
        checkAdmin();
        System.out.print("请输入要删除的资源 ID：");
        String id = sc.nextLine().trim();
        CampusResource target = requireResource(id);
        if (isReferencedByReserve(id)) {
            throw new BusinessException("该资源存在关联预约记录，禁止删除");
        }
        RESOURCE_LIST.remove(target);
        saveResources();
        System.out.println("资源删除成功：" + id);
    }

    /**学生，只看可用资源*/
    private static void listAvailableResources() {
        boolean empty = true;
        for (CampusResource r : RESOURCE_LIST) {
            if (ResourceStatus.AVAILABLE == r.getStatus()) {
                System.out.println(r);
                empty = false;
            }
        }
        if (empty) {
            System.out.println("暂无可预约资源");
        }
    }

    /** 按 ID 取资源，取不到抛业务异常（替代返回 null） */
    private static CampusResource requireResource(String id) {
        for (CampusResource r : RESOURCE_LIST) {
            if (r.getId().equals(id)) {
                return r;
            }
        }
        throw new ResourceNotFoundException("未找到 ID 为 " + id + " 的资源");
    }

    /** 权限校验：只有管理员能增删改 */
    private static void checkAdmin() {
        if (currentUser == null || !"admin".equals(currentUser.getRole())) {
            throw new NoPermissionException("该操作仅管理员可执行");
        }
    }

    /** 生成资源编号：R001、R002……取现有最大值 +1 */
    private static String generateResourceId() {
        int max = 0;
        for (CampusResource r : RESOURCE_LIST) {
            String num = r.getId().substring(1);
            max = Math.max(max, Integer.parseInt(num));
        }
        return String.format("R%03d", max + 1);
    }

    /** 占位：固定返回 false*/
    private static boolean isReferencedByReserve(String resourceId) {
        return false;
    }

    /* ==================== 资源持久化（内联 IO，任务10 才抽成 dao） ==================== */
    private static void loadResources() {
        RESOURCE_LIST.clear();
        File file = new File(RESOURCE_FILE);
        try {
            FileUtil.ensureFile(RESOURCE_FILE);
        } catch (RuntimeException e) {
            System.out.println("[提示] 资源文件创建失败：" + e.getMessage());
            return;
        }
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    continue;
                }
                String[] a = line.split(",", -1);
                if (a.length < 6) {
                    continue;                       // 脏数据跳过，不让程序崩
                }
                RESOURCE_LIST.add(new CampusResource(a[0], a[1], a[2],
                        ResourceStatus.valueOf(a[3]), a[4], a[5]));
            }
        } catch (IOException e) {
            System.out.println("[提示] 资源数据读取失败，已按空数据处理");
        }
    }

    private static void saveResources() {
        FileUtil.ensureFile(RESOURCE_FILE);
        try (BufferedWriter bw = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(RESOURCE_FILE), StandardCharsets.UTF_8))) {
            for (CampusResource r : RESOURCE_LIST) {
                bw.write(r.getId() + "," + r.getName() + "," + r.getType() + ","
                        + r.getStatus().name() + "," + r.getLocation() + ","
                        + (r.getDescription() == null ? "" : r.getDescription()));
                bw.newLine();
            }
            bw.flush();
        } catch (IOException e) {
            throw new BusinessException("保存资源数据失败", e);
        }
    }

}
