package ac.grim.grimac.checks.impl.packetorder;

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

@CheckData(name = "PacketOrderF", experimental = true)
public class PacketOrderF extends Check implements PostPredictionCheck {
    public PacketOrderF(GrimPlayer player) {
        super(player);
    }

    private final ArrayDeque<String> flags = new ArrayDeque<>();

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.INTERACT_ENTITY
                || event.getPacketType() == PacketType.Play.Client.PLAYER_BLOCK_PLACEMENT
                || event.getPacketType() == PacketType.Play.Client.USE_ITEM
                || event.getPacketType() == PacketType.Play.Client.PICK_ITEM
                || event.getPacketType() == PacketType.Play.Client.PLAYER_DIGGING
                || (event.getPacketType() == PacketType.Play.Client.CLIENT_STATUS
                && new WrapperPlayClientClientStatus(event).getAction() == WrapperPlayClientClientStatus.Action.OPEN_INVENTORY_ACHIEVEMENT)
        ) if (player.packetOrderProcessor.isSprinting() || player.packetOrderProcessor.isSneaking()) {
            String verbose = "action=" + (event.getPacketType() == PacketType.Play.Client.INTERACT_ENTITY ? "interact"
                    : event.getPacketType() == PacketType.Play.Client.PLAYER_BLOCK_PLACEMENT ? "place"
                    : event.getPacketType() == PacketType.Play.Client.USE_ITEM ? "use"
                    : event.getPacketType() == PacketType.Play.Client.PICK_ITEM ? "pick"
                    : event.getPacketType() == PacketType.Play.Client.PLAYER_DIGGING ? "dig"
                    : "openInventory")
                    + ", sprinting=" + player.packetOrderProcessor.isSprinting()
                    + ", sneaking=" + player.packetOrderProcessor.isSneaking();
            if (!player.canSkipTicks()) {
                if (flagAndAlert(verbose) && shouldModifyPackets()) {
                    if (event.getPacketType() == PacketType.Play.Client.PLAYER_DIGGING
                            && new WrapperPlayClientPlayerDigging(event).getAction() == DiggingAction.RELEASE_USE_ITEM
                    ) return; // don't cause a noslow

                    event.setCancelled(true);
                    player.onPacketCancel();
                }
            } else {
                flags.add(verbose);
            }
        }
    }

    @Override
    public void onPredictionComplete(PredictionComplete predictionComplete) {
        if (!player.canSkipTicks()) return;

        if (player.isTickingReliablyFor(3)) {
            for (String verbose : flags) {
                flagAndAlert(verbose);
            }
        }

        flags.clear();
    }
}
