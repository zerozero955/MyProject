package entity;

import java.time.LocalDateTime;

/**
 * 预约记录实体：只承载数据，状态流转规则在业务方法中实现
 * */

public class ReserveRecord {

    private String recordId;               // 预约记录编号 Y001
    private String resourceId;             // 关联资源 ID
    private String applicantUsername;      // 申请人用户名
    private LocalDateTime applyTime;       // 申请时间
    private LocalDateTime reserveStartTime;// 使用开始时间
    private LocalDateTime reserveEndTime;  // 使用结束时间
    private ReserveStatus status;          // 预约状态
    private String remark;                 // 备注

    public ReserveRecord(){

    }

    public ReserveRecord(String recordId, String resourceId, String applicantUsername,
                        LocalDateTime applyTime, LocalDateTime reserveStartTime,
                        LocalDateTime reserveEndTime, ReserveStatus status, String remark) {
        this.recordId = recordId;
        this.resourceId = resourceId;
        this.applicantUsername = applicantUsername;
        this.applyTime = applyTime;
        this.reserveStartTime = reserveStartTime;
        this.reserveEndTime = reserveEndTime;
        this.status = status;
        this.remark = remark;
    }

    public String getRecordId() {
        return recordId;
    }
    public void setRecordId(String recordId) {
        this.recordId = recordId;
    }
    public String getResourceId() {
        return resourceId;
    }
    public void setResourceId(String resourceId) {
        this.resourceId = resourceId;
    }
    public String getApplicantUsername() {
        return applicantUsername;
    }
    public void setApplicantUsername(String applicantUsername) {
        this.applicantUsername = applicantUsername;
    }
    public LocalDateTime getApplyTime() {
        return applyTime;
    }
    public void setApplyTime(LocalDateTime applyTime) {
        this.applyTime = applyTime;
    }
    public LocalDateTime getReserveStartTime() {
        return reserveStartTime;
    }
    public void setReserveStartTime(LocalDateTime reserveStartTime) {
        this.reserveStartTime = reserveStartTime;
    }
    public LocalDateTime getReserveEndTime() {
        return reserveEndTime;
    }
    public void setReserveEndTime(LocalDateTime reserveEndTime) {
        this.reserveEndTime = reserveEndTime;
    }
    public ReserveStatus getStatus() {
        return status;
    }
    public void setStatus(ReserveStatus status) {
        this.status = status;
    }
    public String getRemark() {
        return remark;
    }
    public void setRemark(String remark) {
        this.remark = remark;
    }

    @Override
    public String toString() {
        return recordId + " | " + resourceId + " | " + applicantUsername
                + " | " + reserveStartTime + " ~ " + reserveEndTime
                + " | " + (status == null ? "" : status.getDesc())
                + (remark == null || remark.isEmpty() ? "" : " | 备注：" + remark);
    }
}
