package ac.grim.grimac.events.packets;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.type.PacketCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.math.GrimMath;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
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
    private double oldDiameter;
    private double newDiameter;
    @Getter
    private double absoluteMaxSize;
    private long startTime = 1;
    private long endTime = 1;

    public PacketWorldBorder(GrimPlayer playerData) {
        super(playerData);
    }

    public double getCurrentDiameter() {
        double d0 = (double) (System.currentTimeMillis() - startTime) / ((double) endTime - startTime);
        return d0 < 1.0D ? GrimMath.lerp(d0, oldDiameter, newDiameter) : newDiameter;
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
        oldDiameter = size;
        newDiameter = size;
    }

    @Contract(mutates = "this")
    private void setLerp(double oldDiameter, double newDiameter, long length) {
        this.oldDiameter = oldDiameter;
        this.newDiameter = newDiameter;
        this.startTime = System.currentTimeMillis();
        this.endTime = this.startTime + length;
    }
}
