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
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerDigging;

import java.util.ArrayDeque;

@CheckData(name = "PacketOrderF", stableKey = "grim.packetorder.input_tick_to_sneak_sprint_order", experimental = true, verboseVersion = 1)
public class PacketOrderF extends Check implements PostPredictionCheck {
    public static final VerboseSchema V = VerboseSchema.of("action:vi", "sprinting:bool", "sneaking:bool");

    static final int ACTION_INTERACT = 0;
    static final int ACTION_ATTACK = 1;
    static final int ACTION_SPECTATE_ENTITY = 2;
    static final int ACTION_PLACE = 3;
    static final int ACTION_USE = 4;
    static final int ACTION_PICK = 5;
    static final int ACTION_DIG = 6;
    static final int ACTION_OPEN_INVENTORY = 7;

    public PacketOrderF(GrimPlayer player) {
        super(player);
    }

    private final ArrayDeque<FlagData> flags = new ArrayDeque<>();

    static String actionName(int action) {
        return switch (action) {
            case ACTION_INTERACT -> "interact";
            case ACTION_ATTACK -> "attack";
            case ACTION_SPECTATE_ENTITY -> "spectateEntity";
            case ACTION_PLACE -> "place";
            case ACTION_USE -> "use";
            case ACTION_PICK -> "pick";
            case ACTION_DIG -> "dig";
            case ACTION_OPEN_INVENTORY -> "openInventory";
            default -> "unknown";
        };
    }

    static String verbose(int action, boolean sprinting, boolean sneaking) {
        return "action=" + actionName(action) + ", sprinting=" + sprinting + ", sneaking=" + sneaking;
    }

    private static int action(PacketReceiveEvent event) {
        return event.getPacketType() == PacketType.Play.Client.INTERACT_ENTITY ? ACTION_INTERACT
                : event.getPacketType() == PacketType.Play.Client.ATTACK ? ACTION_ATTACK
                : event.getPacketType() == PacketType.Play.Client.SPECTATE_ENTITY ? ACTION_SPECTATE_ENTITY
                : event.getPacketType() == PacketType.Play.Client.PLAYER_BLOCK_PLACEMENT ? ACTION_PLACE
                : event.getPacketType() == PacketType.Play.Client.USE_ITEM ? ACTION_USE
                : event.getPacketType() == PacketType.Play.Client.PICK_ITEM ? ACTION_PICK
                : event.getPacketType() == PacketType.Play.Client.PLAYER_DIGGING ? ACTION_DIG
                : ACTION_OPEN_INVENTORY;
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.INTERACT_ENTITY
                || event.getPacketType() == PacketType.Play.Client.ATTACK
                || event.getPacketType() == PacketType.Play.Client.SPECTATE_ENTITY
                || event.getPacketType() == PacketType.Play.Client.PLAYER_BLOCK_PLACEMENT
                || event.getPacketType() == PacketType.Play.Client.USE_ITEM
                || event.getPacketType() == PacketType.Play.Client.PICK_ITEM
                || event.getPacketType() == PacketType.Play.Client.PLAYER_DIGGING
                || (event.getPacketType() == PacketType.Play.Client.CLIENT_STATUS
                && new WrapperPlayClientClientStatus(event).getAction() == WrapperPlayClientClientStatus.Action.OPEN_INVENTORY_ACHIEVEMENT)
        ) if (player.packetOrderProcessor.isSprinting() || player.packetOrderProcessor.isSneaking()) {
            int action = action(event);
            boolean sprinting = player.packetOrderProcessor.isSprinting();
            boolean sneaking = player.packetOrderProcessor.isSneaking();
            if (!player.canSkipTicks()) {
                if (flagAndAlert(V.write(verbose()).vi(action).bool(sprinting).bool(sneaking)) && shouldModifyPackets()) {
                    if (event.getPacketType() == PacketType.Play.Client.PLAYER_DIGGING
                            && !canCancel(new WrapperPlayClientPlayerDigging(event).getAction())
                    ) return; // don't cause a noslow

                    event.setCancelled(true);
                    player.onPacketCancel();
                }
            } else {
                flags.add(new FlagData(action, sprinting, sneaking));
            }
        }
    }

    @Override
    public void onPredictionComplete(PredictionComplete predictionComplete) {
        if (!player.canSkipTicks()) return;

        if (player.isTickingReliablyFor(3)) {
            for (FlagData data : flags) {
                flagAndAlert(V.write(verbose()).vi(data.action()).bool(data.sprinting()).bool(data.sneaking()));
            }
        }

        flags.clear();
    }

    private record FlagData(int action, boolean sprinting, boolean sneaking) {
    }
}
