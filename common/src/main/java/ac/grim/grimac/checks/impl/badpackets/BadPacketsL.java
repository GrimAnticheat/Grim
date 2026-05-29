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

@CheckData(name = "BadPacketsL", stableKey = "grim.badpackets.invalid_dig", description = "Sent impossible dig packet")
public class BadPacketsL extends Check implements PacketCheck {

    public BadPacketsL(GrimPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.PLAYER_DIGGING) {
            final WrapperPlayClientPlayerDigging packet = new WrapperPlayClientPlayerDigging(event);

            if (packet.getAction() == DiggingAction.START_DIGGING || packet.getAction() == DiggingAction.FINISHED_DIGGING || packet.getAction() == DiggingAction.CANCELLED_DIGGING)
                return;

            // 1.8 and above clients always send digging packets that aren't used for digging at 0, 0, 0, face 0
            // 1.7 and below clients do the same, except use face 255 for RELEASE_USE_ITEM
            // as of https://github.com/ViaVersion/ViaRewind/commit/e7b0606e187afbccf98ef7c88d3f3af27fe11da3, ViaRewind maps the face to 0
            // let's allow both, just to be safe
            final boolean allowLegacyFace = player.getClientVersion().isOlderThanOrEquals(ClientVersion.V_1_7_10)
                    && packet.getAction() == DiggingAction.RELEASE_USE_ITEM;
            final boolean isValidFace = packet.getBlockFaceId() == 0 || allowLegacyFace && packet.getBlockFaceId() == 255;

            if (!isValidFace
                    || packet.getBlockPosition().getX() != 0
                    || packet.getBlockPosition().getY() != 0
                    || packet.getBlockPosition().getZ() != 0
                    || packet.getSequence() != 0
            ) {
                if (flagAndAlert("pos="
                        + packet.getBlockPosition().getX() + ", " + packet.getBlockPosition().getY() + ", " + packet.getBlockPosition().getZ()
                        + ", face=" + packet.getBlockFaceId()
                        + ", sequence=" + packet.getSequence()
                        + ", action=" + packet.getAction().toString().toLowerCase(Locale.ROOT)
                ) && shouldModifyPackets() && canCancel(packet.getAction())) {
                    event.setCancelled(true);
                    player.onPacketCancel();
                }
            }
        }
    }
}
