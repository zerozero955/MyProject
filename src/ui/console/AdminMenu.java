package ui.console;

import common.Result;
import entity.ReserveStatus;
import entity.User;
import service.ReserveRecordService;
import service.ResourceService;
import service.UserService;

import java.util.Scanner;


/**
 * 管理员菜单：只做输入收集与 Result 展示，业务全部在 Service
 */
public class AdminMenu {
    private final Scanner sc;
    private final User operator;
    private final UserService userService;
    private final ResourceService resourceService;
    private final ReserveRecordService reserveService;

    public AdminMenu(Scanner sc, User operator, UserService userService,
                     ResourceService resourceService, ReserveRecordService reserveService) {
        this.sc = sc;
        this.operator = operator;
        this.userService = userService;
        this.resourceService = resourceService;
        this.reserveService = reserveService;
    }

    public void start() {
        while (true) {
            System.out.println("========== 管理员菜单（" + operator.getUsername() + "）==========");
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
            String choice = sc.nextLine().trim();
            if ("0".equals(choice)) {
                System.out.println("已退出登录");
                return;
            }
            handle(choice);
        }
    }

    private void handle(String choice) {
        switch (choice) {
            case "1": doAdd();          break;
            case "2": doQueryAll();     break;
            case "3": doUpdate();       break;
            case "4": doDelete();       break;
            case "5": doListReserve();  break;
            case "6": doAudit();        break;
            case "7": doQueryReserve(); break;
            case "8": doListUsers();    break;
            default:  System.out.println("输入有误，请重新选择！");
        }
    }

    private void doAdd() {
        System.out.print("资源名称：");
        String name = sc.nextLine().trim();
        System.out.print("资源类型（教室/实验室/设备/场地）：");
        String type = sc.nextLine().trim();
        System.out.print("位置：");
        String location = sc.nextLine().trim();
        System.out.print("描述（可空）：");
        String desc = sc.nextLine().trim();
        printResult(resourceService.add(name, type, location, desc, operator));
    }

    private void doQueryAll(){
        printResult(resourceService.queryAll(operator));
    }

    private void doUpdate() {
        System.out.print("要修改的资源 ID：");
        String id = sc.nextLine().trim();
        System.out.print("新名称（回车不改）：");
        String name = sc.nextLine().trim();
        System.out.print("新位置（回车不改）：");
        String location = sc.nextLine().trim();
        System.out.print("新状态（AVAILABLE/OCCUPIED/DISABLED，回车不改）：");
        String status = sc.nextLine().trim();
        printResult(resourceService.update(id, name, location, status, operator));
    }

    private void doDelete() {
        System.out.print("要删除的资源 ID：");
        String id = sc.nextLine().trim();
        // 跨模块校验：有已通过的预约则禁止删除（任务19 由 Service 组合完成）
        if (reserveService.isReferencedByReserve(id)) {
            System.out.println("[失败] 该资源存在已通过的预约记录，禁止删除");
            return;
        }
        printResult(resourceService.delete(id, operator));
    }

    private void doListReserve() {
        printResult(reserveService.listAll(operator));
    }

    private void doAudit() {
        System.out.print("要审核的预约编号：");
        String recordId = sc.nextLine().trim();
        System.out.print("审核结果（1 通过 / 2 驳回）：");
        String flag = sc.nextLine().trim();
        if (!"1".equals(flag) && !"2".equals(flag)) {
            System.out.println("[失败] 审核结果只能输入 1 或 2");
            return;
        }
        printResult(reserveService.audit(recordId, "1".equals(flag), operator));
    }

    private void doQueryReserve() {
            System.out.print("资源 ID（回车跳过）：");
        String resourceId = sc.nextLine().trim();
        System.out.print("申请人（回车跳过）：");
        String applicant = sc.nextLine().trim();
        System.out.print("状态（PENDING/PASS/REJECT/CANCEL，回车跳过）：");
        String statusText = sc.nextLine().trim();
        ReserveStatus status;
        try {
            status = reserveService.parseStatus(statusText);
        } catch (IllegalArgumentException e) {
            System.out.println("[失败] 状态只能是 PENDING / PASS / REJECT / CANCEL");
            return;
        }
        printResult(reserveService.query(resourceId, applicant, status, operator));
    }

    private void doListUsers() {
        printResult(userService.queryAll(operator));
    }

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
