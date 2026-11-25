package enums;

/**
 * 预约状态：PENDING 待审核 / PASS 已通过 / REJECT 已驳回 / CANCEL 已取消
 * */

public enum ReserveStatus {
    PENDING("待审核"),
    PASS("已通过"),
    REJECT("已驳回"),
    CANCEL("已取消");

    private final String desc;
    ReserveStatus(String desc){
        this.desc = desc;
    }
    public String getDesc(){
        return desc;

    }
}
