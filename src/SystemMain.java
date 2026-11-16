import common.Result;
import entity.*;
import exception.*;
import utils.FileUtil;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.DateTimeException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class SystemMain {

    private static final List<User> USERS_LIST = new ArrayList<>();
    private static final List<CampusResource> RESOURCE_LIST = new ArrayList<>();
    private static final List<ReserveRecord> RESERVE_LIST = new ArrayList<>();

    private static final String RESOURCE_FILE = "resource.txt";
    private static final String RESERVE_FILE = "reserve.txt";
    private static final DateTimeFormatter TIME_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

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
        loadReserves();

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

    /* ==================== 统一输出 ==================== */

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
        System.out.println("========== 校园资源预约系统 V3.0 ==========");
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
        System.out.println("5. 查看全部预约");
        System.out.println("6. 审核预约");
        System.out.println("7. 按条件查询预约");
        System.out.println("8. 查看全部用户");
        System.out.println("0. 退出登录");
        System.out.print("请输入选项：");

    }

    private static void printStudentMenu(){
        System.out.println("========== 学生菜单（" + currentUser.getUsername() + "）==========");
        System.out.println("1. 查看可预约资源");
        System.out.println("2. 提交预约申请");
        System.out.println("3. 我的预约");
        System.out.println("4. 取消预约");
        System.out.println("0. 退出登录");
        System.out.print("请输入选项：");

    }
    private static void dispatchLogin(String choice, Scanner sc){
        switch (choice) {
            case "1": printResult(login(sc));
                break;
            case "2": printResult(register(sc));
                break;
            default: System.out.println("输入有误，请重新选择！");
        }
    }
    private static void dispatchAdmin(String choice, Scanner sc){
        switch (choice){
            case "1": printResult(addResource(sc));           break;
            case "2": printResult(queryAllResources());       break;
            case "3": printResult(updateResource(sc));        break;
            case "4": printResult(deleteResource(sc));        break;
            case "5": printResult(listAllReserve());          break;
            case "6": printResult(auditReserve(sc));          break;
            case "7": printResult(queryReserve(sc));          break;
            case "8": printResult(queryAllUsers());           break;

            default:  System.out.println("输入有误，请重新选择！");

        }
    }
    private static void dispatchStudent(String choice, Scanner sc) {
        switch (choice) {
            case "1": printResult(listAvailableResources()); break;
            case "2": printResult(submitReserve(sc));        break;
            case "3": printResult(listMyReserve());          break;
            case "4": printResult(cancelReserve(sc));        break;
            default:  System.out.println("输入有误，请重新选择！");
        }
    }

    /* ==================== 用户模块 ==================== */
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
        boolean exists = USERS_LIST.stream().anyMatch(u -> u.getUsername().equals(username));
        if (exists) {
            return Result.fail("该用户名已被注册，请更换！");
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
        Optional<User> opt = USERS_LIST.stream()
                .filter(u -> u.getUsername().equals(username))
                .findFirst();
        if (!opt.isPresent()) {
            return Result.fail("账号不存在，请先注册");
        }
        User u = opt.get();
        if (!u.getPassword().equals(password)) {
            return Result.fail("密码错误，请重新输入");
        }
        currentUser = u;
        String roleName = "admin".equals(u.getRole()) ? "管理员" : "同学";
        return Result.success("登录成功，欢迎您，" + roleName + "！", u);
    }
    /** 管理员：查看全部已注册用户 */
    private static Result<List<User>> queryAllUsers() {
        if (!isAdmin()) {
            return Result.fail("该操作仅管理员可执行");
        }
        List<User> list = USERS_LIST.stream()
                .sorted(Comparator.comparing(User::getUsername))
                .collect(Collectors.toList());
        return Result.success("共 " + list.size() + " 个用户", list);
    }

    /* ==================== 资源模块 ==================== */
    /** 可预约资源：filter + sorted + collect */
    private static Result<List<CampusResource>> listAvailableResources() {
        List<CampusResource> list = new ArrayList<>();
        for (CampusResource r : RESOURCE_LIST) {
            if (ResourceStatus.AVAILABLE == r.getStatus()) {
                list.add(r);
            }
        }
        return Result.success("共 " + list.size() + " 条可预约资源", list);
    }


    /**按编号升序查询全部（管理员）*/
    private static Result<List<CampusResource>> queryAllResources() {
        if (!isAdmin()) {
            return Result.fail("该操作仅管理员可执行");
        }
        List<CampusResource> list = RESOURCE_LIST.stream()
                .sorted(Comparator.comparing(CampusResource::getId))
                .collect(Collectors.toList());
        return Result.success("共 " + list.size() + " 条资源", list);
    }

    /** 按 ID 查询：返回 Optional，调用方必须显式处理"查不到" */
    private static Optional<CampusResource> findById(String id) {
        if (id == null || id.trim().isEmpty()) {
            return Optional.empty();
        }
        return RESOURCE_LIST.stream().filter(r -> r.getId().equals(id.trim())).findFirst();
    }

    /* ==================== 资源增删改 ==================== */
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



    /**管理员：修改资源信息*/
    private static Result<CampusResource> updateResource(Scanner sc){
        if (!isAdmin()) {
            return Result.fail("该操作仅管理员可执行");
        }
        System.out.print("请输入要修改的资源ID： ");
        String id=sc.nextLine().trim();
        Optional<CampusResource> opt = findById(id);
        if (!opt.isPresent()){
            return Result.fail("未找到 ID 为 " + id + " 的资源");
        }
        CampusResource target = opt.get();

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
        Optional<CampusResource> opt = findById(id);
        if (!opt.isPresent()) {
            return Result.fail("未找到 ID 为 " + id + " 的资源");
        }
        if (isReferencedByReserve(id)) {
            return Result.fail("该资源存在已通过的预约记录，禁止删除");
        }
        RESOURCE_LIST.remove(opt.get());
        saveResources();
        return Result.success("资源删除成功");
    }

    /* ==================== 预约模块（本阶核心） ==================== */
    /** 学生提交预约：资源存在且可用，用户余额充足即可预约 */
    private static Result<ReserveRecord> submitReserve(Scanner sc) {
        if (isAdmin()){
            return Result.fail("该操作仅学生可执行");
        }
        System.out.print("可预约资源 ID：");
        List<CampusResource> available = RESOURCE_LIST.stream()
                .filter(r ->ResourceStatus.AVAILABLE == r.getStatus())
                .sorted(Comparator.comparing(CampusResource::getId))
                .collect(Collectors.toList());
        if (available.isEmpty()){
            return Result.fail("目前没有可预约的资源");
        }
        available.forEach(System.out::println);

        System.out.print("请选择要预约的资源 ID：");
        String resourceId = sc.nextLine().trim();
        Optional<CampusResource> optRes = findById(resourceId);
        if (!optRes.isPresent()){
            return Result.fail("未找到 ID 为 " + resourceId + " 的资源");
        }
        CampusResource resource = optRes.get();
        if (ResourceStatus.AVAILABLE != resource.getStatus()){
            return Result.fail("该资源当前状态为【" + resource.getStatus().getDesc() + "】，不可预约");
        }

        System.out.print("使用开始时间（yyyy-MM-dd HH:mm）：");
        LocalDateTime start = parseTime(sc.nextLine().trim());
        System.out.print("使用结束时间（yyyy-MM-dd HH:mm）：");
        LocalDateTime end = parseTime(sc.nextLine().trim());
        System.out.print("备注（可空）：");
        String remark = sc.nextLine().trim();

        if (start == null || end == null){
            return Result.fail("时间格式错误，正确格式如 2025-06-01 09:00");
        }
        if (!end.isAfter(start)){
            return Result.fail("结束时间必须晚于开始时间");
        }
        if (end.isBefore(LocalDateTime.now())){
            return Result.fail("结束时间必须晚于当前时间");
        }
        if (hasTimeConflict(resourceId, start, end)) {
            return Result.fail("该资源在指定时间已预约，请选择其他时间");
        }

        ReserveRecord record = new ReserveRecord(generateResourceId(), resourceId,
                currentUser.getUsername(), LocalDateTime.now(), start, end,
                ReserveStatus.PENDING, remark);
        saveResources();
        return Result.success("预约提交成功", record);
    }

    /** 时间冲突：同资源存在 PASS 且区间重叠（start < 已有 end && end > 已有 start） */
    private static boolean hasTimeConflict(String resourceId, LocalDateTime start, LocalDateTime end) {
        return RESERVE_LIST.stream()
                .filter(r -> r.getResourceId().equals(resourceId))
                .filter(r -> ReserveStatus.PASS == r.getStatus())
                .anyMatch(r -> start.isBefore(r.getReserveEndTime())
                        && end.isAfter(r.getReserveStartTime()));
    }

    private static LocalDateTime parseTime(String text) {
        try {
            return LocalDateTime.parse(text, TIME_FMT);
        } catch (DateTimeException e){
            return null;
        }
    }

    private static String generateRecordId() {
        int max = RESERVE_LIST.stream()
                .mapToInt(r -> Integer.parseInt(r.getRecordId().substring(1)))
                .max()
                .orElse(0);
        return String.format("Y%03d", max + 1);
    }

    /** 学生：我的预约（Stream 过滤当前用户） */
    private static Result<List<ReserveRecord>> listMyReserve(){
        List<ReserveRecord> mine = RESERVE_LIST.stream()
                .filter(r -> r.getApplicantUsername().equals(currentUser.getUsername()))
                .sorted(Comparator.comparing(ReserveRecord::getApplyTime).reversed())
                .collect(Collectors.toList());
        return Result.success("共 " + mine.size() + " 条预约", mine);
    }

    /** 学生：取消预约（仅待审核 / 已通过可取消） */
    private static Result<Void> cancelReserve(Scanner sc){
        System.out.print("请输入要取消的预约编号：");
        String recordId = sc.nextLine().trim();
        Optional<ReserveRecord> opt = RESERVE_LIST.stream()
                .filter(r -> r.getRecordId().equals(recordId))
                .findFirst();
        if (!opt.isPresent()) {
            return Result.fail("未找到编号为 " + recordId + " 的预约记录");
        }
        ReserveRecord record = opt.get();
        if (!record.getApplicantUsername().equals(currentUser.getUsername())) {
            return Result.fail("无权操作他人的预约记录");
        }
        if (ReserveStatus.REJECT == record.getStatus() || ReserveStatus.CANCEL == record.getStatus()) {
            return Result.fail("该预约已" + record.getStatus().getDesc() + "，不能取消");
        }
        boolean wasPassed = ReserveStatus.PASS == record.getStatus();
        record.setStatus(ReserveStatus.CANCEL);
        if (wasPassed) {
            findById(record.getResourceId()).ifPresent(r -> r.setStatus(ResourceStatus.AVAILABLE));
            saveResources();
        }
        saveReserves();
        return Result.success("预约已取消");
    }

    /** 管理员：全部预约 */
    private static Result<List<ReserveRecord>> listAllReserve() {
        if (!isAdmin()) {
            return Result.fail("仅管理员可查看全部预约");
        }
        List<ReserveRecord> list = RESERVE_LIST.stream()
                .sorted(Comparator.comparing(ReserveRecord::getApplyTime).reversed())
                .collect(Collectors.toList());
        return Result.success("共 " + list.size() + " 条预约记录", list);
    }

    /** 管理员：审核（pass=true 通过 / false 驳回） */
    private static Result<Void> auditReserve(Scanner sc) {
        if (!isAdmin()) {
            return Result.fail("仅管理员可执行预约审核");
        }
        System.out.print("请输入要审核的预约编号：");
        String recordId = sc.nextLine().trim();
        Optional<ReserveRecord> opt = RESERVE_LIST.stream()
                .filter(r -> r.getRecordId().equals(recordId))
                .findFirst();
        if (!opt.isPresent()) {
            return Result.fail("未找到编号为 " + recordId + " 的预约记录");
        }
        ReserveRecord record = opt.get();
        System.out.print("审核结果（1 通过 / 2 驳回）：");
        String flag = sc.nextLine().trim();
        if (!"1".equals(flag) && !"2".equals(flag)) {
            return Result.fail("审核结果只能输入 1 或 2");
        }
        boolean pass = "1".equals(flag);
        record.setStatus(pass ? ReserveStatus.PASS : ReserveStatus.REJECT);
        if (pass) {
            findById(record.getResourceId()).ifPresent(r -> r.setStatus(ResourceStatus.OCCUPIED));
            saveResources();
        }
        saveReserves();
        return Result.success(pass ? "审核通过，资源已置为占用" : "已驳回该预约申请");
    }

    /** 管理员：按条件查询预约（资源 ID / 申请人 / 状态，均可为空） */
    private static Result<List<ReserveRecord>> queryReserve(Scanner sc) {
        if (!isAdmin()) {
            return Result.fail("仅管理员可查询全部预约");
        }
        System.out.print("资源 ID（回车跳过）：");
        String resourceId = sc.nextLine().trim();
        System.out.print("申请人用户名（回车跳过）：");
        String applicant = sc.nextLine().trim();
        System.out.print("状态（PENDING/PASS/REJECT/CANCEL，回车跳过）：");
        String statusText = sc.nextLine().trim();
        ReserveStatus status = null;
        if (!statusText.isEmpty()) {
            try {
                status = ReserveStatus.valueOf(statusText.toUpperCase());
            } catch (IllegalArgumentException e) {
                return Result.fail("状态只能是 PENDING / PASS / REJECT / CANCEL");
            }
        }
        ReserveStatus finalStatus = status;
        List<ReserveRecord> list = RESERVE_LIST.stream()
                .filter(r -> resourceId.isEmpty() || r.getResourceId().equals(resourceId))
                .filter(r -> applicant.isEmpty() || r.getApplicantUsername().equals(applicant))
                .filter(r -> finalStatus == null || finalStatus == r.getStatus())
                .collect(Collectors.toList());
        return Result.success("共 " + list.size() + " 条记录", list);
    }


    /* ==================== 工具方法 ==================== */

    private static boolean isAdmin() {
        return currentUser != null && "admin".equals(currentUser.getRole());
    }


    private static String generateResourceId() {
        int max = RESOURCE_LIST.stream()
                .mapToInt(r ->Integer.parseInt(r.getId().substring(1)))
                .max()
                .orElse(0);
        return String.format("R%03d", max + 1);
    }

    /** 资源删除校验升级：存在 PASS 记录的资源禁止删除 */
    private static boolean isReferencedByReserve(String resourceId) {
        return RESERVE_LIST.stream()
                .anyMatch(r -> r.getResourceId().equals(resourceId)
                        && ReserveStatus.PASS == r.getStatus());
    }

    /* ==================== 持久化 ==================== */
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


    private static void loadReserves() {
        RESERVE_LIST.clear();
        FileUtil.ensureFile(RESERVE_FILE);
        File file = new File(RESERVE_FILE);
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    continue;
                }
                String[] a = line.split(",", -1);
                if (a.length < 8) {
                    continue;
                }
                RESERVE_LIST.add(new ReserveRecord(a[0], a[1], a[2],
                        LocalDateTime.parse(a[3]), LocalDateTime.parse(a[4]),
                        LocalDateTime.parse(a[5]), ReserveStatus.valueOf(a[6]), a[7]));
            }
        } catch (IOException e) {
            System.out.println("[提示] 预约数据读取失败，已按空数据处理");
        }
    }

    private static void saveReserves() {
        FileUtil.ensureFile(RESERVE_FILE);
        try (BufferedWriter bw = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(RESERVE_FILE), StandardCharsets.UTF_8))) {
            for (ReserveRecord r : RESERVE_LIST) {
                bw.write(r.getRecordId() + "," + r.getResourceId() + ","
                        + r.getApplicantUsername() + "," + r.getApplyTime() + ","
                        + r.getReserveStartTime() + "," + r.getReserveEndTime() + ","
                        + r.getStatus().name() + ","
                        + (r.getRemark() == null ? "" : r.getRemark()));
                bw.newLine();
            }
            bw.flush();
        } catch (IOException e) {
            throw new BusinessException("保存预约数据失败", e);
        }
    }
}
