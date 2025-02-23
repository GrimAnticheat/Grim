package ac.grim.grimac.manager;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.manager.init.Initable;
import ac.grim.grimac.manager.init.load.PacketEventsInit;
import ac.grim.grimac.manager.init.start.*;
import ac.grim.grimac.manager.init.stop.TerminatePacketEvents;
import ac.grim.grimac.platform.api.sender.Sender;
import com.github.retrooper.packetevents.PacketEventsAPI;
import com.google.common.collect.ImmutableList;
import lombok.Getter;
import org.incendo.cloud.CommandManager;

import java.util.Arrays;
import java.util.function.Supplier;

public class InitManager {

    private final ImmutableList<Initable> initializersOnLoad;
    private final ImmutableList<Initable> initializersOnStart;
    private final ImmutableList<Initable> initializersOnStop;

    @Getter private boolean loaded = false;
    @Getter private boolean started = false;
    @Getter private boolean stopped = false;

    public InitManager(PacketEventsAPI<?> packetEventsAPI, Supplier<CommandManager<Sender>> commandManager, Initable... platformSpecificInitables) {
        initializersOnLoad = ImmutableList.<Initable>builder()
                .add(new PacketEventsInit(packetEventsAPI))
                .add(() -> GrimAPI.INSTANCE.getExternalAPI().load())
                .build();

        initializersOnStart = ImmutableList.<Initable>builder()
                .add(GrimAPI.INSTANCE.getExternalAPI())
//                .add(new BukkitExemptOnlinePlayersOnReload())
//                .add(new BukkitEventManager())
                .add(new PacketManager())
                .add(new ViaBackwardsManager())
                .add(new TickRunner())
//                .add(new AbstractTickEndEvent())
                .add(new CommandRegister(commandManager))
                .add(new BStats())
                .add(new PacketLimiter())
                .add(GrimAPI.INSTANCE.getDiscordManager())
                .add(GrimAPI.INSTANCE.getSpectateManager())
                .add(new JavaVersion())
                .add(new ViaVersion())
                .add(new TAB())
//                .add(() -> {
//                    if (MessageUtil.hasPlaceholderAPI) {
//                        new PlaceholderAPIExpansion().register();
//                    }
//                })
                .addAll(Arrays.asList(platformSpecificInitables))
                .build();

        initializersOnStop = ImmutableList.<Initable>builder()
                .add(new TerminatePacketEvents())
                .build();
    }

    public void load() {
        for (Initable initable : initializersOnLoad) handle(initable);
        loaded = true;
    }

    public void start() {
        for (Initable initable : initializersOnStart) handle(initable);
        started = true;
    }

    public void stop() {
        for (Initable initable : initializersOnStop) handle(initable);
        stopped = true;
    }

    private void handle(Initable initable) {
        try {
            initable.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
