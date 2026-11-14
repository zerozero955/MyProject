package common;


/**
 * 统一返回结果，只负责封装，不含业务逻辑与IO
 * @param <T>业务数据类型
 */
public class Result<T> {

    private boolean success;
    private String msg;
    private T data;

    public Result(){
    }

    public Result(boolean success, String msg, T data){
        this.success = success;
        this.msg = msg;
        this.data = data;
    }
    /** 成功，只带数据（提示信息默认"操作成功"） */
    public static <T> Result<T> success(T data) {
        return new Result<>(true, "操作成功", data);
    }
    /** 成功，只带提示信息 */
    public static <T> Result<T> success(String msg) {
        return new Result<>(true, msg, null);
    }
    /** 成功，带提示信息与数据 */
    public static <T> Result<T> success(String msg, T data) {
        return new Result<>(true, msg, data);
    }
    /** 失败，只带错误提示，data 一定为 null */
    public static <T> Result<T> fail(String msg) {
        return new Result<>(false, msg, null);
    }
    public boolean isSuccess(){
        return success;
    }
    public void setSuccess(){
        this.success = success;
    }
    public String getMsg(){
        return msg;
    }
    public void setMsg(String msg){
        this.msg = msg;
    }
    public T getData(){
        return data;
    }
    public void setData(T data){
        this.data = data;
    }

    @Override
    public String toString(){
        return "Result{success=" + success + ", msg='" + msg + "', data=" + data + "}";
    }
}
