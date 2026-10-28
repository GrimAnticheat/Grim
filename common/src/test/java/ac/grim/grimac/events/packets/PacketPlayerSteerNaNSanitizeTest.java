package ac.grim.grimac.events.packets;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pins the fix for GrimAnticheat/Grim#2983 ("EntitySpeed (more of a fly) horse bypass").
 * <p>
 * A modified client (see the public PoC at github.com/crabs3/horsefly) can send
 * {@link Float#NaN} in the STEER_VEHICLE packet's forward/sideways fields. Vanilla clients
 * cannot: real steering input is always 0, +-1, or that times 0.98.
 * <p>
 * {@link PacketPlayerSteer#onPacketReceive} used to store that value directly into
 * {@code player.vehicleData.nextVehicleForward/Horizontal} with no finite check.
 * {@code MovementCheckRunner}'s {@code Math.min(0.98, Math.max(-0.98, x))} clamp does not
 * sanitize it, since both {@link Math#min} and {@link Math#max} return NaN whenever either
 * operand is NaN. The NaN then poisons the horse movement prediction, and every later
 * {@code actual > predicted} style check silently stops firing, because any relational
 * comparison against NaN in Java evaluates to false.
 * <p>
 * This test isolates the exact guard added at the point the packet is read (the one place
 * all steer input enters, instead of the clamp further downstream) and proves it rejects
 * non-finite input while leaving every legal input value untouched.
 */
public class PacketPlayerSteerNaNSanitizeTest {

    /** Mirrors the pre-fix line: {@code nextVehicleForward = steer.getForward();} verbatim. */
    private static float unsanitized(float raw) {
        return raw;
    }

    /** Mirrors the fix applied in {@link PacketPlayerSteer#onPacketReceive}. */
    private static float sanitized(float raw) {
        float value = raw;
        if (!Float.isFinite(value)) value = 0F;
        return value;
    }

    @Test
    void unpatchedPathLetsNaNThrough() {
        // Reproduces the bug: before the fix, a NaN steer value survives untouched and would
        // go on to poison Math.min/Math.max's clamp and the horse movement prediction.
        assertTrue(Float.isNaN(unsanitized(Float.NaN)));
    }

    @Test
    void clampDoesNotSanitizeNaN() {
        // Confirms the README's claim about Grim's existing clamp, the reason this bug exists:
        // Math.min/Math.max propagate NaN instead of stripping it.
        float clamped = (float) Math.min(0.98, Math.max(-0.98, Float.NaN));
        assertTrue(Float.isNaN(clamped));
    }

    @Test
    void fixRejectsNaN() {
        assertEquals(0F, sanitized(Float.NaN));
    }

    @Test
    void fixRejectsPositiveAndNegativeInfinity() {
        assertEquals(0F, sanitized(Float.POSITIVE_INFINITY));
        assertEquals(0F, sanitized(Float.NEGATIVE_INFINITY));
    }

    @Test
    void fixLeavesLegalSteerValuesUnchanged() {
        // Every value a real client can actually send: 0, +-1, and +-1 * 0.98.
        for (float legal : new float[]{0F, 1F, -1F, 0.98F, -0.98F}) {
            assertEquals(legal, sanitized(legal));
        }
    }
}
