package ac.grim.grimac.checks.impl.packetorder;

import ac.grim.grimac.api.storage.verbose.Verbose;
import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PostPredictionCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.update.PredictionComplete;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientClientStatus;

import java.util.ArrayDeque;

@CheckData(name = "PacketOrderK", stableKey = "grim.packetorder.inventory_open_order", description = "Opened, clicked, or closed inventory in the wrong packet order", experimental = true)
public class PacketOrderK extends Check implements PostPredictionCheck {
    // Shape index == KIND_* constant value.
    private static final Verbose V = Verbose
            .of("open, clicking={bool}, closing={bool}")
            .or("click")
            .or("close");

    static final int KIND_OPEN = 0;
    static final int KIND_CLICK = 1;
    static final int KIND_CLOSE = 2;

    public PacketOrderK(final GrimPlayer player) {
        super(player);
    }

    private final ArrayDeque<FlagData> flags = new ArrayDeque<>();

    private Verbose.Writer write(int kind, boolean clicking, boolean closing) {
        Verbose.Writer writer = V.write(verbose(), kind);
        if (kind == KIND_OPEN) writer.bool(clicking).bool(closing);
        return writer;
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.CLIENT_STATUS) {
            if (new WrapperPlayClientClientStatus(event).getAction() == WrapperPlayClientClientStatus.Action.OPEN_INVENTORY_ACHIEVEMENT) {
                if (player.packetOrderProcessor.isClickingInInventory() || player.packetOrderProcessor.isClosingInventory()) {
                    boolean clicking = player.packetOrderProcessor.isClickingInInventory();
                    boolean closing = player.packetOrderProcessor.isClosingInventory();
                    if (!player.canSkipTicks()) {
                        flag(write(KIND_OPEN, clicking, closing));
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
                    if (flag(write(kind, false, false))
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
                flag(write(data.kind(), data.clicking(), data.closing()));
            }
        }

        flags.clear();
    }

    private record FlagData(int kind, boolean clicking, boolean closing) {
    }
}
