package ac.grim.grimac.events.packets;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.data.SetBackData;
import io.github.retrooper.packetevents.event.PacketListenerAbstract;
import io.github.retrooper.packetevents.event.PacketListenerPriority;
import io.github.retrooper.packetevents.event.impl.PacketPlaySendEvent;
import io.github.retrooper.packetevents.packettype.PacketType;
import io.github.retrooper.packetevents.packetwrappers.WrappedPacket;
import io.github.retrooper.packetevents.packetwrappers.play.out.position.WrappedPacketOutPosition;
import io.github.retrooper.packetevents.utils.pair.Pair;
import io.github.retrooper.packetevents.utils.server.ServerVersion;
import io.github.retrooper.packetevents.utils.vector.Vector3d;

public class PacketServerTeleport extends PacketListenerAbstract {

    public PacketServerTeleport() {
        super(PacketListenerPriority.LOW);
    }

    @Override
    public void onPacketPlaySend(PacketPlaySendEvent event) {
        byte packetID = event.getPacketId();

        if (packetID == PacketType.Play.Server.POSITION) {
            WrappedPacketOutPosition teleport = new WrappedPacketOutPosition(event.getNMSPacket());

            GrimPlayer player = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(event.getPlayer());

            byte relative = teleport.getRelativeFlagsMask();
            Vector3d pos = teleport.getPosition();
            float pitch = teleport.getPitch();
            float yaw = teleport.getYaw();

            if (player == null) {
                // Player teleport event gets called AFTER player join event (wtf md_5)
                player = new GrimPlayer(event.getPlayer());
            }

            // Convert relative teleports to normal teleports
            // We have to do this because 1.8 players on 1.9+ get teleports changed by ViaVersion
            // Additionally, velocity is kept after relative teleports making predictions difficult
            // The added complexity isn't worth a feature that I have never seen used
            //
            // If you do actually need this make an issue on GitHub with an explanation for why
            if ((relative & 1) == 1)
                pos = pos.add(new Vector3d(player.packetStateData.packetPosition.x, 0, 0));

            if ((relative >> 1 & 1) == 1)
                pos = pos.add(new Vector3d(0, player.packetStateData.packetPosition.y, 0));

            if ((relative >> 2 & 1) == 1)
                pos = pos.add(new Vector3d(0, 0, player.packetStateData.packetPosition.z));

            if ((relative >> 3 & 1) == 1)
                yaw += player.packetStateData.packetPlayerXRot;

            if ((relative >> 3 & 1) == 1)
                pitch += player.packetStateData.packetPlayerYRot;

            teleport.setPosition(pos);
            teleport.setYaw(yaw);
            teleport.setPitch(pitch);
            teleport.setRelativeFlagsMask((byte) 0);

            final int lastTransactionSent = player.lastTransactionSent.get();

            // For some reason teleports on 1.7 servers are offset by 1.62?
            if (ServerVersion.getVersion().isOlderThan(ServerVersion.v_1_8))
                pos.setY(pos.getY() - 1.62);

            Vector3d finalPos = pos;

            player.getSetbackTeleportUtil().addSentTeleport(pos, lastTransactionSent);

            GrimPlayer finalPlayer = player;
            event.setPostTask(() -> {
                finalPlayer.sendTransaction();

                SetBackData data = finalPlayer.getSetbackTeleportUtil().getRequiredSetBack();
                if (data == null) return;

                Vector3d setbackPos = data.getPosition();
                if (setbackPos == null || finalPos.equals(setbackPos)) return;

                // If this wasn't the vanilla anticheat, we would have set the target position here
                SetBackData setBackData = finalPlayer.getSetbackTeleportUtil().getRequiredSetBack();
                if (setBackData != null && !setBackData.isComplete()) {
                    finalPlayer.getSetbackTeleportUtil().resendSetback(true);
                }
            });
        }

        if (packetID == PacketType.Play.Server.VEHICLE_MOVE) {
            WrappedPacket vehicleMove = new WrappedPacket(event.getNMSPacket());
            double x = vehicleMove.readDouble(0);
            double y = vehicleMove.readDouble(1);
            double z = vehicleMove.readDouble(2);

            GrimPlayer player = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(event.getPlayer());
            if (player == null) return;

            int lastTransactionSent = player.lastTransactionSent.get();
            Vector3d finalPos = new Vector3d(x, y, z);

            event.setPostTask(player::sendTransaction);
            player.vehicleData.vehicleTeleports.add(new Pair<>(lastTransactionSent, finalPos));
        }
    }
}
