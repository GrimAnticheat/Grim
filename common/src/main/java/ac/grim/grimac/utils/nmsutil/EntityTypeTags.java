package ac.grim.grimac.utils.nmsutil;

import com.github.retrooper.packetevents.protocol.entity.type.EntityType;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import lombok.experimental.UtilityClass;

// this class represents vanilla entity tags that packetevents lacks
@UtilityClass
public class EntityTypeTags {

    public static final EntityTag CAN_FLOAT_WHILE_RIDDEN = new EntityTag(
            EntityTypes.HORSE, EntityTypes.ZOMBIE_HORSE, EntityTypes.MULE,
            EntityTypes.DONKEY, EntityTypes.CAMEL, EntityTypes.CAMEL_HUSK
    );

    public record EntityTag(EntityType... tags) {
        public boolean anyOf(EntityType tested) {
            for (EntityType type : tags) {
                if (tested.isInstanceOf(type)) return true;
            }
            return false;
        }
    }
}
