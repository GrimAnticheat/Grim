package ac.grim.grimac.checks.impl.crash;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.packethandler.PacketHandlerRegistryBuilder;
import ac.grim.grimac.checks.type.PacketListener;
import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;
import org.jetbrains.annotations.NotNull;

@CheckData(name = "CrashA", stableKey = "grim.crash.large_position", description = "Sent a position outside the valid world bounds")
public class CrashA extends Check implements PacketListener {
    private static final double HARD_CODED_BORDER = 2.9999999E7D;

    public CrashA(GrimPlayer player) {
        super(player);
    }

    @Override
    public void registerPacketHandlers(@NotNull PacketHandlerRegistryBuilder registry) {
        registry.registerPrePredictionReceiveHandler(event -> {
            if (player.packetStateData.lastPacketWasTeleport) return;
            WrapperPlayClientPlayerFlying packet = new WrapperPlayClientPlayerFlying(event);
            Vector3d pos = packet.getLocation().getPosition();

            // Y technically is uncapped, but no player will reach these values legit
            if (Math.abs(pos.getX()) > HARD_CODED_BORDER || Math.abs(pos.getZ()) > HARD_CODED_BORDER || Math.abs(pos.getY()) > Integer.MAX_VALUE) {
                flag(); // Ban
                executeViolationSetback();
                event.setCancelled(true);
                player.onPacketCancel();
            }
        }, PacketType.Play.Client.PLAYER_POSITION, PacketType.Play.Client.PLAYER_POSITION_AND_ROTATION);
    }
}
