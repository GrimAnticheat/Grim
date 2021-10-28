package ac.grim.grimac.manager.init.start;

import ac.grim.grimac.events.packets.*;
import ac.grim.grimac.events.packets.worldreader.PacketWorldReaderEight;
import ac.grim.grimac.events.packets.worldreader.PacketWorldReaderNine;
import ac.grim.grimac.events.packets.worldreader.PacketWorldReaderSeven;
import ac.grim.grimac.events.packets.worldreader.PacketWorldReaderSixteen;
import ac.grim.grimac.manager.init.Initable;
import ac.grim.grimac.utils.anticheat.LogUtil;
import io.github.retrooper.packetevents.PacketEvents;
import io.github.retrooper.packetevents.utils.server.ServerVersion;

public class PacketManager implements Initable {
    @Override
    public void start() {
        LogUtil.info("Registering packets...");

        PacketEvents.get().registerListener(new PacketPlayerAbilities());
        PacketEvents.get().registerListener(new PacketPingListener());
        PacketEvents.get().registerListener(new PacketPlayerDigging());
        PacketEvents.get().registerListener(new PacketPlayerAttack());
        PacketEvents.get().registerListener(new PacketEntityAction());
        PacketEvents.get().registerListener(new PacketBlockAction());
        PacketEvents.get().registerListener(new PacketFireworkListener());
        PacketEvents.get().registerListener(new PacketSelfMetadataListener());
        PacketEvents.get().registerListener(new PacketServerTeleport());
        PacketEvents.get().registerListener(new PacketPlayerCooldown());
        PacketEvents.get().registerListener(new PacketPlayerRespawn());
        PacketEvents.get().registerListener(new CheckManagerListener());
        PacketEvents.get().registerListener(new PacketPlayerSteer());

        if (ServerVersion.getVersion().isNewerThanOrEquals(ServerVersion.v_1_16)) {
            PacketEvents.get().registerListener(new PacketWorldReaderSixteen());
        } else if (ServerVersion.getVersion().isNewerThanOrEquals(ServerVersion.v_1_9)) {
            PacketEvents.get().registerListener(new PacketWorldReaderNine());
        } else if (ServerVersion.getVersion().isNewerThanOrEquals(ServerVersion.v_1_8)) {
            PacketEvents.get().registerListener(new PacketWorldReaderEight());
        } else {
            PacketEvents.get().registerListener(new PacketWorldReaderSeven());
        }

        PacketEvents.get().init();
    }
}
