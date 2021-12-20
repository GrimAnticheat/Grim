package ac.grim.grimac.manager.init.load;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.manager.init.Initable;
import ac.grim.grimac.utils.anticheat.LogUtil;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.factory.bukkit.BukkitPacketEventsBuilder;

public class PacketEventsInit implements Initable {
    @Override
    public void start() {
        LogUtil.info("Loading PacketEvents...");

        PacketEvents.setAPI(BukkitPacketEventsBuilder.build(GrimAPI.INSTANCE.getPlugin()));
        PacketEvents.getAPI().getSettings().bStats(true).checkForUpdates(false);
        PacketEvents.getAPI().load();
    }
}
