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
import org.bukkit.entity.Player;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class PacketServerTeleport extends PacketListenerAbstract {

    public PacketServerTeleport() {
        super(PacketListenerPriority.LOW);
    }

    // Don't lecture me about how this isn't object orientated and should be in the player object
    // Bukkit internal code is like this:
    // 1) Teleport the player
    // 2) Call the player join event
    //
    // It would be more of a hack to wait on the first teleport to add the player to the list of checked players...
    public static final ConcurrentHashMap<Player, ConcurrentLinkedQueue<Pair<Integer, Vector3d>>> teleports = new ConcurrentHashMap<>();

    public static void removePlayer(Player player) {
        teleports.remove(player);
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
                // Login
                if (relative == 0) {
                    // Init teleports
                    initPlayer(event.getPlayer());
                    ConcurrentLinkedQueue<Pair<Integer, Vector3d>> map = getPlayerTeleports(event.getPlayer());
                    // Don't memory leak on players not being checked while still allowing reasonable plugins to teleport
                    // before our player join event is called
                    if (map.size() > 10) return;
                    // 0 transactions total have been sent - we aren't tracking this player yet!
                    map.add(new Pair<>(0, pos));
                }
                return;
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

            // Fucking bukkit teleports the player before login event!
            // Meaning that we miss the first teleport, thanks a lot
            ConcurrentLinkedQueue<Pair<Integer, Vector3d>> map = teleports.get(event.getPlayer());
            map.add(new Pair<>(lastTransactionSent, finalPos));

            event.setPostTask(() -> {
                player.sendTransaction();

                SetBackData data = player.getSetbackTeleportUtil().getRequiredSetBack();
                if (data == null) return;

                Vector3d setbackPos = data.getPosition();
                if (setbackPos == null || finalPos.equals(setbackPos)) return;

                // If this wasn't the vanilla anticheat, we would have set the target position here
                SetBackData setBackData = player.getSetbackTeleportUtil().getRequiredSetBack();
                if (setBackData != null && !setBackData.isComplete()) {
                    player.getSetbackTeleportUtil().resendSetback(true);
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

    public static void initPlayer(Player player) {
        teleports.putIfAbsent(player, new ConcurrentLinkedQueue<>());
    }

    public static ConcurrentLinkedQueue<Pair<Integer, Vector3d>> getPlayerTeleports(Player player) {
        return teleports.get(player);
    }
}
