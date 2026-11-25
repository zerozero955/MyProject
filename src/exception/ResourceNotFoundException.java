package exception;

import common.BusinessException;

/**资源不存在*/

public class ResourceNotFoundException extends BusinessException {

    public ResourceNotFoundException(){
        super("未找到对应资源，请确认资源编号");
    }

    public ResourceNotFoundException(String message){
        super(message);
    }
}
