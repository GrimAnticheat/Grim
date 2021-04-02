package ac.grim.grimac.events.anticheat;

import ac.grim.grimac.GrimAC;
import ac.grim.grimac.GrimPlayer;
import ac.grim.grimac.checks.movement.MovementCheck;
import ac.grim.grimac.utils.chunks.ChunkCache;
import ac.grim.grimac.utils.enums.MoverType;
import ac.grim.grimac.utils.nmsImplementations.Collisions;
import io.github.retrooper.packetevents.event.PacketListenerDynamic;
import io.github.retrooper.packetevents.event.impl.PacketPlayReceiveEvent;
import io.github.retrooper.packetevents.event.priority.PacketEventPriority;
import io.github.retrooper.packetevents.packettype.PacketType;
import io.github.retrooper.packetevents.packetwrappers.play.in.flying.WrappedPacketInFlying;
import net.minecraft.server.v1_16_R3.Block;
import net.minecraft.server.v1_16_R3.IBlockData;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class GenericMovementCheck extends PacketListenerDynamic {
    // Yeah... I know I lose a bit of performance from a list over a set, but it's worth it for consistency
    static List<MovementCheck> movementCheckListeners = new ArrayList<>();

    public GenericMovementCheck() {
        super(PacketEventPriority.MONITOR);
    }

    @Override
    public void onPacketPlayReceive(PacketPlayReceiveEvent event) {
        byte packetID = event.getPacketId();
        if (packetID == PacketType.Play.Client.POSITION) {
            WrappedPacketInFlying position = new WrappedPacketInFlying(event.getNMSPacket());

            int playerX = (int) position.getX();
            int playerZ = (int) position.getZ();

            final List<IBlockData> materials = new LinkedList<>();

            Long startTime = System.nanoTime();

            try {
                for (int x = 0; x < 16; x++) {
                    for (int y = 0; y < 128; y++) {
                        for (int z = 0; z < 16; z++) {
                            materials.add(Block.getByCombinedId(ChunkCache.getBlockAt(playerX + x, y, playerZ + z)));
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            //Bukkit.broadcastMessage("Listening to chunks " + (System.nanoTime() - startTime) + " " + materials.size());

            Bukkit.getScheduler().runTask(GrimAC.plugin, () -> {
                check(GrimAC.playerGrimHashMap.get(event.getPlayer()), position.getX(), position.getY(), position.getZ(), position.getPitch(), position.getYaw(), position.isOnGround());
            });


            //Bukkit.broadcastMessage("Final block type " + output);
        }
    }
        /*manager.addPacketListener(new PacketAdapter(plugin, ListenerPriority.NORMAL, PacketType.Play.Client.POSITION) {
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

    }*/

    public void check(GrimPlayer grimPlayer, double x, double y, double z, float xRot, float yRot, boolean onGround) {
        CompletableFuture.runAsync(() -> {
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
            // TODO: This is just here right now to debug collisions
            final List<Vector> collisions = new LinkedList<>();

            Long startTime = System.nanoTime();

            for (int i = 0; i < Integer.MAX_VALUE; i++) {
                collisions.add(Collisions.collide(Collisions.maybeBackOffFromEdge(new Vector(1, -1, 1), MoverType.SELF, grimPlayer), grimPlayer));
            }

            Bukkit.broadcastMessage("Time taken " + (System.nanoTime() - startTime) + " " + collisions.size());

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
        });

        // This is not affected by any movement
        /*new PlayerBaseTick(grimPlayer).doBaseTick();

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

    }
}*/
    }
}