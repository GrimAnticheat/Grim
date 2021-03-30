package ac.grim.grimac.events.anticheat;

import ac.grim.grimac.checks.movement.MovementCheck;
import io.github.retrooper.packetevents.event.PacketListenerDynamic;
import io.github.retrooper.packetevents.event.priority.PacketEventPriority;

import java.util.ArrayList;
import java.util.List;

public class GenericMovementCheck extends PacketListenerDynamic {
    // Yeah... I know I lose a bit of performance from a list over a set, but it's worth it for consistency
    static List<MovementCheck> movementCheckListeners = new ArrayList<>();

    // YES I KNOW THIS CLASS IS TERRIBLE.
    // EARLIER TODAY I WANTED IT TO BE A MANAGER CLASS
    // LATER TODAY A CLASS THAT THINGS EXTEND
    // AND NOW IT'S BOTH SO THE CODE IS TERRIBLE!
    public GenericMovementCheck() {
        super(PacketEventPriority.MONITOR);
    }

    public static void registerCheck(MovementCheck movementCheck) {
        movementCheckListeners.add(movementCheck);
    }

    /*public void registerPackets() {
        manager.addPacketListener(new PacketAdapter(plugin, ListenerPriority.NORMAL, PacketType.Play.Client.POSITION) {
            @Override
            public void onPacketReceiving(PacketEvent event) {
                PacketContainer packet = event.getPacket();
                GrimPlayer player = GrimAC.playerGrimHashMap.get(event.getPlayer());
                double x = packet.getDoubles().read(0);
                double y = packet.getDoubles().read(1);
                double z = packet.getDoubles().read(2);
                boolean onGround = packet.getBooleans().read(0);

                check(player, x, y, z, player.lastXRot, player.lastYRot, onGround);
            }
        });

        manager.addPacketListener(new PacketAdapter(plugin, ListenerPriority.NORMAL, PacketType.Play.Client.POSITION_LOOK) {
            @Override
            public void onPacketReceiving(PacketEvent event) {
                PacketContainer packet = event.getPacket();
                GrimPlayer player = GrimAC.playerGrimHashMap.get(event.getPlayer());
                double x = packet.getDoubles().read(0);
                double y = packet.getDoubles().read(1);
                double z = packet.getDoubles().read(2);
                float xRot = packet.getFloat().read(0);
                float yRot = packet.getFloat().read(1);
                boolean onGround = packet.getBooleans().read(0);

                check(player, x, y, z, xRot, yRot, onGround);
            }
        });

        manager.addPacketListener(new PacketAdapter(plugin, ListenerPriority.NORMAL, PacketType.Play.Client.LOOK) {
            @Override
            public void onPacketReceiving(PacketEvent event) {
                PacketContainer packet = event.getPacket();
                GrimPlayer player = GrimAC.playerGrimHashMap.get(event.getPlayer());
                float xRot = packet.getFloat().read(0);
                float yRot = packet.getFloat().read(1);
                boolean onGround = packet.getBooleans().read(0);

                check(player, player.lastX, player.lastY, player.lastZ, xRot, yRot, onGround);
            }
        });

        manager.addPacketListener(new PacketAdapter(plugin, ListenerPriority.NORMAL, PacketType.Play.Client.FLYING) {
            @Override
            public void onPacketReceiving(PacketEvent event) {
                PacketContainer packet = event.getPacket();
                GrimPlayer player = GrimAC.playerGrimHashMap.get(event.getPlayer());
                boolean onGround = packet.getBooleans().read(0);

                check(player, player.lastX, player.lastY, player.lastZ, player.lastXRot, player.lastYRot, onGround);
            }
        });
    }

    public void check(GrimPlayer grimPlayer, double x, double y, double z, float xRot, float yRot, boolean onGround) {
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

        // To get the velocity of the player in the beginning of the next tick
        // We need to run the code that is ran after the movement is applied to the player
        // We do it at the start of the next movement check where the movement is applied
        // This allows the check to be more accurate than if we were a tick off on the player position
        //
        // Currently disabled because I'd rather know if something is wrong than try and hide it
        //grimPlayer.clientVelocity = move(MoverType.SELF, grimPlayer.lastActualMovement, false);

        // With 0 ping I haven't found ANY margin of error
        // Very useful for reducing x axis effect on y axis precision
        // Since the Y axis is extremely easy to predict
        // It once is different if the player is trying to clip through stuff
        //
        // This would error when the player has mob collision
        // I should probably separate mob and block collision
        grimPlayer.actualMovementCalculatedCollision = Collisions.collide(Collisions.maybeBackOffFromEdge(grimPlayer.actualMovement.clone(), MoverType.SELF, grimPlayer), grimPlayer);

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


        grimPlayer.predictedVelocity.setY(0);
        grimPlayer.clientVelocity.setY(0);

        Bukkit.broadcastMessage("Time since last event " + (grimPlayer.movementEventMilliseconds - grimPlayer.lastMovementEventMilliseconds));
        Bukkit.broadcastMessage("P: " + color + grimPlayer.predictedVelocity.getX() + " " + grimPlayer.predictedVelocity.getY() + " " + grimPlayer.predictedVelocity.getZ());
        Bukkit.broadcastMessage("A: " + color + grimPlayer.actualMovement.getX() + " " + grimPlayer.actualMovement.getY() + " " + grimPlayer.actualMovement.getZ());


        // TODO: This is a check for is the player actually on the ground!
        // TODO: This check is wrong with less 1.9+ precision on movement
        // mainly just debug for now rather than an actual check
        /*if (grimPlayer.isActuallyOnGround != grimPlayer.lastOnGround) {
            Bukkit.broadcastMessage("Failed on ground, client believes: " + grimPlayer.onGround);
        }*/

        /*if (grimPlayer.predictedVelocity.distanceSquared(grimPlayer.actualMovement) > new Vector(0.03, 0.03, 0.03).lengthSquared()) {
            //Bukkit.broadcastMessage(ChatColor.RED + "FAILED MOVEMENT CHECK");
        }

        grimPlayer.lastActualMovement = grimPlayer.actualMovement;

        // TODO: This is a terrible hack
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
    }*/
}
