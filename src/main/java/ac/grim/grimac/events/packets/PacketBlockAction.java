package ac.grim.grimac.events.packets;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.data.ShulkerData;
import ac.grim.grimac.utils.nmsImplementations.Materials;
import io.github.retrooper.packetevents.event.PacketListenerAbstract;
import io.github.retrooper.packetevents.event.PacketListenerPriority;
import io.github.retrooper.packetevents.event.impl.PacketPlaySendEvent;
import io.github.retrooper.packetevents.packettype.PacketType;
import io.github.retrooper.packetevents.packetwrappers.play.out.blockaction.WrappedPacketOutBlockAction;
import io.github.retrooper.packetevents.utils.vector.Vector3i;

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
    public void onPacketPlaySend(PacketPlaySendEvent event) {
        byte packetID = event.getPacketId();

        if (packetID == PacketType.Play.Server.BLOCK_ACTION) {
            GrimPlayer player = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(event.getPlayer());
            if (player == null) return;

            WrappedPacketOutBlockAction blockAction = new WrappedPacketOutBlockAction(event.getNMSPacket());
            Vector3i position = blockAction.getBlockPosition();

            if (Materials.checkFlag(blockAction.getBlockType(), Materials.SHULKER)) {
                // Param is the number of viewers of the shulker box.
                // Hashset with .equals() set to be position
                if (blockAction.getActionParam() >= 1) {
                    ShulkerData data = new ShulkerData(position, player.lastTransactionSent.get(), false);
                    player.compensatedWorld.openShulkerBoxes.removeIf(shulkerData -> shulkerData.position.equals(position));
                    player.compensatedWorld.openShulkerBoxes.add(data);
                } else {
                    // The shulker box is closing
                    ShulkerData data = new ShulkerData(position, player.lastTransactionSent.get(), true);
                    player.compensatedWorld.openShulkerBoxes.removeIf(shulkerData -> shulkerData.position.equals(position));
                    player.compensatedWorld.openShulkerBoxes.add(data);
                }
            }
        }
    }
}
