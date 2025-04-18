package ac.grim.grimac.checks.impl.breaking;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.BlockBreakCheck;
import ac.grim.grimac.checks.CheckType;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.MessageUtil;
import ac.grim.grimac.utils.anticheat.update.BlockBreak;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.player.DiggingAction;
import com.github.retrooper.packetevents.util.Vector3i;

import static ac.grim.grimac.utils.nmsutil.BlockBreakSpeed.getBlockDamage;

@CheckData(name = "WrongBreak", checkType = CheckType.WORLD)
public class WrongBreak extends Check implements BlockBreakCheck {
    public WrongBreak(final GrimPlayer player) {
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
            final Vector3i pos = blockBreak.position;

            lastBlockWasInstantBreak = getBlockDamage(player, pos) >= 1;
            lastCancelledBlock = null;
            lastLastBlock = lastBlock;
            lastBlock = pos;
        }

        if (blockBreak.action == DiggingAction.CANCELLED_DIGGING) {
            final Vector3i pos = blockBreak.position;

            if (!shouldExempt(pos) && !pos.equals(lastBlock)) {
                // https://github.com/GrimAnticheat/Grim/issues/1512
                if (player.getClientVersion().isOlderThan(ClientVersion.V_1_14_4) || (!lastBlockWasInstantBreak && pos.equals(lastCancelledBlock))) {
                    if (flagAndAlert("action=CANCELLED_DIGGING" + ", last=" + MessageUtil.toUnlabledString(lastBlock) + ", pos=" + MessageUtil.toUnlabledString(pos))) {
                        if (shouldModifyPackets()) {
                            blockBreak.cancel();
                        }
                    }
                }
            }

            lastCancelledBlock = pos;
            lastLastBlock = null;
            lastBlock = null;
            return;
        }

        if (blockBreak.action == DiggingAction.FINISHED_DIGGING) {
            final Vector3i pos = blockBreak.position;

            // when a player looks away from the mined block, they send a cancel, and if they look at it again, they don't send another start. (thanks mojang!)
            if (!pos.equals(lastCancelledBlock) && (!lastBlockWasInstantBreak || player.getClientVersion().isOlderThan(ClientVersion.V_1_14_4)) && !pos.equals(lastBlock)) {
                if (flagAndAlert("action=FINISHED_DIGGING" + ", last=" + MessageUtil.toUnlabledString(lastBlock) + ", pos=" + MessageUtil.toUnlabledString(pos))) {
                    if (shouldModifyPackets()) {
                        blockBreak.cancel();
                    }
                }
            }

            // 1.14.4+ clients don't send another start break in protected regions
            if (player.getClientVersion().isOlderThan(ClientVersion.V_1_14_4)) {
                lastCancelledBlock = null;
                lastLastBlock = null;
                lastBlock = null;
            }
        }
    }
}
