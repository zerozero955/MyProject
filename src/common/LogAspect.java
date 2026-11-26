package common;

import common.annotation.OperationLog;
import dao.OperationLogDao;
import entity.OperationLogRecord;
import common.util.ObjectReflectUtil;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.LocalDateTime;

/**
 * 简易日志切面（纯 JDK 反射，禁止引入 Spring / AspectJ）
 * 职责：拦截被 @OperationLog 标注的方法，记录操作人、入参、结果、异常
 * 约束：不修改业务方法的返回值；日志写失败不影响业务
 */
public class LogAspect {
    /**
     * @param target     业务 Service 实例（单例）
     * @param methodName 方法名
     * @param paramTypes 参数类型数组（用于定位重载方法）
     * @param args       实参
     * @param username   当前登录用户
     */
    public static Result<?> invoke(Object target, String methodName, Class<?>[] paramTypes,
                                   Object[] args, String username) {
        Method method;
        try {
            method = target.getClass().getMethod(methodName, paramTypes);
        } catch (NoSuchMethodException e) {
            return Result.fail("系统内部错误：方法不存在");
        }

        OperationLog annotation = method.getAnnotation(OperationLog.class);
        Result<?> result;
        String exceptionMsg = "";

        try {
            result = (Result<?>) method.invoke(target, args);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getTargetException();
            String msg = cause.getMessage();
            exceptionMsg = (msg == null) ? cause.getClass().getSimpleName() : msg;
            result = (cause instanceof BusinessException)
                    ? Result.fail(msg)
                    : Result.fail("系统繁忙，请稍后再试");
        } catch (IllegalAccessException e) {
            return Result.fail("系统内部错误：无权访问该方法");
        }

        // 没加注解的方法：直接返回业务结果，不记日志
        if (annotation == null) {
            return result;
        }

        OperationLogRecord log = new OperationLogRecord();
        log.setLogId(nextId());
        log.setUsername(username == null ? "未登录" : username);
        log.setOperateTime(LocalDateTime.now());
        log.setMethodFullName(method.getDeclaringClass().getSimpleName() + "." + method.getName());
        log.setOperationDesc(annotation.value());
        log.setParams(serializeArgs(args));;
        log.setResultInfo(result.isSuccess()
                ? "成功：" + result.getMsg() + "，数据："
                        + ObjectReflectUtil.objectToString(result.getData())
                : "失败：" + result.getMsg());
        log.setExceptionMsg(exceptionMsg);

        OperationLogDao.getInstance().append(log);
        return result;

    }

    /** 入参序列化：每个参数都走反射工具，输出形如 User{username=zs, role=student} */
    private static String serializeArgs(Object[] args) {
        if (args == null || args.length == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (Object arg : args) {
            sb.append(arg == null ? "null" : arg.toString()).append("; ");
        }
        return sb.toString();
    }

    private static String nextId() {
        return "L" + System.currentTimeMillis();
    }

}