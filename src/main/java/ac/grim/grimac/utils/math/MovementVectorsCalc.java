package ac.grim.grimac.utils.math;

import ac.grim.grimac.player.GrimPlayer;
import org.bukkit.util.Vector;

public class MovementVectorsCalc {
    public static Vector getLookAngle(GrimPlayer player) {
        return MovementVectorsCalc.calculateViewVector(player.yRot, player.xRot);
    }

    public static Vector calculateViewVector(float f, float f2) {
        float f3 = f * 0.017453292f;
        float f4 = -f2 * 0.017453292f;
        float f5 = VanillaMath.cos(f4);
        float f6 = VanillaMath.sin(f4);
        float f7 = VanillaMath.cos(f3);
        float f8 = VanillaMath.sin(f3);
        return new Vector(f6 * f7, -f8, f5 * f7);
    }

    // Entity line 1243 - (MCP mappings)
    public static Vector getVectorForRotation(float pitch, float yaw) {
        float f = pitch * ((float) Math.PI / 180F);
        float f1 = -yaw * ((float) Math.PI / 180F);
        float f2 = VanillaMath.cos(f1);
        float f3 = VanillaMath.sin(f1);
        float f4 = VanillaMath.cos(f);
        float f5 = VanillaMath.sin(f);
        return new Vector(f3 * f4, -f5, (double) (f2 * f4));
    }
}
