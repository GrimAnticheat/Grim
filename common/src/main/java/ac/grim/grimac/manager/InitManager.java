package ac.grim.grimac.manager;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.manager.init.Initable;
import ac.grim.grimac.manager.init.load.LoadableInitable;
import ac.grim.grimac.manager.init.load.PacketEventsInit;
import ac.grim.grimac.manager.init.start.CommandRegister;
import ac.grim.grimac.manager.init.start.JavaVersion;
import ac.grim.grimac.manager.init.start.PacketLimiter;
import ac.grim.grimac.manager.init.start.PacketManager;
import ac.grim.grimac.manager.init.start.StartableInitable;
import ac.grim.grimac.manager.init.start.TAB;
import ac.grim.grimac.manager.init.start.TickRunner;
import ac.grim.grimac.manager.init.start.ViaBackwardsManager;
import ac.grim.grimac.manager.init.start.ViaVersion;
import ac.grim.grimac.manager.init.stop.StoppableInitable;
import ac.grim.grimac.manager.init.stop.TerminatePacketEvents;
import ac.grim.grimac.platform.api.sender.Sender;
import com.github.retrooper.packetevents.PacketEventsAPI;
import com.google.common.collect.ImmutableList;
import lombok.Getter;
import org.incendo.cloud.CommandManager;

import java.util.ArrayList;
import java.util.function.Supplier;

public class InitManager {

    private final ImmutableList<LoadableInitable> initializersOnLoad;
    private final ImmutableList<StartableInitable> initializersOnStart;
    private final ImmutableList<StoppableInitable> initializersOnStop;

    @Getter
    private boolean loaded = false;
    @Getter
    private boolean started = false;
    @Getter
    private boolean stopped = false;

    public InitManager(PacketEventsAPI<?> packetEventsAPI, Supplier<CommandManager<Sender>> commandManager, Initable... platformSpecificInitables) {
        ArrayList<LoadableInitable> extraLoadableInitables = new ArrayList<>();
        ArrayList<StartableInitable> extraStartableInitables = new ArrayList<>();
        ArrayList<StoppableInitable> extraStoppableInitables = new ArrayList<>();
        for (Initable initable : platformSpecificInitables) {
            if (initable instanceof LoadableInitable) extraLoadableInitables.add((LoadableInitable) initable);
            if (initable instanceof StartableInitable) extraStartableInitables.add((StartableInitable) initable);
            if (initable instanceof StoppableInitable) extraStoppableInitables.add((StoppableInitable) initable);
        }

        initializersOnLoad = ImmutableList.<LoadableInitable>builder()
                .add(new PacketEventsInit(packetEventsAPI))
                .add(() -> GrimAPI.INSTANCE.getExternalAPI().load())
                .addAll(extraLoadableInitables)
                .build();

        initializersOnStart = ImmutableList.<StartableInitable>builder()
                .add(GrimAPI.INSTANCE.getExternalAPI())
                .add(new PacketManager())
                .add(new ViaBackwardsManager())
                .add(new TickRunner())
                .add(new CommandRegister(commandManager))
                .add(new PacketLimiter())
                .add(GrimAPI.INSTANCE.getAlertManager())
                .add(GrimAPI.INSTANCE.getDiscordManager())
                .add(GrimAPI.INSTANCE.getSpectateManager())
                .add(new JavaVersion())
                .add(new ViaVersion())
                .add(new TAB())
                .addAll(extraStartableInitables)
                .build();

        initializersOnStop = ImmutableList.<StoppableInitable>builder()
                .add(new TerminatePacketEvents())
                .addAll(extraStoppableInitables)
                .build();
    }

    public void load() {
        for (LoadableInitable initable : initializersOnLoad)
            try {
                initable.load();
            } catch (Exception e) {
                e.printStackTrace();
            }
        loaded = true;
    }

    public void start() {
        for (StartableInitable initable : initializersOnStart)
            try {
                initable.start();
            } catch (Exception e) {
                e.printStackTrace();
            }
        started = true;
    }

    public void stop() {
        for (StoppableInitable initable : initializersOnStop)
            try {
                initable.stop();
            } catch (Exception e) {
                e.printStackTrace();
            }
        stopped = true;
    }
}
