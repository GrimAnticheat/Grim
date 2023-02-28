package ac.grim.grimac.checks.impl.badpackets;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PacketCheck;
import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.player.DiggingAction;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerDigging;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;

@CheckData(name = "BadPacketsM", experimental = true)
public class BadPacketsM extends Check implements PacketCheck {
    public BadPacketsM(GrimPlayer player) {
        super(player);
    }

    private boolean placedBlock;

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (player.getClientVersion().isOlderThan(ClientVersion.V_1_9)) return;
        if (WrapperPlayClientPlayerFlying.isFlying(event.getPacketType())) {
            placedBlock = false;
        } else if (event.getPacketType() == PacketType.Play.Client.PLAYER_BLOCK_PLACEMENT) {
            placedBlock = true;
        } else if (event.getPacketType() == PacketType.Play.Client.PLAYER_DIGGING) {
            if (placedBlock) {
                DiggingAction action = new WrapperPlayClientPlayerDigging(event).getAction();
                if (action.equals(DiggingAction.RELEASE_USE_ITEM) || action.equals(DiggingAction.DROP_ITEM) || action.equals(DiggingAction.DROP_ITEM_STACK)) {
                    flagAndAlert();
                }
            }
        }
    }
}
