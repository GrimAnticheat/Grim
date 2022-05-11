package ac.grim.grimac.manager;

import ac.grim.grimac.manager.tick.Tickable;
import ac.grim.grimac.manager.tick.impl.ClientVersionSetter;
import ac.grim.grimac.manager.tick.impl.ResetTick;
import com.google.common.collect.ClassToInstanceMap;
import com.google.common.collect.ImmutableClassToInstanceMap;

public class TickManager {
    ClassToInstanceMap<Tickable> syncTick;

    public TickManager() {
        syncTick = new ImmutableClassToInstanceMap.Builder<Tickable>()
                .put(ClientVersionSetter.class, new ClientVersionSetter())
                .put(ResetTick.class, new ResetTick())
                .build();
    }

    public void tickSync() {
        syncTick.values().forEach(Tickable::tick);
    }
}
