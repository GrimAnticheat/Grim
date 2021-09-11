package ac.grim.grimac.utils.math;

import ac.grim.grimac.player.GrimPlayer;
import org.bukkit.util.Vector;

public class TrigHandler {
    GrimPlayer player;
    private double buffer = 0;
    private boolean isVanillaMath = true;

    public TrigHandler(GrimPlayer player) {
        this.player = player;
    }

    public void setOffset(Vector oldVel, double offset) {
        // Offset too high, this is an outlier, ignore
        // We are checking in the range of 1e-3 to 5e-5, around what using the wrong trig system results in
        //
        // Ignore if 0 offset
        if (offset == 0 || offset > 1e-3) {
            return;
        }

        boolean flags = player.checkManager.getOffsetHandler().doesOffsetFlag(offset);
        buffer = Math.max(0, buffer);

        // Gliding doesn't allow inputs, so, therefore we must rely on the old type of check for this
        // This isn't too accurate but what choice do I have?
        if (player.isGliding) {
            buffer += flags ? 1 : -0.25;

            if (buffer > 5) {
                buffer = 0;
                isVanillaMath = !isVanillaMath;
            }

            return;
        }

        if (player.checkManager.getOffsetHandler().doesOffsetFlag(offset)) {
            Vector trueMovement = player.actualMovement.clone().subtract(oldVel);
            Vector correctMath = getVanillaMathMovement(trueMovement, 0.1f, player.xRot);
            Vector shitMath = getShitMathMovement(trueMovement, 0.1f, player.xRot);

            correctMath = new Vector(Math.abs(correctMath.getX()), 0, Math.abs(correctMath.getZ()));
            shitMath = new Vector(Math.abs(shitMath.getX()), 0, Math.abs(shitMath.getZ()));

            double minCorrectHorizontal = Math.min(correctMath.getX(), correctMath.getZ());
            // Support diagonal inputs
            minCorrectHorizontal = Math.min(minCorrectHorizontal, Math.abs(correctMath.getX() - correctMath.getZ()));

            double minShitHorizontal = Math.min(shitMath.getX(), shitMath.getZ());
            // Support diagonal inputs
            minShitHorizontal = Math.min(minShitHorizontal, Math.abs(shitMath.getX() - shitMath.getZ()));

            boolean newVanilla = minCorrectHorizontal < minShitHorizontal;

            buffer += newVanilla != this.isVanillaMath ? 1 : -0.25;

            if (buffer > 5) {
                buffer = 0;
                this.isVanillaMath = !this.isVanillaMath;
            }
        }
    }

    public static Vector getVanillaMathMovement(Vector wantedMovement, float f, float f2) {
        float f3 = VanillaMath.sin(f2 * 0.017453292f);
        float f4 = VanillaMath.cos(f2 * 0.017453292f);

        float bestTheoreticalX = (float) (f3 * wantedMovement.getZ() + f4 * wantedMovement.getX()) / (f3 * f3 + f4 * f4) / f;
        float bestTheoreticalZ = (float) (-f3 * wantedMovement.getX() + f4 * wantedMovement.getZ()) / (f3 * f3 + f4 * f4) / f;

        return new Vector(bestTheoreticalX, 0, bestTheoreticalZ);
    }

    public static Vector getShitMathMovement(Vector wantedMovement, float f, float f2) {
        float f3 = OptifineShitMath.sin(f2 * 0.017453292f);
        float f4 = OptifineShitMath.cos(f2 * 0.017453292f);

        float bestTheoreticalX = (float) (f3 * wantedMovement.getZ() + f4 * wantedMovement.getX()) / (f3 * f3 + f4 * f4) / f;
        float bestTheoreticalZ = (float) (-f3 * wantedMovement.getX() + f4 * wantedMovement.getZ()) / (f3 * f3 + f4 * f4) / f;

        return new Vector(bestTheoreticalX, 0, bestTheoreticalZ);
    }

    public float sin(float f) {
        return isVanillaMath ? VanillaMath.sin(f) : OptifineShitMath.sin(f);
    }

    public float cos(float f) {
        return isVanillaMath ? VanillaMath.cos(f) : OptifineShitMath.cos(f);
    }
}
