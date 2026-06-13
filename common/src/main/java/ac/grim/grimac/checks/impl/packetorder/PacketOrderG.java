package ac.grim.grimac.checks.impl.packetorder;

import ac.grim.grimac.api.storage.verbose.VerboseSchema;
import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PostPredictionCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.update.PredictionComplete;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.DiggingAction;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientClientStatus;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerDigging;

import java.util.ArrayDeque;

@CheckData(name = "PacketOrderG", stableKey = "grim.packetorder.hotbar_inventory_manage_order", experimental = true, verboseVersion = 1)
public class PacketOrderG extends Check implements PostPredictionCheck {
    public static final VerboseSchema V = VerboseSchema.of(
            "action:vi",
            "attacking:bool",
            "releasing:bool",
            "rightClicking:bool",
            "picking:bool",
            "digging:bool");

    static final int ACTION_OPEN_INVENTORY = 0;
    static final int ACTION_SWAP = 1;
    static final int ACTION_DROP = 2;

    public PacketOrderG(GrimPlayer player) {
        super(player);
    }

    private final ArrayDeque<FlagData> flags = new ArrayDeque<>();

    static String actionName(int action) {
        return switch (action) {
            case ACTION_OPEN_INVENTORY -> "openInventory";
            case ACTION_SWAP -> "swap";
            case ACTION_DROP -> "drop";
            default -> "unknown";
        };
    }

    static String verbose(
            int action,
            boolean attacking,
            boolean releasing,
            boolean rightClicking,
            boolean picking,
            boolean digging) {
        return "action=" + actionName(action)
                + ", attacking=" + attacking
                + ", releasing=" + releasing
                + ", rightClicking=" + rightClicking
                + ", picking=" + picking
                + ", digging=" + digging;
    }

    private static int action(DiggingAction action) {
        return action == null ? ACTION_OPEN_INVENTORY : action == DiggingAction.SWAP_ITEM_WITH_OFFHAND ? ACTION_SWAP : ACTION_DROP;
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.PLAYER_DIGGING || (event.getPacketType() == PacketType.Play.Client.CLIENT_STATUS
                && new WrapperPlayClientClientStatus(event).getAction() == WrapperPlayClientClientStatus.Action.OPEN_INVENTORY_ACHIEVEMENT)) {
            DiggingAction action = null;
            if (event.getPacketType() == PacketType.Play.Client.PLAYER_DIGGING) {
                action = new WrapperPlayClientPlayerDigging(event).getAction();
                if (action != DiggingAction.SWAP_ITEM_WITH_OFFHAND
                        && action != DiggingAction.DROP_ITEM
                        && action != DiggingAction.DROP_ITEM_STACK
                ) return;
            }

            if (player.packetOrderProcessor.isAttackingOrStabbing()
                    || player.packetOrderProcessor.isReleasing()
                    || player.packetOrderProcessor.isRightClicking()
                    || player.packetOrderProcessor.isPicking()
                    || player.packetOrderProcessor.isDigging()
            ) {
                int actionKind = action(action);
                boolean attacking = player.packetOrderProcessor.isAttackingOrStabbing();
                boolean releasing = player.packetOrderProcessor.isReleasing();
                boolean rightClicking = player.packetOrderProcessor.isRightClicking();
                boolean picking = player.packetOrderProcessor.isPicking();
                boolean digging = player.packetOrderProcessor.isDigging();
                if (!player.canSkipTicks()) {
                    if (flagAndAlert(V.write(verbose())
                            .vi(actionKind)
                            .bool(attacking)
                            .bool(releasing)
                            .bool(rightClicking)
                            .bool(picking)
                            .bool(digging)) && shouldModifyPackets() && canCancel(action)) {
                        event.setCancelled(true);
                        player.onPacketCancel();
                    }
                } else {
                    flags.add(new FlagData(actionKind, attacking, releasing, rightClicking, picking, digging));
                }
            }
        }
    }

    @Override
    public void onPredictionComplete(PredictionComplete predictionComplete) {
        if (!player.canSkipTicks()) return;

        if (player.isTickingReliablyFor(3)) {
            for (FlagData data : flags) {
                flagAndAlert(V.write(verbose())
                        .vi(data.action())
                        .bool(data.attacking())
                        .bool(data.releasing())
                        .bool(data.rightClicking())
                        .bool(data.picking())
                        .bool(data.digging()));
            }
        }

        flags.clear();
    }

    private record FlagData(
            int action,
            boolean attacking,
            boolean releasing,
            boolean rightClicking,
            boolean picking,
            boolean digging) {
    }
}
