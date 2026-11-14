import common.Result;
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

    /** 当前登录用户，null 表示未登录 */
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
                System.out.println("[失败] " + e.getMessage());
            } catch (Exception e) {
                System.out.println("[失败] 系统繁忙，请稍后再试");
            }
        }
    }

    /* ==================== 统一结果输出 ==================== */

    /** 控制台唯一的结果出口：成功打印 msg + data，失败只打印 msg */
    private static void printResult(Result<?>result){
        if (result == null){
            System.out.println("[失败] 操作无响应，请重试");
            return;
        }
        if (!result.isSuccess()){
            System.out.println("[失败] " + result.getMsg());
            return;
        }
        System.out.println("[成功] " + result.getMsg());
        Object data = result.getData();
        if(data instanceof List){
            List<?> list = (List<?>) data;
            if (list.isEmpty()){
                System.out.println("（暂无数据）");
            } else {
                for (Object o : list){
                    System.out.println(o);
                }
            }
        }else if (data!=null){
            System.out.println(data);
        }
    }

    /*===============菜单==================*/
    private static void printLogMenu(){
        System.out.println("========== 校园资源预约系统 V2.1 ==========");
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
                printResult(login(sc));
                break;
            case "2": printResult(register(sc)); break;
            default:
                System.out.println("输入有误，请重新选择！");
        }
    }
    private static void dispatchAdmin(String choice, Scanner sc){
        switch (choice){
            case "1": printResult(addResource(sc));        break;
            case "2": printResult(queryAllResources());    break;
            case "3": printResult(updateResource(sc));     break;
            case "4": printResult(deleteResource(sc));     break;
            case "5": printResult(queryAllUsers());        break;
            default:  System.out.println("输入有误，请重新选择！");

        }
    }
    private static void dispatchStudent(String choice, Scanner sc) {
        switch (choice) {
            case "1": printResult(listAvailableResources()); break;
            default:  System.out.println("输入有误，请重新选择！");
        }
    }

    /* ==================== 用户模块（统一返回 Result） ==================== */
    /** 注册主流程 */
    private static Result<User> register(Scanner sc) {
        System.out.println("---------- 用户注册 ----------");
        System.out.print("用户名：");
        String username = sc.nextLine().trim();
        System.out.print("密码（至少6位）：");
        String password = sc.nextLine().trim();

        if (username.isEmpty()) {
            return Result.fail("用户名和密码不能为空");
        }
        if (username.length() < 3 || username.length() > 16) {
            return Result.fail("用户名长度须在 3~16 个字符之间！");
        }
        if (password.length() < 6) {
            return Result.fail("密码长度不能少于 6 位！");
        }
        for (User u : USERS_LIST){
            if (u.getUsername().equals(username)) {
                return Result.fail("该用户名已被注册，请更换！");
            }
        }

        User user = new User(username, password, "student", LocalDateTime.now());
        USERS_LIST.add(user);
        FileUtil.writeUsers(USERS_LIST);
        return Result.success("注册成功", user);
    }
    /** 登录：账号、密码分别抛不同异常*/
    private static Result<User> login(Scanner sc) {
        System.out.print("账号：");
        String username = sc.nextLine().trim();
        System.out.print("密码：");
        String password = sc.nextLine().trim();

        if (username.isEmpty() || password.isEmpty()) {
            return Result.fail("账号和密码不能为空");
        }
        User target = null;
        for (User u : USERS_LIST) {
            if (u.getUsername().equals(username)) {
                if (!u.getPassword().equals(password)) {
                    return Result.fail("密码错误，请重新输入");
                }
                currentUser = u;
                String roleName = "admin".equals(u.getRole()) ? "管理员" : "同学";
                return Result.success("登录成功，欢迎您，" + roleName + "！", u);

            }
        }
        return Result.fail("账号不存在，请先注册");
    }
    /** 管理员：查看全部已注册用户 */
    private static Result<List<User>> queryAllUsers() {
        if (!isAdmin()) {
            return Result.fail("该操作仅管理员可执行");
        }
        return Result.success("共 " + USERS_LIST.size() + " 个用户", USERS_LIST);

    }

    /* ==================== 资源模块 ==================== */
    /**管理员：新增资源*/
    private static Result<CampusResource> addResource(Scanner sc) {
        if (!isAdmin()) {
            return Result.fail("该操作仅管理员可执行");
        }
        System.out.print("资源名称：");
        String name = sc.nextLine().trim();
        if (name.isEmpty()){
            return Result.fail("资源名称不能为空");
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
        return Result.success("资源新增成功", res);
    }

    /**管理员：查询全部资源*/
    private static Result<List<CampusResource>> queryAllResources() {
        if (!isAdmin()) {
            return Result.fail("该操作仅管理员可执行");
        }
        return Result.success("共 " + RESOURCE_LIST.size() + " 条资源", RESOURCE_LIST);
    }

    /**管理员：修改资源信息*/
    private static Result<CampusResource> updateResource(Scanner sc){
        if (!isAdmin()) {
            return Result.fail("该操作仅管理员可执行");
        }
        System.out.print("请输入要修改的资源ID： ");
        Result<CampusResource> found = findResourceById(sc.nextLine().trim());
        if (!found.isSuccess()) {
            return found;
        }
        CampusResource target = found.getData();

        System.out.print("新名称（回车不改）：");
        String name = sc.nextLine().trim();
        if (!name.isEmpty()) {
            target.setName(name);
        }
        System.out.print("新位置（回车不改）：");
        String location = sc.nextLine().trim();
        if (!location.isEmpty()) {
            target.setLocation(location);
        }
        System.out.print("新状态（AVAILABLE/OCCUPIED/DISABLED，回车不改）：");
        String status = sc.nextLine().trim();
        if (!status.isEmpty()) {
            try {
                target.setStatus(ResourceStatus.of(status));
            } catch (ParamEmptyException e) {
                return Result.fail(e.getMessage());
            }
        }
        saveResources();
        return Result.success("资源修改成功", target);

    }

    /**管理员：删除资源（存在预约记录时禁止删除）*/
    private static Result<Void> deleteResource(Scanner sc){
        if (!isAdmin()) {
            return Result.fail("该操作仅管理员可执行");
        }
        System.out.print("请输入要删除的资源 ID：");
        String id = sc.nextLine().trim();
        Result<CampusResource> found = findResourceById(id);
        if (!found.isSuccess()) {
            return Result.fail(found.getMsg());
        }
        if (isReferencedByReserve(id)) {
            return Result.fail("该资源存在关联预约记录，禁止删除");
        }
        RESOURCE_LIST.remove(found.getData());
        saveResources();
        return Result.success("资源删除成功");
    }

    /**学生，只看可用资源*/
    private static Result<List<CampusResource>> listAvailableResources() {
        List<CampusResource> list = new ArrayList<>();
        for (CampusResource r : RESOURCE_LIST) {
            if (ResourceStatus.AVAILABLE == r.getStatus()) {
                list.add(r);
            }
        }
        return Result.success("共 " + list.size() + " 条可预约资源", list);
    }


    private static Result<CampusResource> findResourceById(String id) {
        for (CampusResource r : RESOURCE_LIST) {
            if (r.getId().equals(id)) {
                return Result.success("查询成功", r);
            }
        }
        return Result.fail("未找到 ID 为 " + id + " 的资源");
    }

    private static boolean isAdmin() {
        return currentUser != null && "admin".equals(currentUser.getRole());
    }


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

    /* ==================== 资源持久化 ==================== */
    private static void loadResources() {
        RESOURCE_LIST.clear();
        FileUtil.ensureFile(RESOURCE_FILE);
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
