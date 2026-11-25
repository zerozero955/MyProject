package entity;

public class UserStatDTO {
    private String username;
    private long totalApplyCount;
    private long passCount;
    private long rejectCount;

    public UserStatDTO() {
    }

    public UserStatDTO(String username, long totalApplyCount, long passCount, long rejectCount) {
        this.username = username;
        this.totalApplyCount = totalApplyCount;
        this.passCount = passCount;
        this.rejectCount = rejectCount;
    }

    public String getUsername() {
        return username;
    }
    public void setUsername(String username) {
        this.username = username;
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

    @Override
    public String toString(){
        return username + " | 提交 " + totalApplyCount
                + " | 通过 " + passCount + " | 驳回 " + rejectCount;
    }
}
