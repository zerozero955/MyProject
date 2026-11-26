package entity;

import java.time.LocalDateTime;

/**
 * 操作日志实体：一条日志 = 一次被注解方法的执行记录
 */

public class OperationLogRecord {

    private String logId;                 // 日志唯一编号
    private String username;              // 操作人
    private LocalDateTime operateTime;    // 操作时间
    private String methodFullName;        // 类名.方法名
    private String operationDesc;         // 取自注解 value
    private String params;                // 入参序列化字符串
    private String resultInfo;            // 返回 Result 的信息
    private String exceptionMsg;          // 异常信息，无异常为空串

    public OperationLogRecord() {
    }

    public OperationLogRecord(String logId, String username, LocalDateTime operateTime,
        String methodFullName, String operationDesc,
        String params, String resultInfo, String exceptionMsg) {
        this.logId = logId;
        this.username = username;
        this.operateTime = operateTime;
        this.methodFullName = methodFullName;
        this.operationDesc = operationDesc;
        this.params = params;
        this.resultInfo = resultInfo;
        this.exceptionMsg = exceptionMsg;
    }

    public String getLogId() { return logId; }
    public void setLogId(String logId) { this.logId = logId; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public LocalDateTime getOperateTime() { return operateTime; }
    public void setOperateTime(LocalDateTime operateTime) { this.operateTime = operateTime; }

    public String getMethodFullName() { return methodFullName; }
    public void setMethodFullName(String methodFullName) { this.methodFullName = methodFullName; }

    public String getOperationDesc() { return operationDesc; }
    public void setOperationDesc(String operationDesc) { this.operationDesc = operationDesc; }

    public String getParams() { return params; }
    public void setParams(String params) { this.params = params; }

    public String getResultInfo() { return resultInfo; }
    public void setResultInfo(String resultInfo) { this.resultInfo = resultInfo; }

    public String getExceptionMsg() { return exceptionMsg; }
    public void setExceptionMsg(String exceptionMsg) { this.exceptionMsg = exceptionMsg; }

    @Override
    public String toString(){
        return logId + " | " + operateTime + " | " + username + " | "
            + operationDesc + " | " + methodFullName
            + " | 入参：" + params + " | 结果：" + resultInfo
            + (exceptionMsg == null || exceptionMsg.isEmpty() ? "" : " | 异常：" + exceptionMsg);
    }
}
