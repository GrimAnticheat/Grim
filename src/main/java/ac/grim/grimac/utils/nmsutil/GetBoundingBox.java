package ac.grim.grimac.utils.nmsutil;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.collisions.datatypes.SimpleCollisionBox;
import ac.grim.grimac.utils.data.packetentity.PacketEntity;

public class GetBoundingBox {
    public static SimpleCollisionBox getCollisionBoxForPlayer(GrimPlayer player, double centerX, double centerY, double centerZ) {
        if (player.compensatedEntities.getSelf().getRiding() != null) {
            return getPacketEntityBoundingBox(player, centerX, centerY, centerZ, player.compensatedEntities.getSelf().getRiding());
        }

        return getPlayerBoundingBox(player, centerX, centerY, centerZ);
    }

    public static SimpleCollisionBox getPacketEntityBoundingBox(GrimPlayer player, double centerX, double minY, double centerZ, PacketEntity entity) {
        float width = BoundingBoxSize.getWidth(player, entity);
        float height = BoundingBoxSize.getHeight(player, entity);

        return getBoundingBoxFromPosAndSize(centerX, minY, centerZ, width, height);
    }

    // Size regular: 0.6 width 1.8 height
    // Size shifting on 1.14+ (19w12a): 0.6 width 1.5 height
    // Size while gliding/swimming: 0.6 width 0.6 height
    // Size while sleeping: 0.2 width 0.2 height
    public static SimpleCollisionBox getPlayerBoundingBox(GrimPlayer player, double centerX, double minY, double centerZ) {
        float width = player.pose.width;
        float height = player.pose.height;

        return getBoundingBoxFromPosAndSize(centerX, minY, centerZ, width, height);
    }

    public static SimpleCollisionBox getBoundingBoxFromPosAndSize(double centerX, double minY, double centerZ, float width, float height) {
        double minX = centerX - (width / 2f);
        double maxX = centerX + (width / 2f);
        double maxY = minY + height;
        double minZ = centerZ - (width / 2f);
        double maxZ = centerZ + (width / 2f);

        return new SimpleCollisionBox(minX, minY, minZ, maxX, maxY, maxZ, false);
    }
}
