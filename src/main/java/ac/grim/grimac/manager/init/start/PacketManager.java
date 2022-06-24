package ac.grim.grimac.manager.init.start;

import ac.grim.grimac.events.packets.*;
import ac.grim.grimac.events.packets.worldreader.BasePacketWorldReader;
import ac.grim.grimac.events.packets.worldreader.PacketWorldReaderEight;
import ac.grim.grimac.events.packets.worldreader.PacketWorldReaderEighteen;
import ac.grim.grimac.manager.init.Initable;
import ac.grim.grimac.utils.anticheat.LogUtil;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.manager.server.ServerVersion;


public class PacketManager implements Initable {
    @Override
    public void start() {
        LogUtil.info("Registering packets...");

        PacketEvents.getAPI().getEventManager().registerListener(new PacketPlayerJoinQuit());
        PacketEvents.getAPI().getEventManager().registerListener(new PacketPingListener());
        PacketEvents.getAPI().getEventManager().registerListener(new PacketPlayerDigging());
        PacketEvents.getAPI().getEventManager().registerListener(new PacketPlayerAttack());
        PacketEvents.getAPI().getEventManager().registerListener(new PacketEntityAction());
        PacketEvents.getAPI().getEventManager().registerListener(new PacketBlockAction());
        PacketEvents.getAPI().getEventManager().registerListener(new PacketSelfMetadataListener());
        PacketEvents.getAPI().getEventManager().registerListener(new PacketServerTeleport());
        PacketEvents.getAPI().getEventManager().registerListener(new PacketPlayerCooldown());
        PacketEvents.getAPI().getEventManager().registerListener(new PacketPlayerRespawn());
        PacketEvents.getAPI().getEventManager().registerListener(new CheckManagerListener());
        PacketEvents.getAPI().getEventManager().registerListener(new PacketPlayerSteer());

        if (PacketEvents.getAPI().getServerManager().getVersion().isNewerThanOrEquals(ServerVersion.V_1_18)) {
            PacketEvents.getAPI().getEventManager().registerListener(new PacketWorldReaderEighteen());
        } else if (PacketEvents.getAPI().getServerManager().getVersion().isOlderThanOrEquals(ServerVersion.V_1_8_8)) {
            PacketEvents.getAPI().getEventManager().registerListener(new PacketWorldReaderEight());
        } else {
            PacketEvents.getAPI().getEventManager().registerListener(new BasePacketWorldReader());
        }

        PacketEvents.getAPI().getEventManager().registerListener(new ProxyAlertMessenger());
        PacketEvents.getAPI().getEventManager().registerListener(new PacketSetWrapperNull());

        PacketEvents.getAPI().init();
    }
}
