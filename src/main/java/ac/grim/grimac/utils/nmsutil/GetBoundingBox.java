package ac.grim.grimac.utils.nmsutil;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.collisions.datatypes.SimpleCollisionBox;
import ac.grim.grimac.utils.data.packetentity.PacketEntity;
import com.github.retrooper.packetevents.protocol.attribute.Attributes;

public class GetBoundingBox {
    public static SimpleCollisionBox getCollisionBoxForPlayer(GrimPlayer player, double centerX, double centerY, double centerZ) {
        if (player.inVehicle()) {
            return getPacketEntityBoundingBox(player, centerX, centerY, centerZ, player.compensatedEntities.self.getRiding());
        }

        return getPlayerBoundingBox(player, centerX, centerY, centerZ);
    }

    public static SimpleCollisionBox getPacketEntityBoundingBox(GrimPlayer player, double centerX, double minY, double centerZ, PacketEntity entity) {
        float width = BoundingBoxSize.getWidth(player, entity);
        float height = BoundingBoxSize.getHeight(player, entity);
        return getBoundingBoxFromPosAndSize(entity, centerX, minY, centerZ, width, height);
    }

    // Size regular: 0.6 width 1.8 height
    // Size shifting on 1.14+ (19w12a): 0.6 width 1.5 height
    // Size while gliding/swimming: 0.6 width 0.6 height
    // Size while sleeping: 0.2 width 0.2 height
    public static SimpleCollisionBox getPlayerBoundingBox(GrimPlayer player, double centerX, double minY, double centerZ) {
        float width = player.pose.width;
        float height = player.pose.height;
        return getBoundingBoxFromPosAndSize(player, centerX, minY, centerZ, width, height);
    }

    public static SimpleCollisionBox getBoundingBoxFromPosAndSize(GrimPlayer player, double centerX, double minY, double centerZ, float width, float height) {
        return getBoundingBoxFromPosAndSize(player.compensatedEntities.self, centerX, minY, centerZ, width, height);
    }

    public static SimpleCollisionBox getBoundingBoxFromPosAndSize(PacketEntity entity, double centerX, double minY, double centerZ, float width, float height) {
        final float scale = (float) entity.getAttributeValue(Attributes.SCALE);
        return getBoundingBoxFromPosAndSizeRaw(centerX, minY, centerZ, width * scale, height * scale);
    }

    public static SimpleCollisionBox getBoundingBoxFromPosAndSizeRaw(double centerX, double minY, double centerZ, float width, float height) {
        double minX = centerX - (width / 2f);
        double maxX = centerX + (width / 2f);
        double maxY = minY + height;
        double minZ = centerZ - (width / 2f);
        double maxZ = centerZ + (width / 2f);

        return new SimpleCollisionBox(minX, minY, minZ, maxX, maxY, maxZ, false);
    }

    public static double[] getEntityDimensions(GrimPlayer player, PacketEntity entity) {
        final float scale = (float) entity.getAttributeValue(Attributes.SCALE);
        final float width = BoundingBoxSize.getWidth(player, entity) * scale;
        final float height = BoundingBoxSize.getHeight(player, entity) * scale;
        return new double[] { width, height, width};
    }

    public static void expandBoundingBoxByEntityDimensions(SimpleCollisionBox box, GrimPlayer player, PacketEntity entity) {
        double[] dimensions = getEntityDimensions(player, entity);
        double halfWidth = dimensions[0] / 2.0;
        double height = dimensions[1];
        double halfDepth = dimensions[2] / 2.0;

        box.minX -= halfWidth;
        box.minY -= 0; // No downward expansion
        box.minZ -= halfDepth;
        box.maxX += halfWidth;
        box.maxY += height;
        box.maxZ += halfDepth;
    }
}
