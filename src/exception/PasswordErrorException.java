package exception;

/** 密码错误 */
public class PasswordErrorException extends BusinessException {
    public PasswordErrorException() {
        super("密码错误，请重新输入");
    }
    public PasswordErrorException(String message) {
        super(message);
    }
}

