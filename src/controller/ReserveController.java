package controller;

import common.BusinessException;
import common.Result;
import entity.ReserveRecord;
import entity.User;
import enums.ReserveStatus;
import service.ReserveRecordService;

import java.util.List;

/**
 * 预约控制器：时间字符串的解析交给 Service 的 parseTime，本类只负责转发与异常兜底
 */
public class ReserveController {

    private final ReserveRecordService reserveService = ReserveRecordService.getInstance();

    public Result<ReserveRecord> submit(String resourceId, String startText, String endText,
                                        String remark, User student){
       if (isEmpty(resourceId)){
           return Result.fail("资源ID不能为空");
       }
       return reserveService.submit(resourceId.trim(),
               reserveService.parseTime(startText),
               reserveService.parseTime(endText),
               remark,
               student);
    }

    public Result<List<ReserveRecord>> myList(User student){
        return reserveService.listMine(student);
    }

    public Result<ReserveRecord> cancel(String recordId, User student) {
        if (isEmpty(recordId)) {
            return Result.fail("预约编号不能为空");
        }
        return reserveService.cancel(recordId.trim(), student);
    }

    public Result<List<ReserveRecord>> pending(User admin) {
        return reserveService.listPending(admin);
    }

    public Result<List<ReserveRecord>> all(User admin) {
        return reserveService.listAll(admin);
    }

    public Result<ReserveRecord> auditPass(String recordId, User admin) {
        if (isEmpty(recordId)) {
            return Result.fail("预约编号不能为空");
        }
        try {
            return reserveService.auditPass(recordId.trim(), admin);
        } catch (BusinessException e) {
            return Result.fail(e.getMessage());
        }
    }

    public Result<ReserveRecord> auditReject(String recordId, String reason, User admin) {
        if (isEmpty(recordId)) {
            return Result.fail("预约编号不能为空");
        }
        if (isEmpty(reason)) {
            return Result.fail("驳回原因不能为空");
        }
        try {
            return reserveService.auditReject(recordId.trim(), reason.trim(), admin);
        } catch (BusinessException e) {
            return Result.fail(e.getMessage());
        }
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


    private boolean isEmpty(String s){
        return s == null || s.trim().isEmpty();
    }
}
