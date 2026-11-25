package view;


import common.Result;
import controller.ReserveController;
import controller.ResourceController;
import entity.ReserveRecord;
import entity.User;

import java.util.List;
import java.util.Scanner;

/**
 * 学生视图：4 项菜单，只负责收集输入与展示 Result
 */
public class StudentView {
    private final Scanner sc =new Scanner(System.in);
    private final User operator;
    private final ResourceController resourceController = new ResourceController();
    private final ReserveController reserveController = new ReserveController();

    public StudentView(User operator){
        this.operator = operator;
    }

    public void start() {
        while (true) {
            printMenu();
            String choice = sc.nextLine().trim();
            switch (choice) {
                case "1": doAvailable(); break;
                case "2": doSubmit();    break;
                case "3": doMine();      break;
                case "4": doCancel();    break;
                case "0":
                    System.out.println("已退出登录");
                    return;
                default:
                    System.out.println("输入有误，请重新选择！");
            }
        }
    }

    private void printMenu() {
        System.out.println("========== 学生菜单（" + operator.getUsername() + "）==========");
        System.out.println("1. 查看可预约资源");
        System.out.println("2. 提交预约申请");
        System.out.println("3. 我的预约（含驳回原因）");
        System.out.println("4. 取消预约");
        System.out.println("0. 退出登录");
        System.out.print("请输入选项：");
    }

    private void doAvailable() {
        printResult(resourceController.queryAvailable());
    }

    private void doSubmit() {
        System.out.print("资源 ID：");
        String resourceId = sc.nextLine().trim();
        System.out.print("开始时间（yyyy-MM-dd HH:mm）：");
        String start = sc.nextLine();
        System.out.print("结束时间（yyyy-MM-dd HH:mm）：");
        String end = sc.nextLine();
        System.out.print("备注（可空）：");
        String remark = sc.nextLine().trim();
        printResult(reserveController.submit(resourceId, start, end, remark, operator));
    }

    private void doMine() {
        Result<List<ReserveRecord>> result = reserveController.myList(operator);
        if (!result.isSuccess()) {
            System.out.println("[失败] " + result.getMsg());
            return;
        }
        if (result.getData().isEmpty()) {
            System.out.println("（暂无预约记录）");
            return;
        }
        for (ReserveRecord r : result.getData()) {
            System.out.println(r.toDisplayString());
        }
    }

    private void doCancel() {
        System.out.println("提示：只有【待审核】【已通过】的预约可以取消");
        System.out.print("要取消的预约编号：");
        String recordId = sc.nextLine().trim();
        printResult(reserveController.cancel(recordId, operator));
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
