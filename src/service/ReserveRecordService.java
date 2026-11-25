package service;

import common.BusinessException;
import common.Result;
import dao.DaoFactory;
import dao.ReserveRecordDao;
import entity.*;
import enums.ReserveStatus;
import enums.ResourceStatus;
import exception.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 预约业务层：本阶段核心是状态机
 * 只允许 4 条合法流转：PENDING->PASS、PENDING->REJECT、PENDING->CANCEL、PASS->CANCEL
 * 其余全部被 assertTransfer 拦截并抛 IllegalStatusTransferException

 */

public class ReserveRecordService {
    private static final DateTimeFormatter TIME_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    /** 状态流转白名单 */
    private static final Map<ReserveStatus, Set<ReserveStatus>> ALLOWED =
            new EnumMap<>(ReserveStatus.class);
    static {
        ALLOWED.put(ReserveStatus.PENDING, EnumSet.of(ReserveStatus.PASS,
                ReserveStatus.REJECT, ReserveStatus.CANCEL));
        ALLOWED.put(ReserveStatus.PASS, EnumSet.of(ReserveStatus.CANCEL));
        ALLOWED.put(ReserveStatus.REJECT, EnumSet.noneOf(ReserveStatus.class));
        ALLOWED.put(ReserveStatus.CANCEL, EnumSet.noneOf(ReserveStatus.class));
    }


    /** 饿汉单例 */
    private static final ReserveRecordService INSTANCE = new ReserveRecordService();
    public static ReserveRecordService getInstance() {
        return INSTANCE;
    }

    private final ReserveRecordDao reserveDao;
    private final List<ReserveRecord> records;
    private final ResourceService resourceService;

    /** 私有构造：依赖的两个对象都从工厂/单例取，不再由外部传入 */
    private ReserveRecordService() {
        this.reserveDao = DaoFactory.getReserveRecordDao();
        this.resourceService = ResourceService.getInstance();
        this.records = reserveDao.loadAll();
    }


    /* ==================== 状态机核心 ==================== */
    /** 断言：不在白名单内的流转一律拦截 */
    private void assertTransfer(ReserveStatus from, ReserveStatus to) {
        Set<ReserveStatus> targets = ALLOWED.get(from);
        if (targets == null || !targets.contains(to)) {
            throw new IllegalStatusTransferException(
                    "预约状态不允许从【" + from.getDesc() + "】变更为【" + to.getDesc() + "】");
        }
    }
    /** 资源是否可预约：被占用 / 停用直接拦截 */
    public void assertResourceBookable(String resourceId) {
        CampusResource res = resourceService.findById(resourceId)
                .orElseThrow(() -> new ResourceNotFoundException("未找到 ID 为 " + resourceId + " 的资源"));
        if (ResourceStatus.OCCUPIED == res.getStatus()) {
            throw new ResourceOccupiedException("资源【" + res.getName() + "】已被占用，暂不可预约");
        }
        if (ResourceStatus.DISABLED == res.getStatus()) {
            throw new ResourceOccupiedException("资源【" + res.getName() + "】已停用，不可预约");
        }
    }


