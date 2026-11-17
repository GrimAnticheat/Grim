package ac.grim.grimac.checks.impl.badpackets;

import ac.grim.grimac.api.storage.verbose.Verbose;
import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.packethandler.PacketHandlerRegistryBuilder;
import ac.grim.grimac.checks.type.PacketListener;
import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;
import org.jetbrains.annotations.NotNull;

@CheckData(name = "BadPacketsD", stableKey = "grim.badpackets.invalid_pitch", description = "Sent an invalid rotation pitch outside the -90 to 90 range")
public class BadPacketsD extends Check implements PacketListener {
    private static final Verbose V = Verbose.of("pitch={f32}");

    public BadPacketsD(GrimPlayer player) {
        super(player);
    }

    @Override
    public void registerPacketHandlers(@NotNull PacketHandlerRegistryBuilder registry) {
        registry.registerReceiveHandler(event -> {
            if (player.packetStateData.lastPacketWasTeleport) return;

            final float pitch = new WrapperPlayClientPlayerFlying(event).getLocation().getPitch();
            if (pitch > 90 || pitch < -90) {
                // Ban.
                if (flag(V.write(verbose()).f32(pitch)) && shouldModifyPackets()) {
                    // prevent other checks from using an invalid pitch
                    if (player.pitch > 90) player.pitch = 90;
                    if (player.pitch < -90) player.pitch = -90;

                    event.setCancelled(true);
                    player.onPacketCancel();
                }
            }
        }, PacketType.Play.Client.PLAYER_ROTATION, PacketType.Play.Client.PLAYER_POSITION_AND_ROTATION);
    }
}
