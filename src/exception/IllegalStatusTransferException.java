package exception;

/** 预约状态非法变更 */
public class IllegalStatusTransferException extends BusinessException {
    public IllegalStatusTransferException(String message) {
        super(message);
    }
}
