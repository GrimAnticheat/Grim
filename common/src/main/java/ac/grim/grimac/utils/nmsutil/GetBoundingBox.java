package ac.grim.grimac.utils.nmsutil;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.collisions.datatypes.SimpleCollisionBox;
import ac.grim.grimac.utils.data.packetentity.PacketEntity;
import com.github.retrooper.packetevents.protocol.attribute.Attributes;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

@UtilityClass
public class GetBoundingBox {
    public static SimpleCollisionBox getCollisionBoxForPlayer(@NotNull GrimPlayer player, double centerX, double centerY, double centerZ) {
        if (player.inVehicle()) {
            return getPacketEntityBoundingBox(player, centerX, centerY, centerZ, player.compensatedEntities.self.getRiding());
        }

        return getPlayerBoundingBox(player, centerX, centerY, centerZ);
    }

    public static @NotNull SimpleCollisionBox getPacketEntityBoundingBox(GrimPlayer player, double centerX, double minY, double centerZ, PacketEntity entity) {
        float width = BoundingBoxSize.getWidth(player, entity);
        float height = BoundingBoxSize.getHeight(player, entity);
        return getBoundingBoxFromPosAndSize(entity, centerX, minY, centerZ, width, height);
    }

    // Size regular: 0.6 width 1.8 height
    // Size shifting on 1.14+ (19w12a): 0.6 width 1.5 height
    // Size while gliding/swimming: 0.6 width 0.6 height
    // Size while sleeping: 0.2 width 0.2 height
    public static @NotNull SimpleCollisionBox getPlayerBoundingBox(@NotNull GrimPlayer player, double centerX, double minY, double centerZ) {
        float width = player.pose.width;
        float height = player.pose.height;
        return getBoundingBoxFromPosAndSize(player, centerX, minY, centerZ, width, height);
    }

    public static @NotNull SimpleCollisionBox getBoundingBoxFromPosAndSize(@NotNull GrimPlayer player, double centerX, double minY, double centerZ, float width, float height) {
        return getBoundingBoxFromPosAndSize(player.compensatedEntities.self, centerX, minY, centerZ, width, height);
    }

    public static @NotNull SimpleCollisionBox getBoundingBoxFromPosAndSize(@NotNull PacketEntity entity, double centerX, double minY, double centerZ, float width, float height) {
        final float scale = (float) entity.getAttributeValue(Attributes.SCALE);
        return getBoundingBoxFromPosAndSizeRaw(centerX, minY, centerZ, width * scale, height * scale);
    }

    @Contract("_, _, _, _, _ -> new")
    public static @NotNull SimpleCollisionBox getBoundingBoxFromPosAndSizeRaw(double centerX, double minY, double centerZ, float width, float height) {
        double minX = centerX - (width / 2f);
        double maxX = centerX + (width / 2f);
        double maxY = minY + height;
        double minZ = centerZ - (width / 2f);
        double maxZ = centerZ + (width / 2f);

        // it's possible for width and/or height to be negative,
        // correct the order of min and max
        return new SimpleCollisionBox(
                Math.min(minX, maxX),
                Math.min(minY, maxY),
                Math.min(minZ, maxZ),
                Math.max(minX, maxX),
                Math.max(minY, maxY),
                Math.max(minZ, maxZ),
                false
        );
    }

    public static double @NotNull [] getEntityDimensions(GrimPlayer player, @NotNull PacketEntity entity) {
        final float scale = (float) entity.getAttributeValue(Attributes.SCALE);
        final float width = BoundingBoxSize.getWidth(player, entity) * scale;
        final float height = BoundingBoxSize.getHeight(player, entity) * scale;
        return new double[] { width, height, width };
    }

    public static void expandBoundingBoxByEntityDimensions(@NotNull SimpleCollisionBox box, GrimPlayer player, PacketEntity entity) {
        double[] dimensions = getEntityDimensions(player, entity);
        double halfWidth = dimensions[0] / 2.0;
        double height = dimensions[1];
        double halfDepth = dimensions[2] / 2.0;

        double minX = box.minX - halfWidth;
        double minY = box.minY; // No downward expansion
        double minZ = box.minZ - halfDepth;
        double maxX = box.maxX + halfWidth;
        double maxY = box.maxY + height;
        double maxZ = box.maxZ + halfDepth;

        // it's possible for width and/or height to be negative,
        // correct the order of min and max
        box.minX = Math.min(minX, maxX);
        box.minY = Math.min(minY, maxY);
        box.minZ = Math.min(minZ, maxZ);
        box.maxX = Math.max(minX, maxX);
        box.maxY = Math.max(minY, maxY);
        box.maxZ = Math.max(minZ, maxZ);
    }
}
