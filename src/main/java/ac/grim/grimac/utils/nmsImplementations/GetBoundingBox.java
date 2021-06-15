package ac.grim.grimac.utils.nmsImplementations;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.collisions.datatypes.SimpleCollisionBox;
import io.github.retrooper.packetevents.utils.player.ClientVersion;
import org.bukkit.entity.AbstractHorse;
import org.bukkit.entity.Pig;
import org.bukkit.entity.Strider;

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

    private static SimpleCollisionBox getBoundingBoxFromPosAndSize(double centerX, double minY, double centerZ, double width, double height) {
        double minX = centerX - (width / 2);
        double maxX = centerX + (width / 2);
        double maxY = minY + height;
        double minZ = centerZ - (width / 2);
        double maxZ = centerZ + (width / 2);

        return new SimpleCollisionBox(minX, minY, minZ, maxX, maxY, maxZ);
    }

    public static SimpleCollisionBox getBoatBoundingBox(double centerX, double minY, double centerZ) {
        double width = 1.375;
        double height = 0.5625;

        return getBoundingBoxFromPosAndSize(centerX, minY, centerZ, width, height);
    }

    public static SimpleCollisionBox getHorseBoundingBox(double centerX, double minY, double centerZ, AbstractHorse horse) {
        double width = horse.getBoundingBox().getMaxX() - horse.getBoundingBox().getMinX();
        double height = horse.getBoundingBox().getMaxY() - horse.getBoundingBox().getMinY();

        return getBoundingBoxFromPosAndSize(centerX, minY, centerZ, width, height);
    }

    public static SimpleCollisionBox getPigBoundingBox(double centerX, double minY, double centerZ, Pig pig) {
        // Only adults can be ridden, but plugin magic can make players ride babies
        double width;
        double height;

        if (pig.isAdult()) {
            width = 0.9;
            height = 0.9;
        } else {
            width = 0.45;
            height = 0.45;
        }

        return getBoundingBoxFromPosAndSize(centerX, minY, centerZ, width, height);
    }

    public static SimpleCollisionBox getStriderBoundingBox(double centerX, double minY, double centerZ, Strider strider) {
        // Only adults can be ridden, but plugin magic can make players ride babies
        double width;
        double height;

        if (strider.isAdult()) {
            width = 0.9;
            height = 1.7;
        } else {
            width = 0.45;
            height = 0.85;
        }

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
