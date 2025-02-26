package ac.grim.grimac.checks.impl.badpackets;

import ac.grim.grimac.checks.AbstractPacketCheck;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.PacketHandlerRegistry;
import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;

@CheckData(name = "BadPacketsD", description = "Impossible pitch")
public class BadPacketsD extends AbstractPacketCheck {
    public BadPacketsD(GrimPlayer player) {
        super(player);
    }

    @Override
    protected void registerReceiveHandlers(PacketHandlerRegistry<PacketReceiveEvent> registry) {
        registry.registerHandler(event -> {
            if (player.packetStateData.lastPacketWasTeleport) return;
            final float pitch = new WrapperPlayClientPlayerFlying(event).getLocation().getPitch();
            if (pitch > 90 || pitch < -90) {
                // Ban.
                if (flagAndAlert("pitch=" + pitch)) {
                    if (shouldModifyPackets()) {
                        // prevent other checks from using an invalid pitch
                        if (player.yRot > 90) player.yRot = 90;
                        if (player.yRot < -90) player.yRot = -90;

                        event.setCancelled(true);
                        player.onPacketCancel();
                    }
                }
            }
        }, PacketType.Play.Client.PLAYER_ROTATION, PacketType.Play.Client.PLAYER_POSITION_AND_ROTATION);
    }
}
