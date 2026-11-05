package exception;

/** 参数为空 / 不合法 */
public class ParamEmptyException extends BusinessException {
    public ParamEmptyException() {
        super("必填参数不能为空");
    }
    public ParamEmptyException(String message) {
        super(message);
    }
}
