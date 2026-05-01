package ac.grim.grimac.utils.nmsutil;

import ac.grim.grimac.utils.data.packetentity.PacketEntity;
import ac.grim.grimac.utils.enums.Pose;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.pose.EntityPose;
import com.github.retrooper.packetevents.protocol.entity.type.EntityType;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@UtilityClass
public class EntityMetadataPoseUtil {

    private static final boolean SERVER_HAS_POSE = PacketEvents.getAPI().getServerManager().getVersion().isNewerThanOrEquals(ServerVersion.V_1_14);
    private static final int POSE_METADATA_INDEX = 6;

    public static boolean usesPoseMetadata(@Nullable PacketEntity entity) {
        return entity != null && usesPoseMetadata(entity.type);
    }

    public static boolean usesPoseMetadata(@Nullable EntityType entityType) {
        return SERVER_HAS_POSE && (entityType == EntityTypes.PLAYER || entityType == EntityTypes.MANNEQUIN);
    }

    public static @Nullable Pose getPoseFromMetadata(List<EntityData<?>> entityMetadata) {
        EntityData<?> poseData = WatchableIndexUtil.getIndex(entityMetadata, POSE_METADATA_INDEX);
        return poseData == null || !(poseData.getValue() instanceof EntityPose pose) ? null : mapEntityPose(pose);
    }

    private static Pose mapEntityPose(EntityPose entityPose) {
        return switch (entityPose) {
            case FALL_FLYING -> Pose.FALL_FLYING;
            case SLEEPING -> Pose.SLEEPING;
            case SWIMMING -> Pose.SWIMMING;
            case SPIN_ATTACK -> Pose.SPIN_ATTACK;
            case CROUCHING -> Pose.CROUCHING;
            case LONG_JUMPING -> Pose.LONG_JUMPING;
            case DYING -> Pose.DYING;
            default -> Pose.STANDING;
        };
    }
}
