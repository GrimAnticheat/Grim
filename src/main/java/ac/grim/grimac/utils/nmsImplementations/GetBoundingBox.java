package ac.grim.grimac.utils.nmsImplementations;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.collisions.datatypes.SimpleCollisionBox;
import ac.grim.grimac.utils.data.packetentity.PacketEntity;
import io.github.retrooper.packetevents.utils.player.ClientVersion;

public class GetBoundingBox {
    // Size regular: 0.6 width 1.8 height
    // Size shifting on 1.14+ (19w12a): 0.6 width 1.5 height
    // Size while gliding/swimming: 0.6 width 0.6 height
    // Size while sleeping: 0.2 width 0.2 height
    public static SimpleCollisionBox getPlayerBoundingBox(GrimPlayer player, double centerX, double minY, double centerZ) {
        double width = player.pose.width;
        double height = player.pose.height;

        return getBoundingBoxFromPosAndSize(centerX, minY, centerZ, width, height);
    }

    public static SimpleCollisionBox getCollisionBoxForPlayer(GrimPlayer player, double centerX, double centerY, double centerZ) {
        if (player.playerVehicle != null) {
            return getPacketEntityBoundingBox(centerX, centerY, centerZ, player.playerVehicle);
        }

        return getPlayerBoundingBox(player, centerX, centerY, centerZ);
    }

    public static SimpleCollisionBox getBoundingBoxFromPosAndSize(double centerX, double minY, double centerZ, double width, double height) {
        double minX = centerX - (width / 2);
        double maxX = centerX + (width / 2);
        double maxY = minY + height;
        double minZ = centerZ - (width / 2);
        double maxZ = centerZ + (width / 2);

        return new SimpleCollisionBox(minX, minY, minZ, maxX, maxY, maxZ, false);
    }

    public static SimpleCollisionBox getPacketEntityBoundingBox(double centerX, double minY, double centerZ, PacketEntity entity) {
        double width = BoundingBoxSize.getWidth(entity);
        double height = BoundingBoxSize.getHeight(entity);

        return getBoundingBoxFromPosAndSize(centerX, minY, centerZ, width, height);
    }

    public static SimpleCollisionBox getBoatBoundingBox(double centerX, double minY, double centerZ) {
        double width = 1.375;
        double height = 0.5625;

        return getBoundingBoxFromPosAndSize(centerX, minY, centerZ, width, height);
    }

    // TODO: This should probably just be done in the player's pose
    public static double getEyeHeight(boolean isShifting, boolean isGliding, boolean isSwimming, boolean isSleeping, ClientVersion clientVersion) {
        if (isGliding || isSwimming) {
            return 0.4;
        } else if (isSleeping) {
            // I'm not sure if this is correct.  I'm guessing based on some code.  It doesn't matter.
            return 0.17;
        } else if (isShifting && clientVersion.isNewerThanOrEquals(ClientVersion.v_1_14)) {
            return 1.27;
        } else if (isShifting) {
            return 1.54;
        } else {
            return 1.62;
        }
    }
}
