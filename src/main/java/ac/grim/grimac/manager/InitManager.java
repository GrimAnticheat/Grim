package ac.grim.grimac.manager;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.GrimExternalAPI;
import ac.grim.grimac.manager.init.Initable;
import ac.grim.grimac.manager.init.load.PacketEventsInit;
import ac.grim.grimac.manager.init.start.*;
import ac.grim.grimac.manager.init.stop.TerminatePacketEvents;
import com.google.common.collect.ClassToInstanceMap;
import com.google.common.collect.ImmutableClassToInstanceMap;
import lombok.Getter;

public class InitManager {
    private final ClassToInstanceMap<Initable> initializersOnLoad;
    private final ClassToInstanceMap<Initable> initializersOnStart;
    private final ClassToInstanceMap<Initable> initializersOnStop;

    @Getter private boolean loaded = false;
    @Getter private boolean started = false;
    @Getter private boolean stopped = false;

    public InitManager() {
        initializersOnLoad = new ImmutableClassToInstanceMap.Builder<Initable>()
                .put(PacketEventsInit.class, new PacketEventsInit())
                .build();

        initializersOnStart = new ImmutableClassToInstanceMap.Builder<Initable>()
                .put(GrimExternalAPI.class, GrimAPI.INSTANCE.getExternalAPI())
                .put(ExemptOnlinePlayers.class, new ExemptOnlinePlayers())
                .put(EventManager.class, new EventManager())
                .put(PacketManager.class, new PacketManager())
                .put(ViaBackwardsManager.class, new ViaBackwardsManager())
                .put(TickRunner.class, new TickRunner())
                .put(TickEndEvent.class, new TickEndEvent())
                .put(CommandRegister.class, new CommandRegister())
                .put(BStats.class, new BStats())
                .put(PacketLimiter.class, new PacketLimiter())
                .put(DiscordManager.class, GrimAPI.INSTANCE.getDiscordManager())
                .put(SpectateManager.class, GrimAPI.INSTANCE.getSpectateManager())
                .put(JavaVersion.class, new JavaVersion())
                .build();

        initializersOnStop = new ImmutableClassToInstanceMap.Builder<Initable>()
                .put(TerminatePacketEvents.class, new TerminatePacketEvents())
                .build();
    }

    public void load() {
        for (Initable initable : initializersOnLoad.values()) {
            initable.start();
        }
        loaded = true;
    }

    public void start() {
        for (Initable initable : initializersOnStart.values()) {
            initable.start();
        }
        started = true;
    }

    public void stop() {
        for (Initable initable : initializersOnStop.values()) {
            initable.start();
        }
        stopped = true;
    }
}