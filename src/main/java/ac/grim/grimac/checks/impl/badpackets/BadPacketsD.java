package ac.grim.grimac.checks.impl.badpackets;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PacketCheck;
import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;

@CheckData(name = "BadPacketsD")
public class BadPacketsD extends Check implements PacketCheck {
    public BadPacketsD(final GrimPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(final PacketReceiveEvent event) {
        if (player.packetStateData.lastPacketWasTeleport) return;

        if (event.getPacketType() != PacketType.Play.Client.PLAYER_ROTATION &&
                event.getPacketType() != PacketType.Play.Client.PLAYER_POSITION_AND_ROTATION) return;

        final float pitch = new WrapperPlayClientPlayerFlying(event).getLocation().getPitch();
        if (pitch <= 90f && pitch >= -90f) return;

        // Ban.
        if (!flagAndAlert("pitch=" + pitch) || !shouldModifyPackets()) return;

        // prevent other checks from using an invalid pitch
        if (player.yRot > 90f) player.yRot = 90f;
        if (player.yRot < -90f) player.yRot = -90f;

        event.setCancelled(true);
        player.onPacketCancel();
    }
}
