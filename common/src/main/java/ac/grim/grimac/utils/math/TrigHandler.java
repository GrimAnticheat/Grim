package ac.grim.grimac.utils.math;

import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import lombok.Getter;

public class TrigHandler {
    private final GrimPlayer player;
    private double buffer = 0;
    @Getter
    private boolean isVanillaMath = true;

    public TrigHandler(GrimPlayer player) {
        this.player = player;
    }

    public void toggleShitMath() {
        isVanillaMath = !isVanillaMath;
    }

    public Vector3dm getVanillaMathMovement(Vector3dm wantedMovement, float f, float f2) {
        float f3 = VanillaMath.sin(GrimMath.radians(f2));
        float f4 = VanillaMath.cos(GrimMath.radians(f2));

        float bestTheoreticalX = (float) (f3 * wantedMovement.getZ() + f4 * wantedMovement.getX()) / (f3 * f3 + f4 * f4) / f;
        float bestTheoreticalZ = (float) (-f3 * wantedMovement.getX() + f4 * wantedMovement.getZ()) / (f3 * f3 + f4 * f4) / f;

        return new Vector3dm(bestTheoreticalX, 0, bestTheoreticalZ);
    }

    public Vector3dm getShitMathMovement(Vector3dm wantedMovement, float f, float f2) {
        float f3 = player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_8) ? OptifineFastMath.sin(GrimMath.radians(f2)) : LegacyFastMath.sin(GrimMath.radians(f2));
        float f4 = player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_8) ? OptifineFastMath.cos(GrimMath.radians(f2)) : LegacyFastMath.cos(GrimMath.radians(f2));

        float bestTheoreticalX = (float) (f3 * wantedMovement.getZ() + f4 * wantedMovement.getX()) / (f3 * f3 + f4 * f4) / f;
        float bestTheoreticalZ = (float) (-f3 * wantedMovement.getX() + f4 * wantedMovement.getZ()) / (f3 * f3 + f4 * f4) / f;

        return new Vector3dm(bestTheoreticalX, 0, bestTheoreticalZ);
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
            Vector3dm trueMovement = player.actualMovement.clone().subtract(player.startTickClientVel);
            Vector3dm correctMath = getVanillaMathMovement(trueMovement, 0.1f, player.yaw);
            Vector3dm fastMath = getShitMathMovement(trueMovement, 0.1f, player.yaw);

            correctMath = new Vector3dm(Math.abs(correctMath.getX()), 0, Math.abs(correctMath.getZ()));
            fastMath = new Vector3dm(Math.abs(fastMath.getX()), 0, Math.abs(fastMath.getZ()));

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

    public float sin(float value) {
        return isVanillaMath ? VanillaMath.sin(value) : (player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_8) ? OptifineFastMath.sin(value) : LegacyFastMath.sin(value));
    }

    public float cos(float value) {
        return isVanillaMath ? VanillaMath.cos(value) : (player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_8) ? OptifineFastMath.cos(value) : LegacyFastMath.cos(value));
    }
}
