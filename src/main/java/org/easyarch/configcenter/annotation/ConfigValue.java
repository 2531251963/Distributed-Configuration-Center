package org.easyarch.configcenter.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @ClassName ConfigValue
 * @Description TODO
 * @Author Liyihe
 * @Date 2019/08/30 下午5:19
 * @Version 1.0
 */
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ConfigValue {
    String value() default "";
}
