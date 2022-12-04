package ac.grim.grimac.utils.math;

import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import lombok.Getter;
import org.bukkit.util.Vector;

public class TrigHandler {
    GrimPlayer player;
    private double buffer = 0;
    @Getter
    private boolean isVanillaMath = true;

    public TrigHandler(GrimPlayer player) {
        this.player = player;
    }

    public void toggleShitMath() {
        isVanillaMath = !isVanillaMath;
    }

    public Vector getVanillaMathMovement(Vector wantedMovement, float f, float f2) {
        float f3 = VanillaMath.sin(f2 * 0.017453292f);
        float f4 = VanillaMath.cos(f2 * 0.017453292f);

        float bestTheoreticalX = (float) (f3 * wantedMovement.getZ() + f4 * wantedMovement.getX()) / (f3 * f3 + f4 * f4) / f;
        float bestTheoreticalZ = (float) (-f3 * wantedMovement.getX() + f4 * wantedMovement.getZ()) / (f3 * f3 + f4 * f4) / f;

        return new Vector(bestTheoreticalX, 0, bestTheoreticalZ);
    }

    public Vector getShitMathMovement(Vector wantedMovement, float f, float f2) {
        float f3 = player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_8) ? OptifineFastMath.sin(f2 * 0.017453292f) : LegacyFastMath.sin(f2 * 0.017453292f);
        float f4 = player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_8) ? OptifineFastMath.cos(f2 * 0.017453292f) : LegacyFastMath.cos(f2 * 0.017453292f);

        float bestTheoreticalX = (float) (f3 * wantedMovement.getZ() + f4 * wantedMovement.getX()) / (f3 * f3 + f4 * f4) / f;
        float bestTheoreticalZ = (float) (-f3 * wantedMovement.getX() + f4 * wantedMovement.getZ()) / (f3 * f3 + f4 * f4) / f;

        return new Vector(bestTheoreticalX, 0, bestTheoreticalZ);
    }

    public void setOffset(double offset) {
        // Offset too high, this is an outlier, ignore
        // We are checking in the range of 1e-3 to 5e-5, around what using the wrong trig system results in
        //
        // Ignore if 0 offset
        if (offset == 0 || offset > 1e-3) {
            return;
        }

        if (offset > 1e-5) {
            Vector trueMovement = player.actualMovement.clone().subtract(player.startTickClientVel);
            Vector correctMath = getVanillaMathMovement(trueMovement, 0.1f, player.xRot);
            Vector fastMath = getShitMathMovement(trueMovement, 0.1f, player.xRot);

            correctMath = new Vector(Math.abs(correctMath.getX()), 0, Math.abs(correctMath.getZ()));
            fastMath = new Vector(Math.abs(fastMath.getX()), 0, Math.abs(fastMath.getZ()));

            double minCorrectHorizontal = Math.min(correctMath.getX(), correctMath.getZ());
            // Support diagonal inputs
            minCorrectHorizontal = Math.min(minCorrectHorizontal, Math.abs(correctMath.getX() - correctMath.getZ()));

            double minFastMathHorizontal = Math.min(fastMath.getX(), fastMath.getZ());
            // Support diagonal inputs
            minFastMathHorizontal = Math.min(minFastMathHorizontal, Math.abs(fastMath.getX() - fastMath.getZ()));

            boolean newVanilla = minCorrectHorizontal < minFastMathHorizontal;

            buffer += newVanilla != this.isVanillaMath ? 1 : -0.25;

            if (buffer > 5) {
                buffer = 0;
                this.isVanillaMath = !this.isVanillaMath;
            }

            buffer = Math.max(0, buffer);
        }
    }

    public float sin(float f) {
        return isVanillaMath ? VanillaMath.sin(f) : (player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_8) ? OptifineFastMath.sin(f) : LegacyFastMath.sin(f));
    }

    public float cos(float f) {
        return isVanillaMath ? VanillaMath.cos(f) : (player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_8) ? OptifineFastMath.cos(f) : LegacyFastMath.cos(f));
    }
}
