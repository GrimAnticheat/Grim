package ac.grim.grimac.events.packets;

import ac.grim.grimac.checks.AbstractPacketCheck;
import ac.grim.grimac.checks.PacketHandlerRegistry;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.math.GrimMath;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.server.*;

public class PacketWorldBorder extends AbstractPacketCheck {
    double centerX;
    double centerZ;
    double oldDiameter;
    double newDiameter;
    double absoluteMaxSize;
    long startTime = 1;
    long endTime = 1;

    public PacketWorldBorder(GrimPlayer playerData) {
        super(playerData);
    }

    public double getCenterX() {
        return centerX;
    }

    public double getCenterZ() {
        return centerZ;
    }

    public double getCurrentDiameter() {
        double d0 = (double) (System.currentTimeMillis() - this.startTime) / ((double) this.endTime - this.startTime);
        return d0 < 1.0D ? GrimMath.lerp(d0, oldDiameter, newDiameter) : newDiameter;
    }

    @Override
    protected void registerSendHandlers(PacketHandlerRegistry<PacketSendEvent> registry) {
        registry.registerHandler(event -> {
            WrapperPlayServerWorldBorder packet = new WrapperPlayServerWorldBorder(event);

            player.sendTransaction();
            // Names are misleading, it's diameter not radius.
            if (packet.getAction() == WrapperPlayServerWorldBorder.WorldBorderAction.SET_SIZE) {
                setSize(packet.getRadius());
            } else if (packet.getAction() == WrapperPlayServerWorldBorder.WorldBorderAction.LERP_SIZE) {
                setLerp(packet.getOldRadius(), packet.getNewRadius(), packet.getSpeed());
            } else if (packet.getAction() == WrapperPlayServerWorldBorder.WorldBorderAction.SET_CENTER) {
                setCenter(packet.getCenterX(), packet.getCenterZ());
            } else if (packet.getAction() == WrapperPlayServerWorldBorder.WorldBorderAction.INITIALIZE) {
                setCenter(packet.getCenterX(), packet.getCenterZ());
                setLerp(packet.getOldRadius(), packet.getNewRadius(), packet.getSpeed());
                setAbsoluteMaxSize(packet.getPortalTeleportBoundary());
            }
        }, PacketType.Play.Server.WORLD_BORDER);
        registry.registerHandler(event -> {
            player.sendTransaction();
            WrapperPlayServerInitializeWorldBorder border = new WrapperPlayServerInitializeWorldBorder(event);
            setCenter(border.getX(), border.getZ());
            setLerp(border.getOldDiameter(), border.getNewDiameter(), border.getSpeed());
            setAbsoluteMaxSize(border.getPortalTeleportBoundary());
        }, PacketType.Play.Server.INITIALIZE_WORLD_BORDER);
        registry.registerHandler(event -> {
            player.sendTransaction();
            WrapperPlayServerWorldBorderCenter center = new WrapperPlayServerWorldBorderCenter(event);
            setCenter(center.getX(), center.getZ());
        }, PacketType.Play.Server.WORLD_BORDER_CENTER);
        registry.registerHandler(event -> {
            player.sendTransaction();
            WrapperPlayServerWorldBorderSize size = new WrapperPlayServerWorldBorderSize(event);
            setSize(size.getDiameter());
        }, PacketType.Play.Server.WORLD_BORDER_SIZE);
        registry.registerHandler(event -> {
            player.sendTransaction();
            WrapperPlayWorldBorderLerpSize size = new WrapperPlayWorldBorderLerpSize(event);
            setLerp(size.getOldDiameter(), size.getNewDiameter(), size.getSpeed());
        }, PacketType.Play.Server.WORLD_BORDER_LERP_SIZE);
    }

    private void setCenter(double x, double z) {
        player.latencyUtils.addRealTimeTask(player.lastTransactionSent.get(), () -> {
            centerX = x;
            centerZ = z;
        });
    }

    private void setSize(double size) {
        player.latencyUtils.addRealTimeTask(player.lastTransactionSent.get(), () -> {
            oldDiameter = size;
            newDiameter = size;
        });
    }

    private void setLerp(double oldDiameter, double newDiameter, long length) {
        player.latencyUtils.addRealTimeTask(player.lastTransactionSent.get(), () -> {
            this.oldDiameter = oldDiameter;
            this.newDiameter = newDiameter;
            this.startTime = System.currentTimeMillis();
            this.endTime = this.startTime + length;
        });
    }

    private void setAbsoluteMaxSize(double absoluteMaxSize) {
        player.latencyUtils.addRealTimeTask(player.lastTransactionSent.get(), () -> this.absoluteMaxSize = absoluteMaxSize);
    }

    public double getAbsoluteMaxSize() {
        return absoluteMaxSize;
    }
}
