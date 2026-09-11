package ac.grim.grimac.checks.impl.velocity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers the setback resend decision used by KnockbackHandler's isSetback branch
 * (issue 2865). When a setback fires, its own velocity packet is recorded back as
 * knockback, so the resend decision runs once per setback with the residual offset
 * between Grim's simulated setback movement and the player's real movement.
 * Resending on tiny residuals produced self-sustaining echo chains: each resend was
 * recorded as another isSetback velocity, and the chain only ended when gravity
 * decayed the residual below the flag threshold.
 */
class SetbackResendTest {

    @Test
    void echoResidualsBelowImmediateThresholdAreNotResent() {
        // Residuals from the echo chain in issue 2865: 0.35703, 0.03684, 0.06456, 0.02000.
        // Every value after the first setback sits well below the 0.1 default threshold.
        assertFalse(SetbackResend.shouldResend(0.03684, 0.1));
        assertFalse(SetbackResend.shouldResend(0.06456, 0.1));
        assertFalse(SetbackResend.shouldResend(0.02000, 0.1));
        assertFalse(SetbackResend.shouldResend(0.0010001, 0.1));
        assertFalse(SetbackResend.shouldResend(0.09999, 0.1));
    }

    @Test
    void genuinelyIgnoredVelocityIsStillResent() {
        // A player who ignores the resent velocity produces a large offset on the next tick.
        assertTrue(SetbackResend.shouldResend(0.1, 0.1));
        assertTrue(SetbackResend.shouldResend(0.35703, 0.1));
        assertTrue(SetbackResend.shouldResend(5.0, 0.1));
        assertTrue(SetbackResend.shouldResend(100.0, 0.1));
    }

    @Test
    void boundaryIsInclusive() {
        // offset == immediate resends (inclusive), matching the normal path's
        // `offset >= immediate` semantics.
        assertTrue(SetbackResend.shouldResend(0.1, 0.1));
        assertFalse(SetbackResend.shouldResend(nextDown(0.1), 0.1));
    }

    @Test
    void customConfiguredThresholdsAreHonored() {
        // onReload allows admins to set any threshold, including 0 to restore
        // old resend behavior, or large values to suppress the resend entirely.
        assertFalse(SetbackResend.shouldResend(0.05, 0.2));
        assertTrue(SetbackResend.shouldResend(0.25, 0.2));
        assertTrue(SetbackResend.shouldResend(0.0, 0.0));
        assertTrue(SetbackResend.shouldResend(0.0, -1.0));
        assertFalse(SetbackResend.shouldResend(1.0e9, Double.MAX_VALUE));
    }

    @Test
    void tinyPositiveResidualsAndZeroDoNotResendByDefault() {
        // Pure float-noise residuals and a perfectly clean teleport (offset 0)
        // must not trigger resends with the default threshold.
        assertFalse(SetbackResend.shouldResend(0.0, 0.1));
        assertFalse(SetbackResend.shouldResend(1.0e-9, 0.1));
        assertFalse(SetbackResend.shouldResend(0.001, 0.1));
    }

    /** Largest double strictly less than value. */
    private static double nextDown(double value) {
        return Double.longBitsToDouble(Double.doubleToLongBits(value) - 1);
    }
}
