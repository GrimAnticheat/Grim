package ac.grim.grimac.checks.impl.velocity;

/**
 * Decides whether a setback that the player appears not to have taken should be resent.
 *
 * <p>The setback's own velocity packet is recorded as knockback, so every setback reaches
 * this decision once with a small residual offset (the difference between Grim's simulated
 * setback movement and the player's real movement). Gating the resend on the flag threshold
 * (Knockback.threshold, 0.001 default) makes the resend itself produce the next recorded
 * setback velocity, looping until the residual decays below the threshold. The
 * immediate-setback threshold (Knockback.immediate-setback-threshold, 0.1 default, the same
 * value the normal path uses) filters these echoes while still resending velocities the
 * player genuinely ignores. Kept as a standalone class so the decision stays unit-testable;
 * the enclosing check classes pull in the platform-dependent API on class load.</p>
 */
final class SetbackResend {
    private SetbackResend() {
    }

    static boolean shouldResend(double offset, double immediate) {
        return offset >= immediate;
    }
}
