package ac.grim.grimac.checks.impl.badpackets;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PacketCheck;
import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.world.BlockFace;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerDigging;

@CheckData(name = "BadPackets2", experimental = true)
public class BadPackets2 extends Check implements PacketCheck {
    public BadPackets2(final GrimPlayer player) {
        super(player);
    }

    private boolean lastCancelWasSwitch = false;

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.PLAYER_DIGGING) {
            final WrapperPlayClientPlayerDigging packet = new WrapperPlayClientPlayerDigging(event);

            // The face of the CANCELLED_DIGGING packet is always DOWN (SOUTH in 1.7), except when switching blocks
            switch (packet.getAction()) {
                case CANCELLED_DIGGING:
                    if (lastCancelWasSwitch && flagAndAlert() && shouldModifyPackets()) {
                        event.setCancelled(true);
                        player.onPacketCancel();
                    }
                    lastCancelWasSwitch = packet.getBlockFace() != (player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_8) ? BlockFace.DOWN : BlockFace.SOUTH);
                    return;
                case START_DIGGING:
                    lastCancelWasSwitch = false;
                    return;
            }
        }

        if (lastCancelWasSwitch) {
            flagAndAlert();
            lastCancelWasSwitch = false;
        }
    }
}
