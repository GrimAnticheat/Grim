package ac.grim.grimac.checks.impl.badpackets;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.CheckType;
import ac.grim.grimac.checks.type.PacketCheck;
import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.GameMode;

@CheckData(name = "BadPacketsR", decay = 0.25, experimental = true, checkType = CheckType.PACKETS)
public class BadPacketsR extends Check implements PacketCheck {
    public BadPacketsR(final GrimPlayer player) {
        super(player);
    }

    private int positions = 0;
    private long clock = 0;
    private long lastTransTime;
    private int oldTransId = 0;

    @Override
    public void onPacketReceive(final PacketReceiveEvent event) {
        if (isTransaction(event.getPacketType()) && player.packetStateData.lastTransactionPacketWasValid) {
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
        }
        //
        if ((event.getPacketType() == PacketType.Play.Client.PLAYER_POSITION_AND_ROTATION ||
                event.getPacketType() == PacketType.Play.Client.PLAYER_POSITION) && !player.inVehicle()) {
            positions++;
        } else if ((event.getPacketType() == PacketType.Play.Client.STEER_VEHICLE || event.getPacketType() == PacketType.Play.Client.VEHICLE_MOVE)
                && player.inVehicle()) {
            positions++;
        }
    }

}
