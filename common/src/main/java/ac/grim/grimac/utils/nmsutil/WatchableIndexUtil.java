package ac.grim.grimac.utils.nmsutil;

import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import lombok.experimental.UtilityClass;

import java.util.List;

@UtilityClass
public class WatchableIndexUtil {
    public static EntityData<?> getIndex(List<EntityData<?>> objects, int index) {
        for (EntityData<?> object : objects) {
            if (object.getIndex() == index) return object;
        }

        return null;
    }
}
