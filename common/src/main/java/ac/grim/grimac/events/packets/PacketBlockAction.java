package ac.grim.grimac.events.packets;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.data.PistonData;
import ac.grim.grimac.utils.data.ShulkerData;
import ac.grim.grimac.utils.data.movable.PistonBlockEvent;
import ac.grim.grimac.utils.nmsutil.Materials;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import com.github.retrooper.packetevents.util.Vector3i;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerBlockAction;

/**
 * Tracks client-side block actions from the packets the client receives.
 *
 * <p>The client resolves piston structure from its own world when it handles a block action. Grim must do the same
 * from its per-player compensated world. Bukkit piston events describe server state before the client has necessarily
 * received it and therefore cannot be the source of piston prediction.</p>
 */
public class PacketBlockAction extends PacketListenerAbstract {
    public PacketBlockAction() {
        super(PacketListenerPriority.HIGH);
    }

    @Override
    public boolean isPreVia() {
        return true;
    }

    @Override
    public void onPacketSend(PacketSendEvent event) {
        if (event.getPacketType() != PacketType.Play.Server.BLOCK_ACTION) return;

        GrimPlayer player = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(event.getUser());
        if (player == null) return;

        WrapperPlayServerBlockAction blockAction = new WrapperPlayServerBlockAction(event);
        Vector3i blockPos = blockAction.getBlockPosition();
        int actionId = blockAction.getActionId();
        int actionData = blockAction.getActionData();
        int transaction = player.lastTransactionSent.get();

        player.latencyUtils.addRealTimeTask(transaction, () -> {
            WrappedBlockState existing = player.compensatedWorld.getBlock(blockPos);
            PistonData piston = PistonBlockEvent.handle(
                    player, blockPos.getX(), blockPos.getY(), blockPos.getZ(), actionId, transaction);
            if (piston != null) {
                player.compensatedWorld.activePistons.add(piston);
            } else if (Materials.isShulker(existing.getType())) {
                ShulkerData data = new ShulkerData(blockPos, player.lastTransactionSent.get(), actionData < 1);
                player.compensatedWorld.openShulkerBoxes.remove(data);
                player.compensatedWorld.openShulkerBoxes.add(data);
            }
        });
    }
}
