package ac.grim.grimac.checks.impl.packetorder;

import ac.grim.grimac.api.storage.verbose.VerboseSchema;
import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PostPredictionCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.update.PredictionComplete;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientClientStatus;

import java.util.ArrayDeque;

@CheckData(name = "PacketOrderK", stableKey = "grim.packetorder.inventory_open_order", experimental = true, verboseVersion = 1)
public class PacketOrderK extends Check implements PostPredictionCheck {
    public static final VerboseSchema V = VerboseSchema.of("kind:vi", "clicking:bool", "closing:bool");

    static final int KIND_OPEN = 0;
    static final int KIND_CLICK = 1;
    static final int KIND_CLOSE = 2;

    public PacketOrderK(final GrimPlayer player) {
        super(player);
    }

    private final ArrayDeque<FlagData> flags = new ArrayDeque<>();

    static String verbose(int kind, boolean clicking, boolean closing) {
        return switch (kind) {
            case KIND_OPEN -> "open, clicking=" + clicking + ", closing=" + closing;
            case KIND_CLICK -> "click";
            case KIND_CLOSE -> "close";
            default -> "unknown";
        };
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.CLIENT_STATUS) {
            if (new WrapperPlayClientClientStatus(event).getAction() == WrapperPlayClientClientStatus.Action.OPEN_INVENTORY_ACHIEVEMENT) {
                if (player.packetOrderProcessor.isClickingInInventory() || player.packetOrderProcessor.isClosingInventory()) {
                    boolean clicking = player.packetOrderProcessor.isClickingInInventory();
                    boolean closing = player.packetOrderProcessor.isClosingInventory();
                    if (!player.canSkipTicks()) {
                        flagAndAlert(V.write(verbose()).vi(KIND_OPEN).bool(clicking).bool(closing));
                    } else {
                        flags.add(new FlagData(KIND_OPEN, clicking, closing));
                    }
                }
            }
        }

        if (event.getPacketType() == PacketType.Play.Client.CLICK_WINDOW || event.getPacketType() == PacketType.Play.Client.CLOSE_WINDOW) {
            if (player.packetOrderProcessor.isOpeningInventory()) {
                int kind = event.getPacketType() == PacketType.Play.Client.CLICK_WINDOW ? KIND_CLICK : KIND_CLOSE;
                if (!player.canSkipTicks()) {
                    if (flagAndAlert(V.write(verbose()).vi(kind).bool(false).bool(false))
                            && shouldModifyPackets() && event.getPacketType() == PacketType.Play.Client.CLICK_WINDOW) {
                        event.setCancelled(true);
                        player.onPacketCancel();
                    }
                } else {
                    flags.add(new FlagData(kind, false, false));
                }
            }
        }
    }

    @Override
    public void onPredictionComplete(PredictionComplete predictionComplete) {
        if (!player.canSkipTicks()) return;

        if (player.isTickingReliablyFor(3)) {
            for (FlagData data : flags) {
                flagAndAlert(V.write(verbose()).vi(data.kind()).bool(data.clicking()).bool(data.closing()));
            }
        }

        flags.clear();
    }

    private record FlagData(int kind, boolean clicking, boolean closing) {
    }
}
