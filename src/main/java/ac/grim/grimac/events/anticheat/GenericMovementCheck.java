package ac.grim.grimac.events.anticheat;

import ac.grim.grimac.GrimAC;
import ac.grim.grimac.GrimPlayer;
import ac.grim.grimac.checks.movement.MovementCheck;
import ac.grim.grimac.checks.movement.MovementVelocityCheck;
import io.github.retrooper.packetevents.event.PacketListenerDynamic;
import io.github.retrooper.packetevents.event.impl.PacketPlayReceiveEvent;
import io.github.retrooper.packetevents.event.priority.PacketEventPriority;
import io.github.retrooper.packetevents.packettype.PacketType;
import io.github.retrooper.packetevents.packetwrappers.play.in.flying.WrappedPacketInFlying;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

public class GenericMovementCheck extends PacketListenerDynamic {
    // Yeah... I know I lose a bit of performance from a list over a set, but it's worth it for consistency
    static List<MovementCheck> movementCheckListeners = new ArrayList<>();

    // I maxed out all threads with looping collisions and 4 seems to be the point before it hurts the main thread
    ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(4);

    public GenericMovementCheck() {
        super(PacketEventPriority.MONITOR);
    }

    @Override
    public void onPacketPlayReceive(PacketPlayReceiveEvent event) {
        byte packetID = event.getPacketId();
        if (packetID == PacketType.Play.Client.POSITION) {
            WrappedPacketInFlying position = new WrappedPacketInFlying(event.getNMSPacket());

            Bukkit.broadcastMessage("Position " + executor.toString());
            executor.submit(() -> check(GrimAC.playerGrimHashMap.get(event.getPlayer()), position.getX(), position.getY(), position.getZ(), position.getPitch(), position.getYaw(), position.isOnGround()));
        }

        if (packetID == PacketType.Play.Client.POSITION_LOOK) {
            WrappedPacketInFlying position = new WrappedPacketInFlying(event.getNMSPacket());

            Bukkit.broadcastMessage("Position look " + executor.toString());
            executor.submit(() -> check(GrimAC.playerGrimHashMap.get(event.getPlayer()), position.getX(), position.getY(), position.getZ(), position.getPitch(), position.getYaw(), position.isOnGround()));
        }

        if (packetID == PacketType.Play.Client.LOOK) {
            WrappedPacketInFlying position = new WrappedPacketInFlying(event.getNMSPacket());

            Bukkit.broadcastMessage("Look " + executor.toString());
            executor.submit(() -> check(GrimAC.playerGrimHashMap.get(event.getPlayer()), position.getX(), position.getY(), position.getZ(), position.getPitch(), position.getYaw(), position.isOnGround()));
        }

        if (packetID == PacketType.Play.Client.FLYING) {
            WrappedPacketInFlying position = new WrappedPacketInFlying(event.getNMSPacket());

            Bukkit.broadcastMessage("Flying " + executor.toString());
            executor.submit(() -> check(GrimAC.playerGrimHashMap.get(event.getPlayer()), position.getX(), position.getY(), position.getZ(), position.getPitch(), position.getYaw(), position.isOnGround()));
        }
    }

    public void check(GrimPlayer grimPlayer, double x, double y, double z, float xRot, float yRot, boolean onGround) {
        long startTime = System.nanoTime();

        grimPlayer.x = x;
        grimPlayer.y = y;
        grimPlayer.z = z;
        grimPlayer.xRot = xRot;
        grimPlayer.yRot = yRot;
        grimPlayer.onGround = onGround;
        grimPlayer.isSneaking = grimPlayer.bukkitPlayer.isSneaking();
        grimPlayer.movementPacketMilliseconds = System.currentTimeMillis();

        for (MovementCheck movementCheck : movementCheckListeners) {
            movementCheck.checkMovement(grimPlayer);
        }

        grimPlayer.movementEventMilliseconds = System.currentTimeMillis();

        Location from = new Location(grimPlayer.bukkitPlayer.getWorld(), grimPlayer.lastX, grimPlayer.lastY, grimPlayer.lastZ);
        Location to = new Location(grimPlayer.bukkitPlayer.getWorld(), grimPlayer.x, grimPlayer.y, grimPlayer.z);

        // This isn't the final velocity of the player in the tick, only the one applied to the player
        grimPlayer.actualMovement = new Vector(to.getX() - from.getX(), to.getY() - from.getY(), to.getZ() - from.getZ());

        // This is not affected by any movement
        new PlayerBaseTick(grimPlayer).doBaseTick();

        // baseTick occurs before this
        new MovementVelocityCheck(grimPlayer).livingEntityAIStep();

        ChatColor color;
        double diff = grimPlayer.predictedVelocity.distance(grimPlayer.actualMovement);

        if (diff < 0.05) {
            color = ChatColor.GREEN;
        } else if (diff < 0.15) {
            color = ChatColor.YELLOW;
        } else {
            color = ChatColor.RED;
        }

        Bukkit.broadcastMessage("Time since last event " + (grimPlayer.movementEventMilliseconds - grimPlayer.lastMovementEventMilliseconds + "Time taken " + (System.nanoTime() - startTime)));
        Bukkit.broadcastMessage("P: " + color + grimPlayer.predictedVelocity.getX() + " " + grimPlayer.predictedVelocity.getY() + " " + grimPlayer.predictedVelocity.getZ());
        Bukkit.broadcastMessage("A: " + color + grimPlayer.actualMovement.getX() + " " + grimPlayer.actualMovement.getY() + " " + grimPlayer.actualMovement.getZ());

        grimPlayer.lastX = x;
        grimPlayer.lastY = y;
        grimPlayer.lastZ = z;
        grimPlayer.lastXRot = xRot;
        grimPlayer.lastYRot = yRot;
        grimPlayer.lastOnGround = onGround;
        grimPlayer.lastSneaking = grimPlayer.isSneaking;
        grimPlayer.lastClimbing = grimPlayer.entityPlayer.isClimbing();
        grimPlayer.lastMovementPacketMilliseconds = grimPlayer.movementPacketMilliseconds;
        grimPlayer.lastMovementEventMilliseconds = grimPlayer.movementEventMilliseconds;
    }
}