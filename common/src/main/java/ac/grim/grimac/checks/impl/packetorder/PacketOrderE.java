package ac.grim.grimac.checks.impl.packetorder;

import ac.grim.grimac.api.storage.verbose.Verbose;
import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PostPredictionCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.update.PredictionComplete;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;

import java.util.ArrayDeque;

@CheckData(name = "PacketOrderE", stableKey = "grim.packetorder.slot_order", description = "Changed held item slot during another conflicting action", experimental = true)
public class PacketOrderE extends Check implements PostPredictionCheck {
    private static final Verbose V = Verbose.of(
            "attacking={bool}, rightClicking={bool}, openingInventory={bool}, releasing={bool}"
                    + ", sneaking={bool}, sprinting={bool}, bed={bool}, gliding={bool}, mountJumping={bool}");

    private static final int ATTACKING = 1 << 0;
    private static final int RIGHT_CLICKING = 1 << 1;
    private static final int OPENING_INVENTORY = 1 << 2;
    private static final int RELEASING = 1 << 3;
    private static final int SNEAKING = 1 << 4;
    private static final int SPRINTING = 1 << 5;
    private static final int LEAVING_BED = 1 << 6;
    private static final int GLIDING = 1 << 7;
    private static final int MOUNT_JUMPING = 1 << 8;

    public PacketOrderE(final GrimPlayer player) {
        super(player);
    }

    private final ArrayDeque<Integer> flags = new ArrayDeque<>();
    private boolean setback;

    /** Expands the flag bitmask into the template's individual bool fields. */
    private Verbose.Writer write(int flags) {
        return V.write(verbose())
                .bool(has(flags, ATTACKING))
                .bool(has(flags, RIGHT_CLICKING))
                .bool(has(flags, OPENING_INVENTORY))
                .bool(has(flags, RELEASING))
                .bool(has(flags, SNEAKING))
                .bool(has(flags, SPRINTING))
                .bool(has(flags, LEAVING_BED))
                .bool(has(flags, GLIDING))
                .bool(has(flags, MOUNT_JUMPING));
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.HELD_ITEM_CHANGE) {
            int currentFlags = currentFlags();
            if (currentFlags != 0) {
                if (player.canSkipTicks() && flags.add(currentFlags) || flag(write(currentFlags))) {
                    if (player.packetOrderProcessor.isUsing()) {
                        setback = true;
                    }
                }
            }
        }
    }

    @Override
    public void onPredictionComplete(PredictionComplete predictionComplete) {
        if (!player.canSkipTicks()) {
            if (setback) {
                setback = false;
                setbackIfAboveSetbackVL();
            }
            return;
        }

        if (player.isTickingReliablyFor(3)) {
            for (int currentFlags : flags) {
                if (flag(write(currentFlags)) && setback) {
                    setback = false;
                    setbackIfAboveSetbackVL();
                }
            }
        }

        setback = false;
        flags.clear();
    }

    private int currentFlags() {
        int flags = 0;
        if (player.packetOrderProcessor.isAttackingOrStabbing()) flags |= ATTACKING;
        if (player.packetOrderProcessor.isRightClicking()) flags |= RIGHT_CLICKING;
        if (player.packetOrderProcessor.isOpeningInventory()) flags |= OPENING_INVENTORY;
        if (player.packetOrderProcessor.isReleasing()) flags |= RELEASING;
        if (player.packetOrderProcessor.isSneaking()) flags |= SNEAKING;
        if (player.packetOrderProcessor.isSprinting()) flags |= SPRINTING;
        if (player.packetOrderProcessor.isLeavingBed()) flags |= LEAVING_BED;
        if (player.packetOrderProcessor.isStartingToGlide()) flags |= GLIDING;
        if (player.packetOrderProcessor.isJumpingWithMount()) flags |= MOUNT_JUMPING;
        return flags;
    }

    private static boolean has(int flags, int flag) {
        return (flags & flag) != 0;
    }
}