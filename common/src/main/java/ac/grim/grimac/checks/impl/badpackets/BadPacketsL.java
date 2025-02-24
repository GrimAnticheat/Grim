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

import java.util.Locale;

@CheckData(name = "BadPacketsL", description = "Sent impossible dig packet")
public class BadPacketsL extends Check implements PacketCheck {

    public BadPacketsL(GrimPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.PLAYER_DIGGING) {
            final WrapperPlayClientPlayerDigging packet = new WrapperPlayClientPlayerDigging(event);

            if (packet.getAction() == DiggingAction.START_DIGGING || packet.getAction() == DiggingAction.FINISHED_DIGGING || packet.getAction() == DiggingAction.CANCELLED_DIGGING) return;

            // 1.8 and above clients always send digging packets that aren't used for digging at 0, 0, 0, facing DOWN
            // 1.7 and below clients do the same, except use SOUTH for RELEASE_USE_ITEM
            final int expectedFace = player.getClientVersion().isOlderThanOrEquals(ClientVersion.V_1_7_10) && packet.getAction() == DiggingAction.RELEASE_USE_ITEM
                    ? 255 : 0;

            if (packet.getBlockFaceId() != expectedFace
                    || packet.getBlockPosition().getX() != 0
                    || packet.getBlockPosition().getY() != 0
                    || packet.getBlockPosition().getZ() != 0
                    || packet.getSequence() != 0
            ) {
                if (flagAndAlert("pos="
                        + packet.getBlockPosition().getX() + ", " + packet.getBlockPosition().getY() + ", " + packet.getBlockPosition().getZ()
                        + ", face=" + packet.getBlockFace()
                        + ", sequence=" + packet.getSequence()
                        + ", action=" + packet.getAction().toString().toLowerCase(Locale.ROOT)
                ) && shouldModifyPackets() && packet.getAction() != DiggingAction.RELEASE_USE_ITEM) {
                    event.setCancelled(true);
                    player.onPacketCancel();
                }
            }
        }
    }
}
