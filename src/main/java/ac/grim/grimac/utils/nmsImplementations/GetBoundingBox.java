package ac.grim.grimac.utils.nmsImplementations;

import ac.grim.grimac.GrimPlayer;
import ac.grim.grimac.utils.collisions.types.SimpleCollisionBox;

public class GetBoundingBox {
    // Size regular: 0.6 width 1.8 height
    // Size shifting on 1.14+ (19w12a): 0.6 width 1.5 height
    // Size while gliding/swimming: 0.6 width 0.6 height
    // Size while sleeping: 0.2 width 0.2 height
    public static SimpleCollisionBox getPlayerBoundingBox(GrimPlayer grimPlayer, double centerX, double minY, double centerZ) {
        double playerHeight = grimPlayer.pose.height;
        double playerWidth = grimPlayer.pose.width;

        double minX = centerX - (playerWidth / 2);
        double maxX = centerX + (playerWidth / 2);
        double minZ = centerZ - (playerWidth / 2);
        double maxZ = centerZ + (playerWidth / 2);
        double maxY = minY + playerHeight;

        return new SimpleCollisionBox(minX, minY, minZ, maxX, maxY, maxZ);
    }

    public static SimpleCollisionBox getBoatBoundingBox(double centerX, double minY, double centerZ) {
        double boatWidth = 1.375;
        double boatHeight = 0.5625;

        double minX = centerX - (boatWidth / 2);
        double maxX = centerX + (boatWidth / 2);
        double maxY = minY + boatHeight;
        double minZ = centerZ - (boatWidth / 2);
        double maxZ = centerZ + (boatWidth / 2);

        return new SimpleCollisionBox(minX, minY, minZ, maxX, maxY, maxZ);
    }

    public static double getEyeHeight(boolean isShifting, boolean isGliding, boolean isSwimming, boolean isSleeping, short clientVersion) {
        if (isGliding || isSwimming) {
            return 0.4;
        } else if (isSleeping) {
            // I'm not sure if this is correct.  I'm guessing based on some code.  It doesn't matter.
            return 0.17;
        } else if (isShifting && clientVersion >= 466) {
            return 1.27;
        } else if (isShifting) {
            return 1.54;
        } else {
            return 1.62;
        }
    }
}
