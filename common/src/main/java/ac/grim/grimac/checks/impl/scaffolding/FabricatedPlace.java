package ac.grim.grimac.checks.impl.scaffolding;

import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.BlockPlaceCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.update.BlockPlace;
import ac.grim.grimac.utils.nmsutil.Materials;
import com.github.retrooper.packetevents.protocol.world.states.type.StateTypes;
import com.github.retrooper.packetevents.util.Vector3f;

@CheckData(name = "FabricatedPlace", description = "Sent out of bounds cursor position")
public class FabricatedPlace extends BlockPlaceCheck {

    /**
     * MAX_DOUBLE_ERROR:
     * Represents the maximum possible floating-point arithmetic error that can occur
     * when calculating vectors at the Minecraft World Border (30,000,000 blocks).
     *
     * Math.ulp(30,000,000.0) is approx 3.72E-9.
     * We multiply by 2.0 to account for subtraction compounding logic in raytracing.
     *
     * This constant is safe to use everywhere; 7.450580596923828E-9 is physically indistinguishable from zero.
     */
    private static final double MAX_DOUBLE_ERROR = Math.ulp(30_000_000.0) * 2.0;

    /**
     * FLOAT_STEP_AT_ONE:
     * Represents the resolution of a Float at the value 1.0 (and 1.5).
     * Math.ulp(1.0f) is approx 1.1920929E-7.
     *
     * When checking the upper bound (1.0), we must allow this variance because a Double
     * slightly larger than 1.0 might get cast/snapped to the next available Float step.
     */
    private static final double FLOAT_STEP_AT_ONE = Math.ulp(1.0f);

    public FabricatedPlace(GrimPlayer player) {
        super(player);
    }

    @Override
    public void onBlockPlace(final BlockPlace place) {
        Vector3f cursor = place.cursor;
        if (cursor == null) return;

        // Determine if we allow up to 1.5 (Lecterns, Scaffolding, etc)
        boolean isExtended = Materials.isShapeExceedsCube(place.getPlacedAgainstMaterial())
                || place.getPlacedAgainstMaterial() == StateTypes.LECTERN;

        double maxBound = isExtended ? 1.5 : 1.0;
        double minBound = 1.0 - maxBound; // Usually 0.0

        // ====================================================================================
        // LOWER BOUND CHECK (< 0.0)
        // ====================================================================================
        // Why only MAX_DOUBLE_ERROR?
        // Near 0.0, 'float' has extremely high resolution (down to E-45).
        // It acts like a double. When the client has a tiny calculation error (e.g. -4.44E-16),
        // the float cast preserves it exactly. We only need to account for the arithmetic noise.
        if (cursor.getX() < minBound - MAX_DOUBLE_ERROR ||
                cursor.getY() < minBound - MAX_DOUBLE_ERROR ||
                cursor.getZ() < minBound - MAX_DOUBLE_ERROR) {

            // Alert logic
            String debug = String.format("cursor=%s limit=%.16f", cursor, minBound - MAX_DOUBLE_ERROR);
            if (flagAndAlert(debug) && shouldModifyPackets() && shouldCancel()) {
                place.resync();
            }
            return;
        }

        // ====================================================================================
        // UPPER BOUND CHECK (> 1.0 or > 1.5)
        // ====================================================================================
        // Near 1.0, the "Float Step" is E-7. This is 16x larger than the Double error.
        // If the calculation is slightly off, it might snap to the NEXT float.
        // We tolerate exactly one "Float Step" of overflow.
        double upperTolerance = FLOAT_STEP_AT_ONE;

        // Note: Effectively Math.max(MAX_DOUBLE_ERROR, FLOAT_STEP_AT_ONE)
        // but since FLOAT_STEP is always larger in MC coordinates, we just use it.

        if (cursor.getX() > maxBound + upperTolerance ||
                cursor.getY() > maxBound + upperTolerance ||
                cursor.getZ() > maxBound + upperTolerance) {

            // Alert logic
            String debug = String.format("cursor=%s limit=%.16f", cursor, maxBound + upperTolerance);
            if (flagAndAlert(debug) && shouldModifyPackets() && shouldCancel()) {
                place.resync();
            }
        }
    }
}
