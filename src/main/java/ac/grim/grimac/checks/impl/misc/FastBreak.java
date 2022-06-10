package ac.grim.grimac.checks.impl.misc;

import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PacketCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.math.GrimMath;
import ac.grim.grimac.utils.nmsutil.BlockBreakSpeed;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.player.DiggingAction;
import com.github.retrooper.packetevents.util.Vector3i;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerDigging;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerBlockChange;

// Based loosely off of Hawk BlockBreakSpeedSurvival
// Also based loosely off of NoCheatPlus FastBreak
// Also based off minecraft wiki: https://minecraft.fandom.com/wiki/Breaking#Instant_breaking
@CheckData(name = "FastBreak")
public class FastBreak extends PacketCheck {
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
                targetBlock = digging.getBlockPosition();
                startBreak = System.currentTimeMillis();
                maximumBlockDamage = BlockBreakSpeed.getBlockDamage(player, targetBlock);

                double breakDelay = System.currentTimeMillis() - lastFinishBreak;

                if (breakDelay >= 275) { // Reduce buffer if "close enough"
                    blockDelayBalance *= 0.9;
                } else { // Otherwise, increase buffer
                    blockDelayBalance += 300 - breakDelay;
                }

                if (blockDelayBalance > 1000) { // If more than a second of advantage
                    event.setCancelled(true); // Cancelling start digging will cause server to reject block break
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
                    int blockID = player.compensatedWorld.getWrappedBlockStateAt(digging.getBlockPosition()).getGlobalId();
                    player.user.sendPacket(new WrapperPlayServerBlockChange(digging.getBlockPosition(), blockID));
                    event.setCancelled(true); // Cancelling will make the server believe the player insta-broke the block
                    flagAndAlert("Diff=" + diff + ",Balance=" + blockBreakBalance);
                }

                lastFinishBreak = System.currentTimeMillis();
            }

            if (digging.getAction() == DiggingAction.CANCELLED_DIGGING) {
                targetBlock = null;
            }
        }
    }

    private void clampBalance() {
        double balance = Math.max(1000, (player.getTransactionPing() / 1e6));
        blockBreakBalance = GrimMath.clamp(blockBreakBalance, -balance, balance); // Clamp not Math.max in case other logic changes
        blockDelayBalance = GrimMath.clamp(blockDelayBalance, -balance, balance);
    }
}
