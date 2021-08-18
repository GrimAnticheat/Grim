package ac.grim.grimac.manager.init.load;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.manager.init.Initable;
import ac.grim.grimac.utils.anticheat.LogUtil;
import io.github.retrooper.packetevents.PacketEvents;
import io.github.retrooper.packetevents.settings.PacketEventsSettings;
import io.github.retrooper.packetevents.utils.server.ServerVersion;

public class PacketEventsInit implements Initable {
    @Override
    public void start() {
        LogUtil.info("Loading PacketEvents...");

        PacketEvents.create(GrimAPI.INSTANCE.getPlugin());
        PacketEventsSettings settings = PacketEvents.get().getSettings();
        settings.fallbackServerVersion(ServerVersion.v_1_7_10).compatInjector(false).checkForUpdates(false).bStats(true);
        PacketEvents.get().loadAsyncNewThread();
    }
}
