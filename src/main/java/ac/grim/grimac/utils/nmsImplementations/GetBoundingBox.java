package ac.grim.grimac.utils.nmsImplementations;

public class GetBoundingBox {
    // Size regular: 0.6 width 1.8 height
    // Size shifting on 1.14+ (19w12a): 0.6 width 1.5 height
    // Size while gliding/swimming: 0.6 width 0.6 height
    // Size while sleeping: 0.2 width 0.2 height
    public static ac.grim.grimac.utils.nmsImplementations.tuinityVoxelShapes.AxisAlignedBB getPlayerBoundingBox(double centerX, double minY, double centerZ, boolean isShifting, boolean isGliding, boolean isSwimming, boolean isSleeping, short clientVersion) {
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

        return new ac.grim.grimac.utils.nmsImplementations.tuinityVoxelShapes.AxisAlignedBB(minX, minY, minZ, maxX, maxY, maxZ);
    }

    public static ac.grim.grimac.utils.nmsImplementations.tuinityVoxelShapes.AxisAlignedBB getBoatBoundingBox(double centerX, double minY, double centerZ) {
        double boatWidth = 1.375;
        double boatHeight = 0.5625;

        double minX = centerX - (boatWidth / 2);
        double maxX = centerX + (boatWidth / 2);
        double maxY = minY + boatHeight;
        double minZ = centerZ - (boatWidth / 2);
        double maxZ = centerZ + (boatWidth / 2);

        return new ac.grim.grimac.utils.nmsImplementations.tuinityVoxelShapes.AxisAlignedBB(minX, minY, minZ, maxX, maxY, maxZ);
    }
}
