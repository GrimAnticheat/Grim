package ac.grim.grimac.checks.impl.badpackets;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PacketCheck;
import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType.Play.Client;
import com.github.retrooper.packetevents.protocol.player.DiggingAction;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerDigging;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;

@CheckData(name = "BadPacketsM", experimental = true)
public class BadPacketsM extends Check implements PacketCheck {
    public BadPacketsM(GrimPlayer player) {
        super(player);
    }

    private boolean hasPlacedBlock;

    public void onPacketReceive(PacketReceiveEvent event) {
        if (!WrapperPlayClientPlayerFlying.isFlying(event.getPacketType()) && this.player.isTickingReliablyFor(3) && player.actualMovement.length() > 0.008 && this.player.skippedTickInActualMovement) {
            if (event.getPacketType() == Client.PLAYER_BLOCK_PLACEMENT) {
                this.hasPlacedBlock = true;
            } else if (event.getPacketType() == Client.PLAYER_DIGGING) {
                if (!this.hasPlacedBlock) {
                    return;
                }
                DiggingAction action = (new WrapperPlayClientPlayerDigging(event)).getAction();
                if (action.equals(DiggingAction.RELEASE_USE_ITEM) || action.equals(DiggingAction.DROP_ITEM) || action.equals(DiggingAction.DROP_ITEM_STACK)) {
                    this.flagAndAlert();
                }
            }
        } else {
            this.hasPlacedBlock = false;
        }

    }
}
