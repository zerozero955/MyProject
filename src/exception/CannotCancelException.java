package exception;

import common.BusinessException;

/** 不可取消：状态不允许取消时抛出 */
public class CannotCancelException extends BusinessException {
    public CannotCancelException(String message) {
        super(message);
    }
}