    /* ==================== 学生端 ==================== */
    public Result<ReserveRecord> submit(String resourceId, LocalDateTime start, LocalDateTime end,
                                        String remark, User student) {
        if (student == null || !"student".equals(student.getRole())) {
            return Result.fail("仅学生可以提交预约申请");
        }
        try {
            assertResourceBookable(resourceId);
        } catch (BusinessException e) {
            return Result.fail(e.getMessage());
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

    /** 取消：只允许 PENDING / PASS，且必须断言状态机 */
    public Result<ReserveRecord> cancel(String recordId, User student) {
         Optional<ReserveRecord> opt = findById(recordId);
            if (!opt.isPresent()) {
            return Result.fail("未找到编号为 " + recordId + " 的预约记录");
        }
        ReserveRecord record = opt.get();
        if (!record.getApplicantUsername().equals(student.getUsername())) {
            return Result.fail("只能取消自己的预约");
        }
        try {
            if (ReserveStatus.REJECT == record.getStatus()) {
                throw new CannotCancelException("该预约已被驳回，不能取消");
            }
            if (ReserveStatus.CANCEL == record.getStatus()) {
                throw new CannotCancelException("该预约已取消，不能重复取消");
            }
            assertTransfer(record.getStatus(), ReserveStatus.CANCEL);
        } catch (BusinessException e) {
            return Result.fail(e.getMessage());
        }
        boolean wasPassed = ReserveStatus.PASS == record.getStatus();
        record.setStatus(ReserveStatus.CANCEL);
        if (wasPassed) {
            resourceService.updateStatus(record.getResourceId(), ResourceStatus.AVAILABLE);
        }
        reserveDao.saveAll(records);
        return Result.success("预约已取消");
    }

    /* ==================== 管理员端 ==================== */
    /** 待审核列表：先到先审 */
    public Result<List<ReserveRecord>> listPending(User admin) {
        if (!isAdmin(admin)) {
            return Result.fail("仅管理员可查看待审核预约");
        }
        List<ReserveRecord> list = records.stream()
                .filter(r -> ReserveStatus.PENDING == r.getStatus())
                .sorted(Comparator.comparing(ReserveRecord::getApplyTime))
                .collect(Collectors.toList());
        return Result.success("待审核预约共 " + list.size() + " 条", list);
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

    /** 审核通过：断言 -> 回填审核时间 -> 资源置占用 */
    public Result<ReserveRecord> auditPass(String recordId, User admin) {
        if (!isAdmin(admin)) {
            throw new NoPermissionToAuditException();
        }
        ReserveRecord record = require(recordId);
        try {
            assertTransfer(record.getStatus(), ReserveStatus.PASS);
        } catch (BusinessException e) {
            return Result.fail(e.getMessage());
        }
        record.setStatus(ReserveStatus.PASS);
        record.setAuditTime(LocalDateTime.now());
        resourceService.updateStatus(record.getResourceId(), ResourceStatus.OCCUPIED);
        reserveDao.saveAll(records);
        return Result.success("审核通过，资源已置为占用", record);
    }
    /** 审核驳回：断言 -> 驳回原因非空 -> 资源不变 */
    public Result<ReserveRecord> auditReject(String recordId, String reason, User admin) {
        if (!isAdmin(admin)) {
            throw new NoPermissionToAuditException();
        }
        if (reason == null || reason.trim().isEmpty()) {
            return Result.fail("驳回原因不能为空");
        }
        ReserveRecord record = require(recordId);
        try {
            assertTransfer(record.getStatus(), ReserveStatus.REJECT);
        } catch (BusinessException e) {
            return Result.fail(e.getMessage());
        }
        record.setStatus(ReserveStatus.REJECT);
        record.setRejectReason(reason.trim());
        record.setAuditTime(LocalDateTime.now());
        reserveDao.saveAll(records);
        return Result.success("已驳回，驳回原因已记录", record);
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

    /* ==================== 统计报表（全部使用 Stream 收集器） ==================== */
    /** 统计1：按资源分组，聚合各种状态的数量与通过率 */
    public Result<List<ResourceStatDTO>> statByResource() {
        Map<String, List<ReserveRecord>> grouped = records.stream()
                .collect(Collectors.groupingBy(ReserveRecord::getResourceId));

        List<ResourceStatDTO> list = grouped.entrySet().stream()
                .map(entry -> {
                    List<ReserveRecord> group = entry.getValue();
                    long total = group.size();
                    long pass = group.stream()
                            .filter(r -> ReserveStatus.PASS == r.getStatus()).count();
                    long reject = group.stream()
                            .filter(r -> ReserveStatus.REJECT == r.getStatus()).count();
                    long cancel = group.stream()
                            .filter(r -> ReserveStatus.CANCEL == r.getStatus()).count();
                    // 除零保护：没有申请记录时通过率给 0
                    double rate = total == 0 ? 0.0 : (double) pass / total;
                    // 资源被删除时用 Optional 兜底，报表不报错
                    String name = resourceService.findById(entry.getKey())
                            .map(CampusResource::getName)
                            .orElse("资源已删除");
                    return new ResourceStatDTO(entry.getKey(), name, total, pass, reject, cancel, rate);
                })
                .sorted(Comparator.comparing(ResourceStatDTO::getResourceId))
                .collect(Collectors.toList());

        return Result.success("资源预约统计共 " + list.size() + " 项", list);
    }

    /** 统计2：热度排行 TOP N，按通过次数降序 + limit 截断 */
    public Result<List<ResourceStatDTO>> topN(int n) {
        int limit = n <= 0 ? 10 : n;
        Result<List<ResourceStatDTO>> all = statByResource();
        if (!all.isSuccess()) {
            return all;
        }
        List<ResourceStatDTO> top = all.getData().stream()
                .sorted(Comparator.comparingLong(ResourceStatDTO::getPassCount).reversed())
                .limit(limit)
                .collect(Collectors.toList());
        return Result.success("热门资源 TOP " + limit, top);
    }

    /** 统计3：按申请人分组，统计每个人的提交 / 通过 / 驳回 */
    public Result<List<UserStatDTO>> statByUser() {
        Map<String, List<ReserveRecord>> byUser = records.stream()
                .collect(Collectors.groupingBy(ReserveRecord::getApplicantUsername));

        List<UserStatDTO> list = byUser.entrySet().stream()
                .map(entry -> {
                    List<ReserveRecord> group = entry.getValue();
                    long total = group.size();
                    long pass = group.stream()
                            .filter(r -> ReserveStatus.PASS == r.getStatus()).count();
                    long reject = group.stream()
                            .filter(r -> ReserveStatus.REJECT == r.getStatus()).count();
                    return new UserStatDTO(entry.getKey(), total, pass, reject);
                })
                .sorted(Comparator.comparingLong(UserStatDTO::getTotalApplyCount).reversed())
                .collect(Collectors.toList());
        return Result.success("用户预约统计共 " + list.size() + " 人", list);
    }

    /** 统计4：全局状态汇总（groupingBy + counting） */
    public Result<Map<ReserveStatus, Long>> statusSummary() {
        Map<ReserveStatus, Long> summary = records.stream()
                .collect(Collectors.groupingBy(ReserveRecord::getStatus, Collectors.counting()));
        for (ReserveStatus s : ReserveStatus.values()) {
            summary.putIfAbsent(s, 0L);        // 没有记录的状态补 0，报表不出现空行
        }
        return Result.success("全局预约状态汇总", summary);
    }


    /* ==================== 工具方法 ==================== */
    private ReserveRecord require(String recordId) {
        return findById(recordId).orElseThrow(
                () -> new BusinessException("未找到编号为 " + recordId + " 的预约记录"));
    }

    private Optional<ReserveRecord> findById(String recordId) {
        return records.stream().filter(r -> r.getRecordId().equals(recordId)).findFirst();
    }
    private boolean isAdmin(User user) {
        return user != null && "admin".equals(user.getRole());
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

    private String nextId() {
        int max = records.stream()
                .mapToInt(r -> Integer.parseInt(r.getRecordId().substring(1)))
                .max()
                .orElse(0);
        return String.format("Y%03d", max + 1);
    }

}
