package ac.grim.grimac.checks.impl.badpackets;

import ac.grim.grimac.api.storage.verbose.Verbose;
import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.impl.verbose.VerboseCodecs;
import ac.grim.grimac.checks.type.PacketCheck;
import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.player.DiggingAction;
import com.github.retrooper.packetevents.util.Vector3i;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerDigging;

@CheckData(name = "BadPacketsL", stableKey = "grim.badpackets.invalid_dig", description = "Sent impossible dig packet")
public class BadPacketsL extends Check implements PacketCheck {
    private static final Verbose V =
            Verbose.of("pos={mcpos}, face={sint}, sequence={sint}, action={digging_lower}");

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
                final Vector3i pos = packet.getBlockPosition();
                var buf = V.write(verbose())
                        .mcPos(pos.getX(), pos.getY(), pos.getZ())
                        .sint(packet.getBlockFaceId())
                        .sint(packet.getSequence())
                        .uint(VerboseCodecs.enumId(packet.getAction()));
                if (flag(buf)
                        && shouldModifyPackets() && canCancel(packet.getAction())) {
                    event.setCancelled(true);
                    player.onPacketCancel();
                }
            }
        }
    }
}
