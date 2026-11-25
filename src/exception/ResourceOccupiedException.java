package exception;

import common.BusinessException;

/** 资源被占用 / 已停用，不可预约 */
public class ResourceOccupiedException extends BusinessException {
    public ResourceOccupiedException(String message) {
        super(message);
    }
}
