package ac.grim.grimac.checks.impl.badpackets;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PacketCheck;
import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.player.Combat;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientClientStatus;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerChangeGameState;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerCombatEvent;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDeathCombatEvent;

@CheckData(name = "BadPacketsM", description = "Tried to respawn while alive", experimental = true)
public class BadPacketsM extends Check implements PacketCheck {
    public BadPacketsM(final GrimPlayer player) {
        super(player);
    }

    // not a boolean because the server could send packets that cause
    // the client to send a respawn packet before it receives the first
    private int exempt;

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.CLIENT_STATUS && new WrapperPlayClientClientStatus(event).getAction() == WrapperPlayClientClientStatus.Action.PERFORM_RESPAWN) {
            if (exempt > 0) {
                exempt--;
                return;
            }

            if (!player.compensatedEntities.self.isDead) {
                if (flagAndAlert() && shouldModifyPackets()) {
                    event.setCancelled(true);
                    player.onPacketCancel();
                }
            }
        }
    }

    @Override
    public void onPacketSend(PacketSendEvent event) {
        if (event.getPacketType() == PacketType.Play.Server.CHANGE_GAME_STATE && player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_9)
                && new WrapperPlayServerChangeGameState(event).getReason() == WrapperPlayServerChangeGameState.Reason.WIN_GAME) {
            player.addRealTimeTaskNow(() -> exempt++);
        }

        if (event.getPacketType() == PacketType.Play.Server.DEATH_COMBAT_EVENT && player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_9)) {
            if (new WrapperPlayServerDeathCombatEvent(event).getPlayerId() == player.entityID) {
                player.addRealTimeTaskNow(() -> exempt++);
            }
        }

        if (event.getPacketType() == PacketType.Play.Server.COMBAT_EVENT && player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_9)) {
            WrapperPlayServerCombatEvent packet = new WrapperPlayServerCombatEvent(event);
            if (packet.getCombat() == Combat.ENTITY_DEAD && packet.getPlayerId() == player.entityID) {
                player.addRealTimeTaskNow(() -> exempt++);
            }
        }
    }
}
