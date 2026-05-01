package ac.grim.grimac.checks;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface CheckData {
    String name() default "UNKNOWN";

    String alternativeName() default "UNKNOWN";

    String configName() default "DEFAULT";

    String description() default "No description provided";

    /**
     * Canonical cross-version identity. Dot-separated, lower snake-case
     * (e.g. {@code "badpackets.duplicate_slot"}). Empty string means
     * legacy/unpopulated — the runtime will fall back to
     * {@code StableKeyMapping} during rollout and log a warning.
     */
    String stableKey() default "";

    double decay() default 0.05;

    double setback() default 25;

    boolean experimental() default false;

}
