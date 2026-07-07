package ac.grim.grimac.utils.nmsutil;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NetherPortalMovementTest {
    @Test
    void shouldSweepNormalMovementForPortals() {
        assertTrue(NetherPortalMovement.shouldSweep(0.4D, 0.0D, -0.8D));
        assertTrue(NetherPortalMovement.shouldSweep(64.0D, -64.0D, 64.0D));
    }

    @Test
    void shouldNotSweepTeleportSizedMovementForPortals() {
        assertFalse(NetherPortalMovement.shouldSweep(64.0001D, 0.0D, 0.0D));
        assertFalse(NetherPortalMovement.shouldSweep(0.0D, -64.0001D, 0.0D));
        assertFalse(NetherPortalMovement.shouldSweep(0.0D, 0.0D, 64.0001D));
    }

    @Test
    void shouldNotSweepInvalidMovementForPortals() {
        assertFalse(NetherPortalMovement.shouldSweep(Double.NaN, 0.0D, 0.0D));
        assertFalse(NetherPortalMovement.shouldSweep(0.0D, Double.POSITIVE_INFINITY, 0.0D));
        assertFalse(NetherPortalMovement.shouldSweep(0.0D, 0.0D, Double.NEGATIVE_INFINITY));
    }
}
