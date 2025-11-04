package ac.grim.grimac.checks.impl.breaking;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.BlockBreakCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.MessageUtil;
import ac.grim.grimac.utils.anticheat.update.BlockBreak;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.player.DiggingAction;
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import com.github.retrooper.packetevents.util.Vector3i;

import static ac.grim.grimac.utils.nmsutil.BlockBreakSpeed.getBlockDamage;

@CheckData(name = "WrongBreak")
public class WrongBreak extends Check implements BlockBreakCheck {
    private final int exemptedY = player.getClientVersion().isOlderThan(ClientVersion.V_1_8) ? 255 : (PacketEvents.getAPI().getServerManager().getVersion().isNewerThanOrEquals(ServerVersion.V_1_14) ? -1 : 4095);
    private boolean lastBlockWasInstantBreak = false;

    private boolean lastBlockIsSet;
    private int lastBlockX, lastBlockY, lastBlockZ;

    private boolean lastCancelledBlockIsSet;
    private int lastCancelledBlockX, lastCancelledBlockY, lastCancelledBlockZ;

    private boolean lastLastBlockIsSet;

    public WrongBreak(final GrimPlayer player) {
        super(player);
    }

    // The client sometimes sends a wierd cancel packet
    private boolean shouldExempt(final WrappedBlockState block, int yPos) {
        if (lastLastBlockIsSet || !lastBlockIsSet)
            return false;

        // on pre 1.14.4 clients, the YPos of this packet is always the same
        if (player.getClientVersion().isOlderThan(ClientVersion.V_1_14_4) && yPos != exemptedY)
            return false;

        // and if this block is not an instant break
        return player.getClientVersion().isOlderThan(ClientVersion.V_1_14_4) || getBlockDamage(player, block) < 1;
    }

    @Override
    public void onBlockBreak(BlockBreak blockBreak) {
        final int x = blockBreak.x;
        final int y = blockBreak.y;
        final int z = blockBreak.z;

        if (blockBreak.action == DiggingAction.START_DIGGING) {
            lastBlockWasInstantBreak = getBlockDamage(player, blockBreak.block) >= 1;
            lastCancelledBlockIsSet = false;

            lastLastBlockIsSet = lastBlockIsSet;

            lastBlockIsSet = true;
            lastBlockX = x;
            lastBlockY = y;
            lastBlockZ = z;
        } else if (blockBreak.action == DiggingAction.CANCELLED_DIGGING) {
            boolean notLastBlock = !lastBlockIsSet || x != lastBlockX || y != lastBlockY || z != lastBlockZ;

            if (!shouldExempt(blockBreak.block, y) && notLastBlock) {
                // https://github.com/GrimAnticheat/Grim/issues/1512
                boolean isLastCancelled = lastCancelledBlockIsSet && x == lastCancelledBlockX && y == lastCancelledBlockY && z == lastCancelledBlockZ;
                if (player.getClientVersion().isOlderThan(ClientVersion.V_1_14_4) || (!lastBlockWasInstantBreak && isLastCancelled)) {
                    String lastBlockStr = lastBlockIsSet ? MessageUtil.toUnlabeledString(lastBlockX, lastBlockY, lastBlockZ) : "null";
                    if (flagAndAlert("action=CANCELLED_DIGGING" + ", last=" + lastBlockStr + ", pos=" + MessageUtil.toUnlabeledString(x, y, z))) {
                        if (shouldModifyPackets()) {
                            blockBreak.cancel();
                        }
                    }
                }
            }

            lastCancelledBlockIsSet = true;
            lastCancelledBlockX = x;
            lastCancelledBlockY = y;
            lastCancelledBlockZ = z;

            lastLastBlockIsSet = lastBlockIsSet = false;
        } else if (blockBreak.action == DiggingAction.FINISHED_DIGGING) {
            boolean notLastCancelled = !lastCancelledBlockIsSet || x != lastCancelledBlockX || y != lastCancelledBlockY || z != lastCancelledBlockZ;
            boolean notLastBlock = !lastBlockIsSet || x != lastBlockX || y != lastBlockY || z != lastBlockZ;

            // when a player looks away from the mined block, they send a cancel, and if they look at it again, they don't send another start. (thanks mojang!)
            if (notLastCancelled && (!lastBlockWasInstantBreak || player.getClientVersion().isOlderThan(ClientVersion.V_1_14_4)) && notLastBlock) {
                String lastBlockStr = lastBlockIsSet ? MessageUtil.toUnlabeledString(lastBlockX, lastBlockY, lastBlockZ) : "null";
                if (flagAndAlert("action=FINISHED_DIGGING" + ", last=" + lastBlockStr + ", pos=" + MessageUtil.toUnlabeledString(x, y, z))) {
                    if (shouldModifyPackets()) {
                        blockBreak.cancel();
                    }
                }
            }

            // 1.14.4+ clients don't send another start break in protected regions
            if (player.getClientVersion().isOlderThan(ClientVersion.V_1_14_4)) {
                lastCancelledBlockIsSet = lastLastBlockIsSet = lastBlockIsSet = false;
            }
        }
    }
}
