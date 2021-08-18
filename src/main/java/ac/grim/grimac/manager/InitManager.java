package ac.grim.grimac.manager;

import ac.grim.grimac.manager.init.Initable;
import ac.grim.grimac.manager.init.start.EventManager;
import ac.grim.grimac.manager.init.start.PacketManager;
import ac.grim.grimac.manager.init.start.TickRunner;
import ac.grim.grimac.manager.init.start.ViaBackwardsManager;
import com.google.common.collect.ClassToInstanceMap;
import com.google.common.collect.ImmutableClassToInstanceMap;

public class InitManager implements Initable {
    ClassToInstanceMap<Initable> initializersOnLoad;
    ClassToInstanceMap<Initable> initializersOnStart;

    public InitManager() {
        initializersOnLoad = new ImmutableClassToInstanceMap.Builder<Initable>()
                .build();

        initializersOnStart = new ImmutableClassToInstanceMap.Builder<Initable>()
                .put(EventManager.class, new EventManager())
                .put(PacketManager.class, new PacketManager())
                .put(ViaBackwardsManager.class, new ViaBackwardsManager())
                .put(TickRunner.class, new TickRunner())
                .build();
    }

    @Override
    public void start() {
        for (Initable initable : initializersOnStart.values()) {
            initable.start();
        }
    }
}