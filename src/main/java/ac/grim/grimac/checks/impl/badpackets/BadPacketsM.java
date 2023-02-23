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

    private int i = 0;

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (player.getClientVersion().isOlderThan(ClientVersion.V_1_8)) return;
        if (WrapperPlayClientPlayerFlying.isFlying(event.getPacketType())) {
            i = 0;
        } else if (event.getPacketType() == PacketType.Play.Client.PLAYER_BLOCK_PLACEMENT) {
            i++;
        } else if (event.getPacketType() == PacketType.Play.Client.PLAYER_DIGGING) {
            if (i > 0) {
                DiggingAction action = new WrapperPlayClientPlayerDigging(event).getAction();
                if (action.equals(DiggingAction.RELEASE_USE_ITEM) || action.equals(DiggingAction.DROP_ITEM) || action.equals(DiggingAction.DROP_ITEM_STACK))
                    flagAndAlert();
            }
        }
    }
}
