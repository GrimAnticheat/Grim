package ac.grim.grimac.checks.impl.misc;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PacketCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.math.GrimMath;
import ac.grim.grimac.utils.nmsutil.BlockBreakSpeed;
import io.github.retrooper.packetevents.util.FoliaCompatUtil;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.player.DiggingAction;
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import com.github.retrooper.packetevents.protocol.world.states.type.StateTypes;
import com.github.retrooper.packetevents.util.Vector3i;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerDigging;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerAcknowledgeBlockChanges;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerBlockChange;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

// Based loosely off of Hawk BlockBreakSpeedSurvival
// Also based loosely off of NoCheatPlus FastBreak
// Also based off minecraft wiki: https://minecraft.fandom.com/wiki/Breaking#Instant_breaking
@CheckData(name = "FastBreak")
public class FastBreak extends Check implements PacketCheck {
    public FastBreak(GrimPlayer playerData) {
        super(playerData);
    }

    // The block the player is currently breaking
    Vector3i targetBlock = null;
    // The maximum amount of damage the player deals to the block
    //
    double maximumBlockDamage = 0;
    // The last time a finish digging packet was sent, to enforce 0.3-second delay after non-instabreak
    long lastFinishBreak = 0;
    // The time the player started to break the block, to know how long the player waited until they finished breaking the block
    long startBreak = 0;

    // The buffer to this check
    double blockBreakBalance = 0;
    double blockDelayBalance = 0;

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        // Find the most optimal block damage using the animation packet, which is sent at least once a tick when breaking blocks
        // On 1.8 clients, via screws with this packet meaning we must fall back to the 1.8 idle flying packet
        if ((player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_9) ? event.getPacketType() == PacketType.Play.Client.ANIMATION : WrapperPlayClientPlayerFlying.isFlying(event.getPacketType())) && targetBlock != null) {
            maximumBlockDamage = Math.max(maximumBlockDamage, BlockBreakSpeed.getBlockDamage(player, targetBlock));
        }

        if (event.getPacketType() == PacketType.Play.Client.PLAYER_DIGGING) {
            WrapperPlayClientPlayerDigging digging = new WrapperPlayClientPlayerDigging(event);

            if (digging.getAction() == DiggingAction.START_DIGGING) {
                WrappedBlockState block = player.compensatedWorld.getWrappedBlockStateAt(digging.getBlockPosition());
                
                // Exempt all blocks that do not exist in the player version
                if (WrappedBlockState.getDefaultState(player.getClientVersion(), block.getType()).getType() == StateTypes.AIR) {
                    return;
                }
            
                startBreak = System.currentTimeMillis() - (targetBlock == null ? 50 : 0); // ???
                targetBlock = digging.getBlockPosition();
                
                maximumBlockDamage = BlockBreakSpeed.getBlockDamage(player, targetBlock);

                double breakDelay = System.currentTimeMillis() - lastFinishBreak;

                if (breakDelay >= 275) { // Reduce buffer if "close enough"
                    blockDelayBalance *= 0.9;
                } else { // Otherwise, increase buffer
                    blockDelayBalance += 300 - breakDelay;
                }

                if (blockDelayBalance > 1000 && shouldModifyPackets()) { // If more than a second of advantage
                    event.setCancelled(true); // Cancelling start digging will cause server to reject block break
                    player.onPacketCancel();
                    flagAndAlert("Delay=" + breakDelay);
                }

                clampBalance();
            }

            if (digging.getAction() == DiggingAction.FINISHED_DIGGING && targetBlock != null) {
                double predictedTime = Math.ceil(1 / maximumBlockDamage) * 50;
                double realTime = System.currentTimeMillis() - startBreak;
                double diff = predictedTime - realTime;

                clampBalance();

                if (diff < 25) {  // Reduce buffer if "close enough"
                    blockBreakBalance *= 0.9;
                } else { // Otherwise, increase buffer
                    blockBreakBalance += diff;
                }

                if (blockBreakBalance > 1000) { // If more than a second of advantage
                    FoliaCompatUtil.runTaskForEntity(player.bukkitPlayer, GrimAPI.INSTANCE.getPlugin(), () -> {
                        Player bukkitPlayer = player.bukkitPlayer;
                        if (bukkitPlayer == null || !bukkitPlayer.isOnline()) return;

                        if (bukkitPlayer.getLocation().distance(new Location(bukkitPlayer.getWorld(), digging.getBlockPosition().getX(), digging.getBlockPosition().getY(), digging.getBlockPosition().getZ())) < 64) {
                            Chunk chunk = bukkitPlayer.getWorld().getChunkAt(digging.getBlockPosition().getX() >> 4, digging.getBlockPosition().getZ() >> 4);
                            if (!chunk.isLoaded()) return; // Don't load chunks sync

                            Block block = chunk.getBlock(digging.getBlockPosition().getX() & 15, digging.getBlockPosition().getY(), digging.getBlockPosition().getZ() & 15);

                            int blockId;

                            if (PacketEvents.getAPI().getServerManager().getVersion().isNewerThanOrEquals(ServerVersion.V_1_13)) {
                                // Cache this because strings are expensive
                                blockId = WrappedBlockState.getByString(PacketEvents.getAPI().getServerManager().getVersion().toClientVersion(), block.getBlockData().getAsString(false)).getGlobalId();
                            } else {
                                blockId = (block.getType().getId() << 4) | block.getData();
                            }

                            player.user.sendPacket(new WrapperPlayServerBlockChange(digging.getBlockPosition(), blockId));

                            if (PacketEvents.getAPI().getServerManager().getVersion().isNewerThanOrEquals(ServerVersion.V_1_19)) { // Via will handle this for us pre-1.19
                                player.user.sendPacket(new WrapperPlayServerAcknowledgeBlockChanges(digging.getSequence())); // Make 1.19 clients apply the changes
                            }
                        }
                    }, null, 0);

                    if (flagAndAlert("Diff=" + diff + ",Balance=" + blockBreakBalance) && shouldModifyPackets()) {
                        event.setCancelled(true);
                        player.onPacketCancel();
                    }
                }

                lastFinishBreak = System.currentTimeMillis();
            }

            if (digging.getAction() == DiggingAction.CANCELLED_DIGGING) {
                targetBlock = null;
            }
        }
    }

    private void clampBalance() {
        double balance = Math.max(1000, (player.getTransactionPing()));
        blockBreakBalance = GrimMath.clamp(blockBreakBalance, -balance, balance); // Clamp not Math.max in case other logic changes
        blockDelayBalance = GrimMath.clamp(blockDelayBalance, -balance, balance);
    }
}
