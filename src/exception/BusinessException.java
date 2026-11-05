package exception;


/**
 * 业务异常父类：继承时运行异常
 * 区分业务异常和系统异常，方便统一处理
 */
public class BusinessException extends RuntimeException {

    public BusinessException(String message) {
        super(message);
    }

    public BusinessException(String message, Throwable cause){
        super(message, cause);
    }
}
