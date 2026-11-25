package exception;

import common.BusinessException;

/** 权限不足：越权操作统一用这个异常 */

public class NoPermissionException extends BusinessException {

    public NoPermissionException(){
        super("权限不足,该权限仅管理员可执行");
    }

    public NoPermissionException(String message){
        super(message);
    }
}
