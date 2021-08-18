package ac.grim.grimac.manager;

import ac.grim.grimac.manager.tick.Tickable;
import ac.grim.grimac.manager.tick.impl.LastTransactionSetter;
import com.google.common.collect.ClassToInstanceMap;
import com.google.common.collect.ImmutableClassToInstanceMap;
import lombok.Getter;

public class TickManager {
    ClassToInstanceMap<Tickable> syncTick;
    ClassToInstanceMap<Tickable> asyncTick;

    @Getter
    private int tick = 0;

    public TickManager() {
        syncTick = new ImmutableClassToInstanceMap.Builder<Tickable>()
                .put(LastTransactionSetter.class, new LastTransactionSetter())
                .build();

        asyncTick = new ImmutableClassToInstanceMap.Builder<Tickable>()
                .build();
    }

    public void tickSync() {
        tick++;
        syncTick.values().forEach(Tickable::tick);
    }

    public void tickAsync() {
        asyncTick.values().forEach(Tickable::tick);
    }
}
