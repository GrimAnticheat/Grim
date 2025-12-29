package ac.grim.grimac.utils.math;

import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.Contract;

// Optifine fastmath is terrible.
//
// Optifine fastmath sends NaN while using an elytra
// It allows jumps that aren't possible in vanilla
// It changes movement by 0.0001
//
// Link to issue:
// https://github.com/sp614x/optifine/issues/5578
//
// Response by sp614x - Optifine's author:
// "If the anticheat fails due to a position difference of 1e-4m (1mm), then it has some problems.
// It should have a tolerance for player actions that is well above 1mm, probably 10cm or something."
//
// No, if your client is flagging my anticheat for not following vanilla behavior, that is on you!
// My anticheat flagging 1e-4 means it's very good, not that it has issues.
//
// I'd suggest everyone to go use Sodium instead as it's open source, is usually faster, and follows vanilla behavior
//
// I don't care when vanilla does something stupid, but I get angry when a proprietary mod breaks my anticheat
//

// Update a few months later

// WHY DID THEY CHANGE FASTMATH
// This is impossible, and I give up!
//
// Instead of fixing the damn issue of changing vanilla mechanics, the new version patches some
// issues with half angles.  Yes, it was wrong, so they made it more accurate, but this makes our
// job impossible without significant performance degradation and 1e-4 bypasses from switching
// between whichever trig table gives the most advantage.
//
// YOU ARE NOT VANILLA OPTIFINE.  YOU DO NOT CONTROL WHAT VANILLA MOVEMENT IS!
//
// I'm seriously considering allowing a warning for FastMath users that it may lead to false bans
// his arrogance is impossible to patch.
//
@UtilityClass
public class OptifineFastMath {
    private static final float[] SIN = new float[4096];

    static {
        for (int i = 0; i < 4096; i++) {
            SIN[i] = roundToFloat(StrictMath.sin(i * Math.PI * 2d / 4096d));
        }
    }

    @Contract(pure = true)
    public static float sin(float value) {
        return SIN[(int) (value * 651.8986f) & 4095];
    }

    @Contract(pure = true)
    public static float cos(float value) {
        return SIN[(int) (value * 651.8986f + 1024f) & 4095];
    }

    @Contract(pure = true)
    public static float roundToFloat(double value) {
        return (float) ((double) Math.round(value * 1.0E8d) / 1.0E8d);
    }
}
