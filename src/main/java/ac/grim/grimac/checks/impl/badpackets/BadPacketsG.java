package ac.grim.grimac.checks.impl.badpackets;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.CheckType;
import ac.grim.grimac.checks.type.PacketCheck;
import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientEntityAction;

@CheckData(name = "BadPacketsG", description = "Sent duplicate sneaking status", checkType = CheckType.PACKETS)
public class BadPacketsG extends Check implements PacketCheck {
    private boolean lastSneaking, respawn;

    public BadPacketsG(GrimPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.ENTITY_ACTION) {
            WrapperPlayClientEntityAction packet = new WrapperPlayClientEntityAction(event);

            if (packet.getAction() == WrapperPlayClientEntityAction.Action.START_SNEAKING) {
                // The player may send two START_SNEAKING packets if they respawned
                if (lastSneaking && !respawn) {
                    if (flagAndAlert("state=true") && shouldModifyPackets()) {
                        event.setCancelled(true);
                        player.onPacketCancel();
                    }
                } else {
                    lastSneaking = true;
                }
                respawn = false;
            } else if (packet.getAction() == WrapperPlayClientEntityAction.Action.STOP_SNEAKING) {
                if (!lastSneaking && !respawn) {
                    if (flagAndAlert("state=false") && shouldModifyPackets()) {
                        event.setCancelled(true);
                        player.onPacketCancel();
                    }
                } else {
                    lastSneaking = false;
                }
                respawn = false;
            }
        }
    }

    public void handleRespawn() {
        // Clients could potentially not send a STOP_SNEAKING packet when they die, so we need to track it
        respawn = true;
    }
}
