package controller;

import common.LogAspect;
import common.Result;
import entity.ReserveRecord;
import entity.ResourceStatDTO;
import entity.User;
import entity.UserStatDTO;
import enums.ReserveStatus;
import service.ReserveRecordService;
import common.util.ObjectReflectUtil;

import java.util.List;
import java.util.Map;

/**
 * 预约控制器：提交 / 通过 / 驳回 / 取消走切面；查询与统计直接调 Service
 */
public class ReserveController {

    private final ReserveRecordService reserveService = ReserveRecordService.getInstance();

    public Result<ReserveRecord> submit(String resourceId, String startText, String endText,
                                        String remark, User student){
        ReserveRecord apply = new ReserveRecord();
        apply.setResourceId(isEmpty(resourceId) ? null : resourceId.trim());
        apply.setReserveStartTime(reserveService.parseTime(startText));
        apply.setReserveEndTime(reserveService.parseTime(endText));
        apply.setRemark(remark);
        if (ObjectReflectUtil.isAllFieldNull(apply)) {
            return Result.fail("预约参数不能为空");
        }
        return (Result<ReserveRecord>) LogAspect.invoke(reserveService, "submit",
                new Class<?>[]{String.class, java.time.LocalDateTime.class,
                        java.time.LocalDateTime.class, String.class, User.class},
                new Object[]{resourceId.trim(), reserveService.parseTime(startText),
                        reserveService.parseTime(endText), remark, student},
                student.getUsername());

    }
    public Result<ReserveRecord> cancel(String recordId, User student) {
        if (isEmpty(recordId)) {
            return Result.fail("预约编号不能为空");
        }
        return (Result<ReserveRecord>) LogAspect.invoke(reserveService, "cancel",
                new Class<?>[]{String.class, User.class},
                new Object[]{recordId.trim(), student}, student.getUsername());

    }

    public Result<ReserveRecord> auditPass(String recordId, User admin) {
        if (isEmpty(recordId)) {
            return Result.fail("预约编号不能为空");
        }
        return (Result<ReserveRecord>) LogAspect.invoke(reserveService, "auditPass",
                new Class<?>[]{String.class, User.class},
                new Object[]{recordId.trim(), admin}, admin.getUsername());
    }

    public Result<ReserveRecord> auditReject(String recordId, String reason, User admin) {
        if (isEmpty(recordId)) {
            return Result.fail("预约编号不能为空");
        }
        if (isEmpty(reason)) {
            return Result.fail("驳回原因不能为空");
        }
        return (Result<ReserveRecord>) LogAspect.invoke(reserveService, "auditReject",
                new Class<?>[]{String.class, String.class, User.class},
                new Object[]{recordId.trim(), reason.trim(), admin}, admin.getUsername());
    }

    public Result<List<ReserveRecord>> myList(User student) {
        return reserveService.listMine(student);
    }


    public Result<List<ReserveRecord>> pending(User admin) {
        return reserveService.listPending(admin);
    }

    public Result<List<ReserveRecord>> all(User admin) {
        return reserveService.listAll(admin);
    }





    /** 状态字符串转枚举，非法值统一给中文提示，避免抛 IllegalArgumentException 到界面 */
    public Result<List<ReserveRecord>> query(String resourceId, String applicant,
                                             String statusText, User admin) {
        ReserveStatus status;
        try {
            status = reserveService.parseStatus(statusText);
        } catch (IllegalArgumentException e) {
            return Result.fail("状态只能是 PENDING / PASS / REJECT / CANCEL");
        }
        return reserveService.query(resourceId == null ? "" : resourceId.trim(),
                applicant == null ? "" : applicant.trim(), status, admin);
    }

    public Result<List<ResourceStatDTO>> statByResource() {
        return reserveService.statByResource();
    }

    public Result<List<ResourceStatDTO>> topN(int n) {
        return reserveService.topN(n);
    }

    public Result<List<UserStatDTO>> statByUser() {
        return reserveService.statByUser();
    }

    public Result<Map<ReserveStatus, Long>> statusSummary() {
        return reserveService.statusSummary();
    }


    private boolean isEmpty(String s){
        return s == null || s.trim().isEmpty();
    }
}
