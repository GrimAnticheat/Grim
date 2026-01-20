package ac.grim.grimac.events.packets;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.type.PacketCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.worldborder.BorderExtent;
import ac.grim.grimac.utils.worldborder.RealTimeMovingBorderExtent;
import ac.grim.grimac.utils.worldborder.StaticBorderExtent;
import ac.grim.grimac.utils.worldborder.TickBasedMovingBorderExtent;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerInitializeWorldBorder;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerWorldBorder;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerWorldBorderCenter;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerWorldBorderSize;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayWorldBorderLerpSize;
import lombok.Getter;
import org.jetbrains.annotations.Contract;

public class PacketWorldBorder extends Check implements PacketCheck {
    @Getter
    private double centerX;
    @Getter
    private double centerZ;
    @Getter
    private double absoluteMaxSize;
    @Getter
    private BorderExtent extent;

    public PacketWorldBorder(GrimPlayer playerData) {
        super(playerData);
        this.extent = new StaticBorderExtent(5.999997E7);
    }

    public double getCurrentDiameter() {
        return extent.size();
    }

    public double getMinX() {
        return extent.getMinX(centerX, absoluteMaxSize);
    }

    public double getMaxX() {
        return extent.getMaxX(centerX, absoluteMaxSize);
    }

    public double getMinZ() {
        return extent.getMinZ(centerZ, absoluteMaxSize);
    }

    public double getMaxZ() {
        return extent.getMaxZ(centerZ, absoluteMaxSize);
    }

    public void tickBorder() {
        extent = extent.tick();
    }

    @Override
    public void onPacketSend(PacketSendEvent event) {
        if (event.getPacketType() == PacketType.Play.Server.WORLD_BORDER) {
            WrapperPlayServerWorldBorder packet = new WrapperPlayServerWorldBorder(event);

            player.sendTransaction();
            // Names are misleading, it's diameter not radius.
            if (packet.getAction() == WrapperPlayServerWorldBorder.WorldBorderAction.SET_SIZE) {
                double size = packet.getRadius();
                player.addRealTimeTaskNow(() -> setSize(size));
            } else if (packet.getAction() == WrapperPlayServerWorldBorder.WorldBorderAction.LERP_SIZE) {
                double oldDiameter = packet.getOldRadius();
                double newDiameter = packet.getNewRadius();
                long speed = packet.getSpeed();
                player.addRealTimeTaskNow(() -> setLerp(oldDiameter, newDiameter, speed));
            } else if (packet.getAction() == WrapperPlayServerWorldBorder.WorldBorderAction.SET_CENTER) {
                double centerX = packet.getCenterX();
                double centerZ = packet.getCenterZ();
                player.addRealTimeTaskNow(() -> setCenter(centerX, centerZ));
            } else if (packet.getAction() == WrapperPlayServerWorldBorder.WorldBorderAction.INITIALIZE) {
                double centerX = packet.getCenterX();
                double centerZ = packet.getCenterZ();
                double oldDiameter = packet.getOldRadius();
                double newDiameter = packet.getNewRadius();
                long speed = packet.getSpeed();
                int portalTeleportBoundary = packet.getPortalTeleportBoundary();
                player.addRealTimeTaskNow(() -> {
                    setCenter(centerX, centerZ);
                    setLerp(oldDiameter, newDiameter, speed);
                    absoluteMaxSize = portalTeleportBoundary;
                });
            }
        }

        if (event.getPacketType() == PacketType.Play.Server.INITIALIZE_WORLD_BORDER) {
            player.sendTransaction();
            WrapperPlayServerInitializeWorldBorder packet = new WrapperPlayServerInitializeWorldBorder(event);
            double centerX = packet.getX();
            double centerZ = packet.getZ();
            double oldDiameter = packet.getOldDiameter();
            double newDiameter = packet.getNewDiameter();
            long speed = packet.getSpeed();
            int portalTeleportBoundary = packet.getPortalTeleportBoundary();
            player.addRealTimeTaskNow(() -> {
                setCenter(centerX, centerZ);
                setLerp(oldDiameter, newDiameter, speed);
                absoluteMaxSize = portalTeleportBoundary;
            });
        }

        if (event.getPacketType() == PacketType.Play.Server.WORLD_BORDER_CENTER) {
            player.sendTransaction();
            WrapperPlayServerWorldBorderCenter packet = new WrapperPlayServerWorldBorderCenter(event);
            double centerX = packet.getX();
            double centerZ = packet.getZ();
            player.addRealTimeTaskNow(() -> setCenter(centerX, centerZ));
        }

        if (event.getPacketType() == PacketType.Play.Server.WORLD_BORDER_SIZE) {
            player.sendTransaction();
            double size = new WrapperPlayServerWorldBorderSize(event).getDiameter();
            player.addRealTimeTaskNow(() -> setSize(size));
        }

        if (event.getPacketType() == PacketType.Play.Server.WORLD_BORDER_LERP_SIZE) {
            player.sendTransaction();
            WrapperPlayWorldBorderLerpSize packet = new WrapperPlayWorldBorderLerpSize(event);
            double oldDiameter = packet.getOldDiameter();
            double newDiameter = packet.getNewDiameter();
            long speed = packet.getSpeed();
            player.addRealTimeTaskNow(() -> setLerp(oldDiameter, newDiameter, speed));
        }
    }

    @Contract(mutates = "this")
    private void setCenter(double x, double z) {
        centerX = x;
        centerZ = z;
    }

    @Contract(mutates = "this")
    private void setSize(double size) {
        this.extent = new StaticBorderExtent(size);
    }

    @Contract(mutates = "this")
    private void setLerp(double oldDiameter, double newDiameter, long speed) {
        if (speed <= 0 || oldDiameter == newDiameter) {
            this.extent = new StaticBorderExtent(newDiameter);
        } else {
            this.extent = createMovingExtent(oldDiameter, newDiameter, speed);
        }
    }

    private static final boolean SERVER_TICK_BASED = PacketEvents.getAPI().getServerManager().getVersion().isNewerThanOrEquals(ServerVersion.V_1_21_11);

    private BorderExtent createMovingExtent(double from, double to, long speed) {
        if (player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_21_11)) { // tick-based
            long durationTicks = SERVER_TICK_BASED ? speed : (speed / 50);
            return new TickBasedMovingBorderExtent(from, to, durationTicks);
        } else { // real-time based
            long durationMs = SERVER_TICK_BASED ? (speed * 50) : speed;
            return new RealTimeMovingBorderExtent(from, to, durationMs);
        }
    }

}
