package exception;

/** 无权审核：只有管理员可以审核预约 */
public class NoPermissionToAuditException extends BusinessException {
    public NoPermissionToAuditException() {
        super("越权操作：仅管理员可审核预约");
    }
}
