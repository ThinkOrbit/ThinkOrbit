package tw.yukina.thinkorbit.service.event;

import tw.yukina.thinkorbit.service.event.entity.SemanticTier;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface OnEvent {
    String type();
    String source() default "";
    SemanticTier[] tier() default {};
}
