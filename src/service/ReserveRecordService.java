package service;

import common.Result;
import dao.ReserveRecordDao;
import entity.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 预约业务层：承接时间校验、状态变更与资源联动；依赖 ReserveRecordDao 与 ResourceService
 */

public class ReserveRecordService {
    private static final DateTimeFormatter TIME_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private final ReserveRecordDao reserveDao = new ReserveRecordDao();
    private final List<ReserveRecord> records;
    private final ResourceService resourceService;

    public ReserveRecordService(ResourceService resourceService) {
        this.resourceService = resourceService;
        this.records = reserveDao.loadAll();
    }

    /** 学生：提交预约 */
    public Result<ReserveRecord> submit(String resourceId, LocalDateTime start, LocalDateTime end,
                                        String remark, User student) {
        if (student == null || !"student".equals(student.getRole())) {
            return Result.fail("仅学生可以提交预约申请");
        }
        Optional<CampusResource> optRes = resourceService.findById(resourceId);
        if (!optRes.isPresent()) {
            return Result.fail("未找到 ID 为 " + resourceId + " 的资源");
        }
        CampusResource resource = optRes.get();
        if (ResourceStatus.AVAILABLE != resource.getStatus()) {
            return Result.fail("该资源当前状态为【" + resource.getStatus().getDesc() + "】，不可预约");
        }
        if (start == null || end == null) {
            return Result.fail("时间格式错误，正确格式如 2025-06-01 09:00");
        }
        if (!end.isAfter(start)) {
            return Result.fail("结束时间必须晚于开始时间");
        }
        if (end.isBefore(LocalDateTime.now())) {
            return Result.fail("预约结束时间不能早于当前时间");
        }
        if (hasConflict(resourceId, start, end)) {
            return Result.fail("该时段已被预约，请更换时间");
        }
        ReserveRecord record = new ReserveRecord(nextId(), resourceId, student.getUsername(),
                LocalDateTime.now(), start, end, ReserveStatus.PENDING, remark);
        records.add(record);
            reserveDao.saveAll(records);
        return Result.success("预约申请已提交，等待管理员审核", record);
    }

    /** 冲突校验：同资源存在 PASS 且时间重叠 */
    public boolean hasConflict(String resourceId, LocalDateTime start, LocalDateTime end) {
        return records.stream()
                .filter(r -> r.getResourceId().equals(resourceId))
                .filter(r -> ReserveStatus.PASS == r.getStatus())
                .anyMatch(r -> start.isBefore(r.getReserveEndTime())
                        && end.isAfter(r.getReserveStartTime()));
    }

    public Result<List<ReserveRecord>> listMine(User student) {
        List<ReserveRecord> mine = records.stream()
                .filter(r -> r.getApplicantUsername().equals(student.getUsername()))
                .sorted(Comparator.comparing(ReserveRecord::getApplyTime).reversed())
                .collect(Collectors.toList());
        return Result.success("共 " + mine.size() + " 条预约", mine);
    }

    public Result<Void> cancel(String recordId, User student) {
         Optional<ReserveRecord> opt = findById(recordId);
            if (!opt.isPresent()) {
            return Result.fail("未找到编号为 " + recordId + " 的预约记录");
        }
        ReserveRecord record = opt.get();
        if (!record.getApplicantUsername().equals(student.getUsername())) {
            return Result.fail("无权操作他人的预约记录");
        }
        if (ReserveStatus.REJECT == record.getStatus() || ReserveStatus.CANCEL == record.getStatus()) {
            return Result.fail("该预约已" + record.getStatus().getDesc() + "，不能取消");
        }
        boolean wasPassed = ReserveStatus.PASS == record.getStatus();
        record.setStatus(ReserveStatus.CANCEL);
        if (wasPassed) {
            resourceService.updateStatus(record.getResourceId(), ResourceStatus.AVAILABLE);
        }
        reserveDao.saveAll(records);
        return Result.success("预约已取消");
    }

    public Result<List<ReserveRecord>> listAll(User admin) {
        if (!isAdmin(admin)) {
            return Result.fail("仅管理员可查看全部预约");
        }
        return Result.success("共 " + records.size() + " 条预约记录",
                records.stream()
                .sorted(Comparator.comparing(ReserveRecord::getApplyTime).reversed())
                .collect(Collectors.toList()));
    }

    public Result<Void> audit(String recordId, boolean pass, User admin) {
        if (!isAdmin(admin)) {
            return Result.fail("仅管理员可执行预约审核");
        }
             Optional<ReserveRecord> opt = findById(recordId);
         if (!opt.isPresent()) {
                 return Result.fail("未找到编号为 " + recordId + " 的预约记录");
        }
        ReserveRecord record = opt.get();
        record.setStatus(pass ? ReserveStatus.PASS : ReserveStatus.REJECT);
        if (pass) {
            resourceService.updateStatus(record.getResourceId(), ResourceStatus.OCCUPIED);
        }
        reserveDao.saveAll(records);
        return Result.success(pass ? "审核通过，资源已置为占用" : "已驳回该预约申请");
    }

    public Result<List<ReserveRecord>> query(String resourceId, String applicant,
                                             ReserveStatus status, User admin) {
        if (!isAdmin(admin)) {
            return Result.fail("仅管理员可查询全部预约");
        }
        List<ReserveRecord> list = records.stream()
                .filter(r -> resourceId == null || resourceId.isEmpty() || r.getResourceId().equals(resourceId))
                .filter(r -> applicant == null || applicant.isEmpty() || r.getApplicantUsername().equals(applicant))
                .filter(r -> status == null || status == r.getStatus())
                .collect(Collectors.toList());
        return Result.success("共 " + list.size() + " 条记录", list);
    }

    /** 资源是否被已通过的预约占用（删除资源前置校验） */
    public boolean isReferencedByReserve(String resourceId) {
        return records.stream()
                .anyMatch(r -> r.getResourceId().equals(resourceId)
                        && ReserveStatus.PASS == r.getStatus());
    }
    public ReserveStatus parseStatus(String text) {
        if (text == null || text.trim().isEmpty()) {
            return null;
        }
        return ReserveStatus.valueOf(text.trim().toUpperCase());
    }

    public LocalDateTime parseTime(String text) {
        try {
             return LocalDateTime.parse(text.trim(), TIME_FMT);
        } catch (DateTimeParseException e) {
                 return null;
        }
     }

    private Optional<ReserveRecord> findById(String recordId) {
        return records.stream().filter(r -> r.getRecordId().equals(recordId)).findFirst();
    }

    private boolean isAdmin(User user) {
        return user != null && "admin".equals(user.getRole());
    }

    private String nextId() {
        int max = records.stream()
                .mapToInt(r -> Integer.parseInt(r.getRecordId().substring(1)))
                .max()
                .orElse(0);
        return String.format("Y%03d", max + 1);
    }

}
