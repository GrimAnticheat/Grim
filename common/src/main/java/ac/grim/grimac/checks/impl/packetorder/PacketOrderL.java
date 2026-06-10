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

@CheckData(name = "PacketOrderL", stableKey = "grim.packetorder.drop_item_order", verboseVersion = 1, description = "Sent drop, inventory open, or offhand swap packets in an invalid order", experimental = true)
public class PacketOrderL extends Check implements PostPredictionCheck {
    public static final VerboseSchema V = VerboseSchema.of("action:vi");

    static final int ACTION_INVENTORY = 0;
    static final int ACTION_SWAP = 1;

    public PacketOrderL(final GrimPlayer player) {
        super(player);
    }

    private final ArrayDeque<Integer> flags = new ArrayDeque<>();

    static String verbose(int action) {
        return switch (action) {
            case ACTION_INVENTORY -> "inventory";
            case ACTION_SWAP -> "swap";
            default -> "unknown";
        };
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.CLIENT_STATUS) {
            if (new WrapperPlayClientClientStatus(event).getAction() == WrapperPlayClientClientStatus.Action.OPEN_INVENTORY_ACHIEVEMENT) {
                if (player.packetOrderProcessor.isDropping()) {
                    if (!player.canSkipTicks()) {
                        if (flagAndAlert(V.write(verbose()).vi(ACTION_INVENTORY)) && shouldModifyPackets()) {
                            event.setCancelled(true);
                            player.onPacketCancel();
                        }
                    } else {
                        flags.add(ACTION_INVENTORY);
                    }
                }
            }
        }

        if (event.getPacketType() == PacketType.Play.Client.PLAYER_DIGGING) {
            if (new WrapperPlayClientPlayerDigging(event).getAction() == DiggingAction.SWAP_ITEM_WITH_OFFHAND) {
                if (player.packetOrderProcessor.isDropping()) {
                    if (!player.canSkipTicks()) {
                        if (flagAndAlert(V.write(verbose()).vi(ACTION_SWAP)) && shouldModifyPackets()) {
                            event.setCancelled(true);
                            player.onPacketCancel();
                        }
                    } else {
                        flags.add(ACTION_SWAP);
                    }
                }
            }
        }
    }

    @Override
    public void onPredictionComplete(PredictionComplete predictionComplete) {
        if (!player.canSkipTicks()) return;

        if (player.isTickingReliablyFor(3)) {
            for (int action : flags) {
                flagAndAlert(V.write(verbose()).vi(action));
            }
        }

        flags.clear();
    }
}
