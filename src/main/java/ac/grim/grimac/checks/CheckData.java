package ac.grim.grimac.checks;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface CheckData {
    String name() default "UNKNOWN";

    String configName() default "DEFAULT";

    double buffer() default 5;

    double vlMultiplier() default 0.95;

    long reset() default 9000L;

    long flagCooldown() default Long.MAX_VALUE;

    double setback() default 25;

    boolean enabled() default true;
}
