package ac.grim.grimac.events.packets;

import ac.grim.grimac.checks.type.PacketCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.math.GrimMath;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.server.*;

public class PacketWorldBorder extends PacketCheck {
    double centerX;
    double centerZ;
    double oldDiameter;
    double newDiameter;
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
    public void onPacketSend(PacketSendEvent event) {
        if (event.getPacketType() == PacketType.Play.Server.WORLD_BORDER) {
            WrapperPlayServerWorldBorder packet = new WrapperPlayServerWorldBorder(event);

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
            }

        }
        if (event.getPacketType() == PacketType.Play.Server.INITIALIZE_WORLD_BORDER) {
            WrapperPlayServerInitializeWorldBorder border = new WrapperPlayServerInitializeWorldBorder(event);
            setCenter(border.getX(), border.getZ());
            setLerp(border.getOldDiameter(), border.getNewDiameter(), border.getSpeed());
        }

        if (event.getPacketType() == PacketType.Play.Server.WORLD_BORDER_CENTER) {
            WrapperPlayServerWorldBorderCenter center = new WrapperPlayServerWorldBorderCenter(event);
            setCenter(center.getX(), center.getZ());
        }

        if (event.getPacketType() == PacketType.Play.Server.WORLD_BORDER_SIZE) {
            WrapperPlayServerWorldBorderSize size = new WrapperPlayServerWorldBorderSize(event);
            setSize(size.getDiameter());
        }

        if (event.getPacketType() == PacketType.Play.Server.WORLD_BORDER_LERP_SIZE) {
            WrapperPlayWorldBorderLerpSize size = new WrapperPlayWorldBorderLerpSize(event);
            setLerp(size.getOldDiameter(), size.getNewDiameter(), size.getSpeed());
        }
    }

    private void setCenter(double x, double z) {
        player.sendTransaction();
        player.latencyUtils.addRealTimeTask(player.lastTransactionSent.get(), () -> {
            centerX = x;
            centerZ = z;
        });
    }

    private void setSize(double size) {
        player.sendTransaction();
        player.latencyUtils.addRealTimeTask(player.lastTransactionSent.get(), () -> {
            oldDiameter = size;
            newDiameter = size;
        });
    }

    private void setLerp(double oldDiameter, double newDiameter, long length) {
        player.sendTransaction();
        player.latencyUtils.addRealTimeTask(player.lastTransactionSent.get(), () -> {
            this.oldDiameter = oldDiameter;
            this.newDiameter = newDiameter;
            this.startTime = System.currentTimeMillis();
            this.endTime = this.startTime + length;
        });
    }
}
