package ac.grim.grimac.checks.impl.flight;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PacketCheck;
import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.GameMode;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerPosition;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerPositionAndRotation;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;

@CheckData(name = "FlightB")
public class FlightB extends Check implements PacketCheck {
    private int tp = 150;

    public FlightB(GrimPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.PLAYER_POSITION_AND_ROTATION) {
            WrapperPlayClientPlayerPositionAndRotation pkt = new WrapperPlayClientPlayerPositionAndRotation(event);
            PacketData pd = new PacketData(
                    pkt.getLocation().getX(), pkt.getLocation().getY(), pkt.getLocation().getZ(),
                    pkt.getYaw(), pkt.getPitch(), pkt.getServerVersion()
            );
            detect(pd);
        } else if (event.getPacketType() == PacketType.Play.Client.PLAYER_POSITION) {
            WrapperPlayClientPlayerPosition pkt = new WrapperPlayClientPlayerPosition(event);
            PacketData pd = new PacketData(
                    pkt.getLocation().getX(), pkt.getLocation().getY(), pkt.getLocation().getZ(),
                    pkt.getLocation().getYaw(), pkt.getLocation().getPitch(), pkt.getServerVersion()
            );
            detect(pd);
        }
    }

    // idk
    private void detect(PacketData pd) {
        if (player.bukkitPlayer == null) return;
        Player bukkit = player.bukkitPlayer;
        try {
            if (!player.gamemode.equals(GameMode.CREATIVE)
                    && !player.gamemode.equals(GameMode.SPECTATOR)
                    && !player.isGliding) {
                if (player.bukkitPlayer.isInsideVehicle()) return;

                if (Math.abs(pd.pitch) > 90.0f) {
                    flag();
                    return;
                }
                // 1.18+ versions increased minimum y value
                if (pd.y < -8.0 && pd.ver.isNewerThanOrEquals(ServerVersion.V_1_12) && pd.ver.isOlderThan(ServerVersion.V_1_18)){
                    if (chorusFruit()) return;
                    flag();
                    return;
                }

                Location previous = bukkit.getLocation();
                previous.setY(0);
                Location current = new Location(previous.getWorld(), pd.x, 0, pd.z, pd.yaw, pd.pitch);
                double distanceHorizontal = previous.distanceSquared(current);
                double distanceVertical = pd.y - bukkit.getLocation().getY();
                double maxDistanceHorizontal = this.tp;

                if (distanceHorizontal > maxDistanceHorizontal && !bukkit.isGliding() && !bukkit.isInsideVehicle()) {
                    flag();
                    return;
                }
                if ((distanceVertical < -150.0 || distanceVertical >= 300.0) && !bukkit.isGliding() && !bukkit.isInsideVehicle()) {
                    if (distanceVertical < -150.0) {
                        if (chorusFruit()) return;
                    }
                    flag();
                }
            }
        } catch (Exception ignored) {}
    }

    private boolean chorusFruit() {
        try {
            if (player.bukkitPlayer != null) {
                Player p = player.bukkitPlayer;
                return p.getInventory().getItemInMainHand().getType() == Material.CHORUS_FRUIT || p.getInventory().getItemInMainHand().getType() == Material.CHORUS_FRUIT;
            }
        } catch (Throwable t) {
            return false;
        }
        return false;
    }

    static class PacketData {
        public final double x, y, z;
        public final float yaw, pitch;
        public final ServerVersion ver;

        public PacketData(double x, double y, double z, float yaw, float pitch, ServerVersion sv) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.yaw = yaw;
            this.pitch = pitch;
            ver = sv;
        }
    }
}
