package entity;

public class ResourceStatDTO {

    private String resourceId;
    private String resourceName;
    private long totalApplyCount;   // 总申请次数（四种状态合计）
    private long passCount;         // 通过次数
    private long rejectCount;       // 驳回次数
    private long cancelCount;       // 取消次数
    private double passRate;        // 通过率 = 通过数 / 总申请数

    public ResourceStatDTO(){}

    public ResourceStatDTO(String resourceId, String resourceName, long totalApplyCount,
                           long passCount, long rejectCount, long cancelCount, double passRate) {
        this.resourceId = resourceId;
        this.resourceName = resourceName;
        this.totalApplyCount = totalApplyCount;
        this.passCount = passCount;
        this.rejectCount = rejectCount;
        this.cancelCount = cancelCount;
        this.passRate = passRate;
    }

    public String getResourceId() {
        return resourceId;
    }
    public void setResourceId(String resourceId) {
        this.resourceId = resourceId;
    }
    public String getResourceName() {
        return resourceName;
    }
    public void setResourceName(String resourceName) {
        this.resourceName = resourceName;
    }
    public long getTotalApplyCount() {
        return totalApplyCount;
    }
    public void setTotalApplyCount(long totalApplyCount) {
        this.totalApplyCount = totalApplyCount;
    }
    public long getPassCount() {
        return passCount;
    }
    public void setPassCount(long passCount) {
        this.passCount = passCount;
    }
    public long getRejectCount() {
        return rejectCount;
    }
    public void setRejectCount(long rejectCount) {
        this.rejectCount = rejectCount;
    }
    public long getCancelCount() {
        return cancelCount;
    }
    public void setCancelCount(long cancelCount) {
        this.cancelCount = cancelCount;
    }
    public double getPassRate() {
        return passRate;
    }
    public void setPassRate(double passRate) {
        this.passRate = passRate;
    }

    @Override
    public String toString(){
        return resourceId + " | " + resourceName + " | 总申请 " + totalApplyCount
                + " | 通过 " + passCount + " | 驳回 " + rejectCount
                + " | 取消 " + cancelCount + " | 通过率 " + String.format("%.1f%%", passRate * 100);
    }
}
