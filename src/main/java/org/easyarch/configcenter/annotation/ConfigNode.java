package org.easyarch.configcenter.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @ClassName ConfigNode
 * @Description TODO
 * @Author Liyihe
 * @Date 2019/08/30 上午10:40
 * @Version 1.0
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ConfigNode {
    /**
     * 定义注解的一个元素 并给定默认值
     * @return
     */
    String nodename() default "";
}