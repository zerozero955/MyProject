package common.util;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * 通用反射工具：对象转字符串 / 属性拷贝 / 全字段判空
 * 设计：私有构造禁止实例化；所有方法静态；反射失败一律安全降级
 * 约束：不引 BeanUtils、Jackson 等第三方包，纯 JDK
 */
public class ObjectReflectUtil {

    private ObjectReflectUtil(){}

    /** 方法1：遍历所有 getter，拼成"类名{字段=值, ...}"；空对象返回空串 */
    public static String objectToString(Object obj){
        if (obj == null){
            return "";
        }
        Class<?> clazz = obj.getClass();
        StringBuilder sb = new StringBuilder(clazz.getSimpleName()).append("{");
        boolean first = true;
        for (Method m : clazz.getMethods()){
            if (!isGetter(m)){
                continue;
            }
            try {
                Object value = m.invoke(obj);
                if (!first) {
                    sb.append(",");
                }
                sb.append(fieldName(m.getName())).append("=").append(value);
                first = false;
            } catch (Exception e){
                // 单个 getter 抛出异常时跳过，不影响整体字符串
            }
        }
        return sb.append("}").toString();
    }

    /** 方法2：同名同类型字段自动拷贝；字段不存在或类型不兼容就跳过，不抛异常 */
    public static <T> T copyProperties(Object source, Class<T> targetCls) {
        if (source == null) {
            return null;
        }
        try {
            T target = targetCls.getDeclaredConstructor().newInstance();
            for (Method getter : source.getClass().getMethods()) {
                if (!isGetter(getter)) {
                    continue;
                }
                String name = fieldName(getter.getName());
                Class<?> type = getter.getReturnType();
                try {
                    Method setter = targetCls.getMethod("set" + capitalize(name), type);
                    setter.invoke(target, getter.invoke(source));
                } catch (NoSuchMethodException | IllegalAccessException e) {
                    // 目标类没有该字段，或类型不兼容：跳过，不报错
                }
            }
            return target;
        } catch (Exception e) {
            return null;                       // 构造失败等异常统一降级
        }
    }

    /** 方法3：所有非静态字段是否全为 null */
    public static boolean isAllFieldNull(Object obj) {
        if (obj == null) {
            return true;
        }
        for (Field f : obj.getClass().getDeclaredFields()) {
            if (Modifier.isStatic(f.getModifiers())) {
                continue;
            }
            f.setAccessible(true);
            try {
                if (f.get(obj) != null) {
                    return false;
                }
            } catch (IllegalAccessException e) {
                return false;                  // 读不出来时按"非空"处理，避免误判
            }
        }
        return true;
    }

    /* ==================== 内部工具 ==================== */

    private static boolean isGetter(Method m) {
        if (m.getParameterTypes().length != 0) {
            return false;
        }
        if (void.class.equals(m.getReturnType())) {
            return false;
        }
        String n = m.getName();
        return (n.startsWith("get") && n.length() > 3 && !"getClass".equals(n))
                || (n.startsWith("is") && n.length() > 2);
    }

    private static String fieldName(String methodName) {
        String n = methodName.startsWith("get") ? methodName.substring(3) : methodName.substring(2);
        return Character.toLowerCase(n.charAt(0)) + n.substring(1);
    }

    private static String capitalize(String s) {
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

}
