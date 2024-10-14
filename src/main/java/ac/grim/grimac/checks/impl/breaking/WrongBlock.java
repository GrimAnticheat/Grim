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
import com.github.retrooper.packetevents.util.Vector3i;

import static ac.grim.grimac.utils.nmsutil.BlockBreakSpeed.getBlockDamage;

@CheckData(name = "WrongBlock", experimental = true)
public class WrongBlock extends Check implements BlockBreakCheck {
    public WrongBlock(final GrimPlayer player) {
        super(player);
    }

    private boolean lastBlockWasInstantBreak = false;
    private Vector3i lastBlock, lastCancelledBlock, lastLastBlock = null;
    private final int exemptedY = player.getClientVersion().isOlderThan(ClientVersion.V_1_8) ? 255 : (PacketEvents.getAPI().getServerManager().getVersion().isNewerThanOrEquals(ServerVersion.V_1_14) ? -1 : 4095);

    // The client sometimes sends a wierd cancel packet
    private boolean shouldExempt(final Vector3i pos) {
        // lastLastBlock is always null when this happens, and lastBlock isn't
        if (lastLastBlock != null || lastBlock == null)
            return false;

        // on pre 1.14.4 clients, the YPos of this packet is always the same
        if (player.getClientVersion().isOlderThan(ClientVersion.V_1_14_4) && pos.y != exemptedY)
            return false;

        // and if this block is not an instant break
        return player.getClientVersion().isOlderThan(ClientVersion.V_1_14_4) || getBlockDamage(player, pos) < 1;
    }

    @Override
    public void onBlockBreak(BlockBreak blockBreak) {
        if (blockBreak.action == DiggingAction.START_DIGGING) {
            lastBlockWasInstantBreak = getBlockDamage(player, blockBreak.position) >= 1;
            lastCancelledBlock = null;
            lastLastBlock = lastBlock;
            lastBlock = blockBreak.position;
        }

        if (blockBreak.action == DiggingAction.CANCELLED_DIGGING) {
            if (!shouldExempt(blockBreak.position)) {
                if (!blockBreak.position.equals(lastBlock)) {
                    // https://github.com/GrimAnticheat/Grim/issues/1512
                    if (player.getClientVersion().isOlderThan(ClientVersion.V_1_14_4) || (!lastBlockWasInstantBreak && blockBreak.position.equals(lastCancelledBlock))) {
                        if (flagAndAlert("action=CANCELLED_DIGGING" + ", last=" + MessageUtil.toUnlabledString(lastBlock) + ", pos=" + MessageUtil.toUnlabledString(blockBreak.position))) {
                            if (shouldModifyPackets()) {
                                blockBreak.cancel();
                            }
                        }
                    }
                }
            }

            lastCancelledBlock = blockBreak.position;
            lastLastBlock = null;
            lastBlock = null;
        }

        if (blockBreak.action == DiggingAction.FINISHED_DIGGING) {
            if (!blockBreak.position.equals(lastCancelledBlock) // https://bugs.mojang.com/browse/MC-255057
                    && (!lastBlockWasInstantBreak || player.getClientVersion().isOlderThan(ClientVersion.V_1_14_4))
                    && !blockBreak.position.equals(lastBlock)) {
                if (flagAndAlert("action=FINISHED_DIGGING" + ", last=" + MessageUtil.toUnlabledString(lastBlock) + ", pos=" + MessageUtil.toUnlabledString(blockBreak.position))) {
                    if (shouldModifyPackets()) {
                        blockBreak.cancel();
                    }
                }
            }

            lastCancelledBlock = null;

            // 1.14.4+ clients don't send another start break in protected regions
            if (player.getClientVersion().isOlderThan(ClientVersion.V_1_14_4)) {
                lastLastBlock = null;
                lastBlock = null;
            }
        }
    }
}
