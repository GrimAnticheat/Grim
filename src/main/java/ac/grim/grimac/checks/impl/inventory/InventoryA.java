package ac.grim.grimac.checks.impl.inventory;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PacketCheck;
import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity.InteractAction;
import io.github.retrooper.packetevents.util.FoliaCompatUtil;

@CheckData(name = "InventoryA", setback = 3)
public class InventoryA extends Check implements PacketCheck {
    public InventoryA(GrimPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.INTERACT_ENTITY) {
            WrapperPlayClientInteractEntity wrapper = new WrapperPlayClientInteractEntity(event);

            if (wrapper.getAction() != InteractAction.ATTACK) return;

            // Is not possible to attack while the inventory is open.
            if (player.hasInventoryOpen) {
                if (flag()) {
                    // Cancel the packet
                    if (shouldModifyPackets()) {
                        event.setCancelled(true);
                        player.onPacketCancel();
                    }
                    FoliaCompatUtil.runTaskForEntity(player.bukkitPlayer, GrimAPI.INSTANCE.getPlugin(), () -> player.bukkitPlayer.closeInventory(), null, 0);
                    alert("");
                }
            } else {
                reward();
            }
        }
    }
}
