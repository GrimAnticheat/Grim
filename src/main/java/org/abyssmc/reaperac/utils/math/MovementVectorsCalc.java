package org.abyssmc.reaperac.utils.math;

import net.minecraft.server.v1_16_R3.MathHelper;
import net.minecraft.server.v1_16_R3.Vec3D;
import org.bukkit.util.Vector;

public class MovementVectorsCalc {
    public static Vec3D calculateViewVector(float f, float f2) {
        float f3 = f * 0.017453292f;
        float f4 = -f2 * 0.017453292f;
        float f5 = Mth.cos(f4);
        float f6 = Mth.sin(f4);
        float f7 = Mth.cos(f3);
        float f8 = Mth.sin(f3);
        return new Vec3D(f6 * f7, -f8, f5 * f7);
    }

    // Entity line 1243 - (MCP mappings)
    public static Vector getVectorForRotation(float pitch, float yaw) {
        float f = pitch * ((float) Math.PI / 180F);
        float f1 = -yaw * ((float) Math.PI / 180F);
        float f2 = MathHelper.cos(f1);
        float f3 = MathHelper.sin(f1);
        float f4 = MathHelper.cos(f);
        float f5 = MathHelper.sin(f);
        return new Vector(f3 * f4, -f5, (double) (f2 * f4));
    }

    // Entity line 1046
    public static Vector getInputVector(Vector vec3, float f, float f2) {
        // idk why this is needed, but it was fucking up input for other stuff
        double d = vec3.lengthSquared();
        if (d < 1.0E-7) {
            return new Vector();
        }
        Vector vec32 = (d > 1.0 ? vec3.normalize() : vec3).multiply(f);
        float f3 = Mth.sin(f2 * 0.017453292f);
        float f4 = Mth.cos(f2 * 0.017453292f);
        return new Vector(vec32.getX() * (double) f4 - vec32.getZ() * (double) f3,
                vec32.getY(), vec32.getZ() * (double) f4 + vec32.getX() * (double) f3);
    }
}
