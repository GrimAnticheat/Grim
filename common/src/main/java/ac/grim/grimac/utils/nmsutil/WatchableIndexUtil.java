package ac.grim.grimac.utils.nmsutil;

import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@UtilityClass
public class WatchableIndexUtil {
    @Contract(pure = true)
    public static @Nullable EntityData<?> getIndex(@NotNull List<EntityData<?>> objects, int index) {
        for (EntityData<?> object : objects) {
            if (object.getIndex() == index) return object;
        }

        return null;
    }

    @Contract(pure = true)
    public static @Nullable Object getIndexValue(@NotNull List<EntityData<?>> objects, int index) {
        for (EntityData<?> object : objects) {
            if (object.getIndex() == index) return object.getValue();
        }

        return null;
    }
}
