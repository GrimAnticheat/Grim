package ac.grim.grimac.events.packets.patch;

import ac.grim.grimac.checks.type.PacketCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.blockstate.BaseBlockState;
import ac.grim.grimac.utils.blockstate.FlatBlockState;
import ac.grim.grimac.utils.blockstate.MagicBlockState;
import ac.grim.grimac.utils.collisions.datatypes.SimpleCollisionBox;
import ac.grim.grimac.utils.data.BasePlayerChangeBlockData;
import ac.grim.grimac.utils.data.PlayerOpenBlockData;
import ac.grim.grimac.utils.math.GrimMath;
import io.github.retrooper.packetevents.event.impl.PacketPlaySendEvent;
import io.github.retrooper.packetevents.utils.pair.Pair;
import io.github.retrooper.packetevents.utils.server.ServerVersion;
import io.github.retrooper.packetevents.utils.vector.Vector3i;
import org.bukkit.Location;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Predicate;

public class ResyncWorldUtil extends PacketCheck {
    Queue<Runnable> toExecute = new ConcurrentLinkedQueue<>();

    public ResyncWorldUtil(GrimPlayer playerData) {
        super(playerData);
    }

    public void resyncPositions(GrimPlayer player, SimpleCollisionBox box) {
        resyncPositions(player, box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ);
    }

    public void resyncPositions(GrimPlayer player, double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        resyncPositions(player, GrimMath.floor(minX), GrimMath.floor(minY), GrimMath.floor(minZ),
                GrimMath.floor(maxX), GrimMath.floor(maxY), GrimMath.floor(maxZ), material -> true);
    }

    public void resyncPositions(GrimPlayer player, int minX, int minY, int minZ, int maxX, int maxY, int maxZ, Predicate<Pair<BaseBlockState, Vector3i>> shouldSend) {
        toExecute.add(() -> {
            int[][][] blocks = new int[maxX - minX + 1][maxY - minY + 1][maxZ - minZ + 1];

            for (int x = minX; x <= maxX; x++) {
                for (int y = minY; y <= maxY; y++) {
                    for (int z = minZ; z <= maxZ; z++) {
                        // Don't resend blocks if the chunk isn't loaded
                        if (!player.compensatedWorld.isChunkLoaded(x >> 4, z >> 4)) return;

                        blocks[x - minX][y - minY][z - minZ] = player.compensatedWorld.getWrappedBlockStateAt(x, y, z).getCombinedId();
                    }
                }
            }

            for (BasePlayerChangeBlockData changeBlockData : player.compensatedWorld.worldChangedBlockQueue) {
                if (changeBlockData instanceof PlayerOpenBlockData) continue; // Server will resync this later
                if (changeBlockData.blockX >= minX && changeBlockData.blockX <= maxX &&
                        changeBlockData.blockY >= minY && changeBlockData.blockY <= maxY &&
                        changeBlockData.blockZ >= minZ && changeBlockData.blockZ <= maxZ) { // in range
                    // Update this block data to latest to stop any desync's
                    blocks[changeBlockData.blockX - minX][changeBlockData.blockY - minY][changeBlockData.blockZ - minZ] = changeBlockData.getCombinedID();
                } // 526 4 -332
            }

            try {
                player.compensatedWorld.isResync = true;

                // Maybe in the future chunk changes could be sent, but those have a decent amount of version differences
                // Works for now, maybe will fix later, maybe won't.
                //
                // Currently, neither Bukkit nor PacketEvents supports sending these packets (Bukkit broke this in 1.16 (?) making this method useless to us)
                for (int x = minX; x <= maxX; x++) {
                    for (int y = minY; y <= maxY; y++) {
                        for (int z = minZ; z <= maxZ; z++) {
                            if (ServerVersion.getVersion().isNewerThanOrEquals(ServerVersion.v_1_13)) {
                                FlatBlockState state = new FlatBlockState(blocks[x - minX][y - minY][z - minZ]);
                                if (shouldSend.test(new Pair<>(state, new Vector3i(x, y, z)))) {
                                    player.bukkitPlayer.sendBlockChange(new Location(player.bukkitPlayer.getWorld(), x, y, z), state.getBlockData());
                                    player.compensatedWorld.likelyDesyncBlockPositions.add(new Pair<>(player.lastTransactionSent.get(), new Vector3i(x, y, z)));
                                }
                            } else {
                                MagicBlockState state = new MagicBlockState(blocks[x - minX][y - minY][z - minZ]);
                                if (shouldSend.test(new Pair<>(state, new Vector3i(x, y, z)))) {
                                    player.bukkitPlayer.sendBlockChange(new Location(player.bukkitPlayer.getWorld(), x, y, z), state.getMaterial(), (byte) state.getBlockData());
                                    player.compensatedWorld.likelyDesyncBlockPositions.add(new Pair<>(player.lastTransactionSent.get(), new Vector3i(x, y, z)));
                                }
                            }
                        }
                    }
                }
            } finally {
                player.compensatedWorld.isResync = false;
            }

            player.sendAndFlushTransactionOrPingPong();
        });
    }

    public void onPacketSend(final PacketPlaySendEvent event) {
        Runnable next = toExecute.poll();
        if (next != null) {
            next.run();
        }
    }
}
