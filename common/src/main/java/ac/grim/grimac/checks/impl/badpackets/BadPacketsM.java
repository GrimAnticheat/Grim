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

@CheckData(name = "BadPacketsM", stableKey = "grim.badpackets.respawn_alive", description = "Tried to respawn while alive", experimental = true)
public class BadPacketsM extends Check implements PacketCheck {
    public BadPacketsM(final GrimPlayer player) {
        super(player);
    }

    // not a boolean because the server could send packets that cause
    // the client to send a respawn packet before it receives the first
    private int exempt;
    private boolean menu;

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() != PacketType.Play.Client.CLIENT_STATUS
                || new WrapperPlayClientClientStatus(event).getAction() != WrapperPlayClientClientStatus.Action.PERFORM_RESPAWN) {
            return;
        }

        if (exempt > 0) {
            exempt--;
            return;
        }

        if (!player.compensatedEntities.self.isDead && !menu) {
            flagAndAlert(); // don't cancel in case of a false positive
        }

        // the client closes the menu and reopens it if dead
        menu = player.compensatedEntities.self.isDead && player.packetStateData.showsDeathScreen;
    }

    @Override
    public void onPacketSend(PacketSendEvent event) {
        if (player.getClientVersion().isOlderThanOrEquals(ClientVersion.V_1_8)) {
            return;
        }

        if (event.getPacketType() == PacketType.Play.Server.CHANGE_GAME_STATE) {
            WrapperPlayServerChangeGameState packet = new WrapperPlayServerChangeGameState(event);
            if (packet.getReason() != WrapperPlayServerChangeGameState.Reason.WIN_GAME) return;

            if (packet.getValue() != 0 && packet.getValue() != 1) {
                return; // client ignores this
            }

            player.sendTransaction();
            player.addRealTimeTaskNow(() -> {
                // we COULD get a DEATH_COMBAT_EVENT/COMBAT_EVENT while the credits are rolling, (IF packet.getValue == 1)
                // but this can only cause at most one false negative (for each of this packet sent)
                exempt++;
                menu = false;
            });
        }

        if (event.getPacketType() == PacketType.Play.Server.DEATH_COMBAT_EVENT
                && new WrapperPlayServerDeathCombatEvent(event).getPlayerId() == player.entityID) {
            player.sendTransaction();
            player.addRealTimeTaskNow(this::onDeathCombatEvent);
        }

        if (event.getPacketType() == PacketType.Play.Server.COMBAT_EVENT) {
            WrapperPlayServerCombatEvent packet = new WrapperPlayServerCombatEvent(event);
            if (packet.getCombat() == Combat.ENTITY_DEAD && packet.getPlayerId() == player.entityID) {
                player.sendTransaction();
                player.addRealTimeTaskNow(this::onDeathCombatEvent);
            }
        }
    }

    private void onDeathCombatEvent() {
        if (player.packetStateData.showsDeathScreen) {
            menu = true;
        } else {
            exempt++;
        }
    }

    public void onRespawn() {
        menu = false; // the client closes any open screens on respawn
    }

    // the menu is actually kept when the player's health is set to >0
    public void onDeath() {
        if (player.packetStateData.showsDeathScreen) {
            menu = true;
        }
    }

    // via sends a respawn packet for pre-1.15 clients on 1.15+ servers with the immediate_respawn gamerule enabled
    public void exemptVia() {
        exempt++;
    }
}
