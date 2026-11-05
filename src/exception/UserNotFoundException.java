package exception;

/** 用户不存在：登录/查询时账号查不到 */
public class UserNotFoundException extends BusinessException {
    public UserNotFoundException() {
        super("该用户不存在，请确认账号后重试");
    }
    public UserNotFoundException(String message) {
        super(message);
    }
}
