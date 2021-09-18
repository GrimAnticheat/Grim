package ac.grim.grimac.utils.nmsImplementations;

import io.github.retrooper.packetevents.packetwrappers.play.out.entitymetadata.WrappedWatchableObject;

import java.util.List;

public class WatchableIndexUtil {
    public static WrappedWatchableObject getIndex(List<WrappedWatchableObject> objects, int index) {
        for (WrappedWatchableObject object : objects) {
            if (object.getIndex() == index) return object;
        }

        return null;
    }
}
