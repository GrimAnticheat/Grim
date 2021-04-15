package ac.grim.grimac.utils.nmsImplementations;

import net.minecraft.server.v1_16_R3.AxisAlignedBB;

public class GetBoundingBox {
    // Size regular: 0.6 width 1.8 height
    // Size shifting on 1.14+ (19w12a): 0.6 width 1.5 height
    // Size while gliding/swimming: 0.6 width 0.6 height
    // Size while sleeping: 0.2 width 0.2 height
    public static AxisAlignedBB getPlayerBoundingBox(double centerX, double minY, double centerZ, boolean isShifting, boolean isGliding, boolean isSwimming, boolean isSleeping, short clientVersion) {
        double playerHeight;
        double playerWidth = 0.6;

        if (isGliding || isSwimming) {
            playerHeight = 0.6;
        } else if (isSleeping) {
            playerHeight = 0.2;
            playerWidth = 0.2;
        } else if (isShifting && clientVersion >= 466) {
            playerHeight = 1.5;
        } else {
            playerHeight = 1.8;
        }

        double minX = centerX - (playerWidth / 2);
        double maxX = centerX + (playerWidth / 2);
        double minZ = centerZ - (playerWidth / 2);
        double maxZ = centerZ + (playerWidth / 2);
        double maxY = minY + playerHeight;

        return new AxisAlignedBB(minX, minY, minZ, maxX, maxY, maxZ);
    }
}
