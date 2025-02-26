package ac.grim.grimac.checks.impl.badpackets;

import ac.grim.grimac.checks.AbstractPacketCheck;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.PacketHandlerRegistry;
import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.GameMode;

@CheckData(name = "BadPacketsR", decay = 0.25, experimental = true)
public class BadPacketsR extends AbstractPacketCheck {
    public BadPacketsR(final GrimPlayer player) {
        super(player);
    }

    private int positions = 0;
    private long clock = 0;
    private long lastTransTime;
    private int oldTransId = 0;

    @Override
    protected void registerReceiveHandlers(PacketHandlerRegistry<PacketReceiveEvent> registry) {
        registry.registerHandler(event -> {
            if (!player.packetStateData.lastTransactionPacketWasValid) return;
            long ms = (player.getPlayerClockAtLeast() - clock) / 1000000L;
            long diff = (System.currentTimeMillis() - lastTransTime);
            if (diff > 2000 && ms > 2000) {
                if (positions == 0 && clock != 0 && player.gamemode != GameMode.SPECTATOR && !player.compensatedEntities.self.isDead) {
                    flag("time=" + ms + "ms, " + "lst=" + diff + "ms, positions=" + positions);
                } else {
                    reward();
                }
                player.compensatedWorld.removeInvalidPistonLikeStuff(oldTransId);
                positions = 0;
                clock = player.getPlayerClockAtLeast();
                lastTransTime = System.currentTimeMillis();
                oldTransId = player.lastTransactionSent.get();
            }
        }, this::isTransaction);
        registry.registerHandler(event -> {
            if (player.inVehicle()) return;
            positions++;
        }, PacketType.Play.Client.PLAYER_POSITION_AND_ROTATION, PacketType.Play.Client.PLAYER_POSITION);
        registry.registerHandler(event -> {
            if (!player.inVehicle()) return;
            positions++;
        }, PacketType.Play.Client.STEER_VEHICLE, PacketType.Play.Client.VEHICLE_MOVE);
    }
}
