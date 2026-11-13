package entity;

import exception.ParamEmptyException;

/**
 * 资源状态枚举：禁止用字符串表示状态
 * */
public enum ResourceStatus {
    AVAILABLE("可预约"),
    OCCUPIED("已预约"),
    DISABLED("停用");

    private final String desc;

    ResourceStatus(String desc){
        this.desc = desc;
    }

    public String getDesc(){
        return desc;
    }

    /**字符串转枚举*/
    public static ResourceStatus of(String text){
        for (ResourceStatus s : values()) {
            if (s.name().equalsIgnoreCase(text)||s.desc.equals(text)) {
                return s;
            }
        }
        throw new ParamEmptyException("资源状态只能是 AVAILABLE / OCCUPIED / DISABLED");
    }
}
