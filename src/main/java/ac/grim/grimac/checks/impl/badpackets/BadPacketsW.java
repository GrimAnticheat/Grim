package ac.grim.grimac.checks.impl.badpackets;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PacketCheck;
import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

@CheckData(name = "BadPacketsW", experimental = true)
public class BadPacketsW extends Check implements PacketCheck {
    public BadPacketsW(GrimPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (player.getClientVersion().isOlderThan(ClientVersion.V_1_9)) return;
        if (event.getPacketType() == PacketType.Play.Client.INTERACT_ENTITY) {
            WrapperPlayClientInteractEntity interactEntity = new WrapperPlayClientInteractEntity(event);
            if (interactEntity.getAction() != WrapperPlayClientInteractEntity.InteractAction.ATTACK) return;
            if (!player.usingItem) return;
            ItemStack itemInUse = player.getItemInHand(player.usedItemHand);
            if (itemInUse.getType() == Material.SHIELD || itemInUse.getType().isEdible()) {
                if (flagAndAlert("UseItem=" + itemInUse.getType().name()) && shouldModifyPackets()) {
                    event.setCancelled(true);
                    player.onPacketCancel();
                }
            }
        }
    }
}
