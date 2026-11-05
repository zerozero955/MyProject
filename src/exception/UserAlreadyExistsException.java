package exception;

/** 用户已存在：注册时用户名重复 */
public class UserAlreadyExistsException extends BusinessException{
    public UserAlreadyExistsException() {
        super("该用户名已被注册，请更换后重试");
    }
    public UserAlreadyExistsException(String message) {
        super(message);
    }

}
