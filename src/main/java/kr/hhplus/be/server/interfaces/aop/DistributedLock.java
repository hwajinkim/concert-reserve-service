package kr.hhplus.be.server.interfaces.aop;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DistributedLock {
    //락 이름
    String key();

    TimeUnit timeunit() default TimeUnit.SECONDS;

    // 락을 얻기 위해 기다릴 수 있는 시간
    long waitTime() default 5L;

    // 락 획득 후 임대할 수 있는 시간
    long leaseTime() default 3L;
}
