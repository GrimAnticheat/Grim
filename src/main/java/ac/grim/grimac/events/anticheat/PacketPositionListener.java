package ac.grim.grimac.events.anticheat;

import ac.grim.grimac.GrimAC;
import ac.grim.grimac.GrimPlayer;
import ac.grim.grimac.checks.movement.MovementCheckRunner;
import ac.grim.grimac.utils.data.PredictionData;
import io.github.retrooper.packetevents.event.PacketListenerDynamic;
import io.github.retrooper.packetevents.event.impl.PacketPlayReceiveEvent;
import io.github.retrooper.packetevents.event.priority.PacketEventPriority;
import io.github.retrooper.packetevents.packettype.PacketType;
import io.github.retrooper.packetevents.packetwrappers.play.in.flying.WrappedPacketInFlying;

public class PacketPositionListener extends PacketListenerDynamic {
    public PacketPositionListener() {
        super(PacketEventPriority.MONITOR);
    }

    @Override
    public void onPacketPlayReceive(PacketPlayReceiveEvent event) {
        byte packetID = event.getPacketId();

        if (packetID == PacketType.Play.Client.POSITION) {
            WrappedPacketInFlying position = new WrappedPacketInFlying(event.getNMSPacket());
            GrimPlayer grimPlayer = GrimAC.playerGrimHashMap.get(event.getPlayer());

            MovementCheckRunner.addQueuedPrediction(new PredictionData(GrimAC.playerGrimHashMap.get(event.getPlayer()), position.getX(), position.getY(), position.getZ(), grimPlayer.xRot, grimPlayer.yRot, position.isOnGround()));
        }

        if (packetID == PacketType.Play.Client.POSITION_LOOK) {
            WrappedPacketInFlying position = new WrappedPacketInFlying(event.getNMSPacket());

            MovementCheckRunner.addQueuedPrediction(new PredictionData(GrimAC.playerGrimHashMap.get(event.getPlayer()), position.getX(), position.getY(), position.getZ(), position.getYaw(), position.getPitch(), position.isOnGround()));
        }

        if (packetID == PacketType.Play.Client.LOOK) {
            WrappedPacketInFlying position = new WrappedPacketInFlying(event.getNMSPacket());
            GrimPlayer grimPlayer = GrimAC.playerGrimHashMap.get(event.getPlayer());

            MovementCheckRunner.addQueuedPrediction(new PredictionData(GrimAC.playerGrimHashMap.get(event.getPlayer()), grimPlayer.x, grimPlayer.y, grimPlayer.z, position.getYaw(), position.getPitch(), position.isOnGround()));
        }

        if (packetID == PacketType.Play.Client.FLYING) {
            WrappedPacketInFlying position = new WrappedPacketInFlying(event.getNMSPacket());
            GrimPlayer grimPlayer = GrimAC.playerGrimHashMap.get(event.getPlayer());

            MovementCheckRunner.addQueuedPrediction(new PredictionData(GrimAC.playerGrimHashMap.get(event.getPlayer()), grimPlayer.x, grimPlayer.y, grimPlayer.z, grimPlayer.xRot, grimPlayer.yRot, position.isOnGround()));
        }
    }
}