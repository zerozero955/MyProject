package view;

import common.Result;
import controller.ReserveController;
import controller.ResourceController;
import controller.UserController;
import entity.ResourceStatDTO;
import entity.User;
import entity.UserStatDTO;
import enums.ReserveStatus;

import java.util.List;
import java.util.Map;
import java.util.Scanner;

/**
 * 管理员视图：9 项菜单，只负责收集输入与展示 Result
 */
public class AdminView {

    private final Scanner sc = new Scanner(System.in);
    private final User operator;
    private final UserController userController = new UserController();
    private final ResourceController resourceController = new ResourceController();
    private final ReserveController reserveController = new ReserveController();

    public AdminView(User operator){
        this.operator = operator;
    }

    public void start() {
        while (true) {
            printMenu();
            String choice = sc.nextLine().trim();
            if ("0".equals(choice)) {
                System.out.println("已退出登录");
                return;
            }
            handle(choice);
        }
    }

    private void printMenu() {
        System.out.println("========== 管理员菜单（" + operator.getUsername() + "）==========");
        System.out.println("1. 新增校园资源");
        System.out.println("2. 查询全部资源");
        System.out.println("3. 修改资源信息");
        System.out.println("4. 删除资源");
        System.out.println("5. 查看待审核预约");
        System.out.println("6. 审核预约（通过 / 驳回）");
        System.out.println("7. 查看全部预约");
        System.out.println("8. 按条件查询预约");
        System.out.println("9. 查看全部用户");
        System.out.println("10. 资源预约统计报表");
        System.out.println("11. 热门资源 TOP 排行");
        System.out.println("12. 用户预约统计");
        System.out.println("13. 全局预约状态汇总");
        System.out.println("0. 退出登录");
        System.out.print("请输入选项：");
    }

    private void handle(String choice) {
        switch (choice) {
            case "1": doAdd();          break;
            case "2": doQueryAll();     break;
            case "3": doUpdate();       break;
            case "4": doDelete();       break;
            case "5": doPending();      break;
            case "6": doAudit();        break;
            case "7": doListAll();      break;
            case "8": doQueryReserve(); break;
            case "9": doListUsers();    break;
            case "10": doStat();        break;
            case "11": doTopN();        break;
            case "12": doUserStat();    break;
            case "13": doStatusSummary(); break;
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
        printResult(resourceController.add(name, type, location, desc, operator));
    }

    private void doQueryAll() {
        printResult(resourceController.queryAll(operator));
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
        printResult(resourceController.update(id, name, location, status, operator));
    }

    private void doDelete() {
        System.out.print("要删除的资源 ID：");
        String id = sc.nextLine().trim();
        printResult(resourceController.delete(id, operator));
    }

    private void doPending() {
        printResult(reserveController.pending(operator));
    }

    private void doAudit() {
        printResult(reserveController.pending(operator));
        System.out.print("要审核的预约编号：");
        String recordId = sc.nextLine().trim();
        System.out.print("审核结果（1 通过 / 2 驳回）：");
        String flag = sc.nextLine().trim();
        if ("1".equals(flag)) {
            printResult(reserveController.auditPass(recordId, operator));
        } else if ("2".equals(flag)) {
            System.out.print("请输入驳回原因：");
            String reason = sc.nextLine().trim();
            printResult(reserveController.auditReject(recordId, reason, operator));
        } else {
            System.out.println("[失败] 审核结果只能输入 1 或 2");
        }
    }

    private void doListAll() {
        printResult(reserveController.all(operator));
    }

    private void doQueryReserve() {
        System.out.print("资源 ID（回车跳过）：");
        String resourceId = sc.nextLine().trim();
        System.out.print("申请人（回车跳过）：");
        String applicant = sc.nextLine().trim();
        System.out.print("状态（PENDING/PASS/REJECT/CANCEL，回车跳过）：");
        String status = sc.nextLine().trim();
        printResult(reserveController.query(resourceId, applicant, status, operator));
    }

    private void doListUsers() {
        printResult(userController.queryAll(operator));
    }

    /* ==================== 统计报表（视图只负责展示） ==================== */

    private void doStat() {
        Result<List<ResourceStatDTO>> result = reserveController.statByResource();
        if (!result.isSuccess()) {
            System.out.println("[失败] " + result.getMsg());
            return;
        }
        printStatTable(result.getData());
    }

    private void doTopN() {
        System.out.print("要展示前几名：");
        int n;
        try {
            n = Integer.parseInt(sc.nextLine().trim());
        } catch (NumberFormatException e) {
            System.out.println("[失败] 请输入数字");
            return;
        }
        Result<List<ResourceStatDTO>> result = reserveController.topN(n);
        if (!result.isSuccess()) {
            System.out.println("[失败] " + result.getMsg());
            return;
        }
        printStatTable(result.getData());
    }

    private void doUserStat() {
        Result<List<UserStatDTO>> result = reserveController.statByUser();
        if (!result.isSuccess()) {
            System.out.println("[失败] " + result.getMsg());
            return;
        }
        if (result.getData().isEmpty()) {
            System.out.println("（暂无预约数据，报表为空）");
            return;
        }
        for (UserStatDTO d : result.getData()) {
            System.out.println(d);
        }
    }

    private void doStatusSummary() {
        Result<Map<ReserveStatus, Long>> result = reserveController.statusSummary();
        if (!result.isSuccess()) {
            System.out.println("[失败] " + result.getMsg());
            return;
        }
        System.out.println("---------- 全局预约状态汇总 ----------");
        for (Map.Entry<ReserveStatus, Long> e : result.getData().entrySet()) {
            System.out.printf("%-8s %d 条%n", e.getKey().getDesc(), e.getValue());
        }
    }

    /** 统计表打印：通过率转百分比保留 1 位小数 */
    private void printStatTable(List<ResourceStatDTO> list) {
        if (list.isEmpty()) {
            System.out.println("（暂无预约数据，报表为空）");
            return;
        }
        System.out.printf("%-6s %-14s %-8s %-8s %-8s %-8s %-8s%n",
                "资源ID", "资源名称", "总申请", "通过", "驳回", "取消", "通过率");
        for (ResourceStatDTO d : list) {
            System.out.printf("%-6s %-14s %-8d %-8d %-8d %-8d %-8s%n",
                    d.getResourceId(), d.getResourceName(), d.getTotalApplyCount(),
                    d.getPassCount(), d.getRejectCount(), d.getCancelCount(),
                    String.format("%.1f%%", d.getPassRate() * 100));
        }
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
