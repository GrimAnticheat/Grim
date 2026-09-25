package ac.grim.grimac.events.packets;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.connection.ConnectionUtils;
import ac.grim.grimac.utils.data.RotationData;
import ac.grim.grimac.utils.math.GrimMath;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerBundle;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerRotation;

public class PacketServerPlayerRotation extends PacketListenerAbstract {

    public PacketServerPlayerRotation() {
        super(PacketListenerPriority.LOW);
    }

    @Override
    public boolean isPreVia() {
        return true;
    }

    @Override
    public void onPacketSend(PacketSendEvent event) {
        if (event.getPacketType() == PacketType.Play.Server.PLAYER_ROTATION) {
            GrimPlayer player = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(event.getUser());
            if (player == null) return;

            WrapperPlayServerPlayerRotation packet = new WrapperPlayServerPlayerRotation(event);

            // I don't want to deal with this, so we'll prevent it
            if (!Float.isFinite(packet.getPitch())) {
                packet.setPitch(0);
                event.markForReEncode(true);
            }
            if (!Float.isFinite(packet.getYaw())) {
                packet.setYaw(0);
                event.markForReEncode(true);
            }

            if (!player.packetStateData.sendingBundlePacket) {
                ConnectionUtils.sendPacketPreVia(player, new WrapperPlayServerBundle());
                event.getTasksAfterSend().add(() -> ConnectionUtils.sendPacketPreVia(player, new WrapperPlayServerBundle()));
            }
            player.sendTransaction();
            player.pendingRotations.add(new RotationData(
                    packet.getYaw(),
                    packet.isRelativePitch() ? packet.getPitch() : GrimMath.clamp(packet.getPitch() % 360F, -90F, 90F),
                    packet.isRelativeYaw(),
                    packet.isRelativePitch(),
                    player.getLastTransactionSent()
            ));
        }
    }
}
