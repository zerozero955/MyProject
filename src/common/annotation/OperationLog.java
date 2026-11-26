package common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 操作日志注解：标在需要记录日志的业务方法上
 * RUNTIME 保留策略是反射能读到注解的前提；不引 Spring / AspectJ，纯 JDK 实现
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface OperationLog {

    /** 操作描述，如"提交预约申请""审核预约通过" */
    String value();
}

