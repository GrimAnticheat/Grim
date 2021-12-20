package ac.grim.grimac.events.packets;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.data.ShulkerData;
import ac.grim.grimac.utils.latency.CompensatedWorldFlat;
import ac.grim.grimac.utils.nmsutil.Materials;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.impl.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.util.Vector3i;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerBlockAction;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;

// If a player doesn't get this packet, then they don't know the shulker box is currently opened
// Meaning if a player enters a chunk with an opened shulker box, they see the shulker box as closed.
//
// Exempting the player on shulker boxes is an option... but then you have people creating PvP arenas
// on shulker boxes to get high lenience.
//
// Due to the difficulty of cross version shulker box
public class PacketBlockAction extends PacketListenerAbstract {
    public PacketBlockAction() {
        super(PacketListenerPriority.MONITOR);
    }

    @Override
    public void onPacketSend(PacketSendEvent event) {
        if (event.getPacketType() == PacketType.Play.Server.BLOCK_ACTION) {
            GrimPlayer player = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer((Player) event.getPlayer());
            if (player == null) return;

            WrapperPlayServerBlockAction blockAction = new WrapperPlayServerBlockAction(event);
            Vector3i blockPos = blockAction.getBlockPosition();

            // TODO: Legacy support
            BlockData blockData = CompensatedWorldFlat.globalPaletteToBlockData.get(blockAction.getBlockTypeId());

            if (Materials.checkFlag(blockData.getMaterial(), Materials.SHULKER)) {
                // Param is the number of viewers of the shulker box.
                // Hashset with .equals() set to be position
                if (blockAction.getActionData() >= 1) {
                    ShulkerData data = new ShulkerData(blockPos, player.lastTransactionSent.get(), false);
                    player.compensatedWorld.openShulkerBoxes.add(data);
                } else {
                    // The shulker box is closing
                    ShulkerData data = new ShulkerData(blockPos, player.lastTransactionSent.get(), true);
                    player.compensatedWorld.openShulkerBoxes.add(data);
                }
            }
        }
    }
}
