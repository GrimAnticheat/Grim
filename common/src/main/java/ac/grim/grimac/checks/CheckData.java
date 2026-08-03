package ac.grim.grimac.checks;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface CheckData {
    String DEFAULT_DESCRIPTION = "No description provided";
    double DEFAULT_DECAY = 0.05;
    double DEFAULT_SETBACK = 25;

    String name();

    String alternativeName() default "UNKNOWN";

    String configName() default "DEFAULT";

    String description() default DEFAULT_DESCRIPTION;

    /**
     * Canonical cross-version identity. Dot-separated, lower snake-case
     * (e.g. {@code "badpackets.duplicate_slot"}). Empty string means
     * legacy/unpopulated — the runtime will fall back to
     * {@code StableKeyMapping} during rollout and log a warning.
     */
    String stableKey();

    double decay() default DEFAULT_DECAY;

    double setback() default DEFAULT_SETBACK;

    boolean experimental() default false;

}
