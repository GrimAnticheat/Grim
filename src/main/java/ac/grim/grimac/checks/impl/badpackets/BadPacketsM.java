package ac.grim.grimac.checks.impl.badpackets;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PacketCheck;
import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType.Play.Client;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.player.DiggingAction;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerDigging;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;

@CheckData(name = "BadPacketsM", experimental = true)
public class BadPacketsM extends Check implements PacketCheck {
    public BadPacketsM(GrimPlayer player) {
        super(player);
    }

    private boolean hasPlacedBlock;

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        PacketTypeCommon packetType = event.getPacketType();

        // early returning if packet is flying
        if (WrapperPlayClientPlayerFlying.isFlying(packetType)) {
            hasPlacedBlock = false;
            return;
        }

        // early returning if packet types do not match
        if (!packetType.equals(Client.PLAYER_DIGGING) || !packetType.equals(Client.PLAYER_BLOCK_PLACEMENT)) {
            return;
        }

        // isTickingReliablyFor had falses so we added more conditions to actually check if the player was moving for 1.8+
        if (player.getClientVersion().isNewerThan(ClientVersion.V_1_8) && (!player.isTickingReliablyFor(3)
                || player.actualMovement.lengthSquared() <= 0.000064 || !player.skippedTickInActualMovement)) {
            hasPlacedBlock = false;
            return;
        }

        // since order is important we only set true when PLAYER_BLOCK_PLACEMENT
        if (packetType.equals(Client.PLAYER_BLOCK_PLACEMENT)) {
            hasPlacedBlock = true;
        } else if (hasPlacedBlock) {
            DiggingAction action = new WrapperPlayClientPlayerDigging(event).getAction();
            if (action.equals(DiggingAction.RELEASE_USE_ITEM) || action.equals(DiggingAction.DROP_ITEM) || action.equals(DiggingAction.DROP_ITEM_STACK)) {
                flagAndAlert();
            }
        }
    }
}
