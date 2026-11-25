package ac.grim.grimac.utils.item;

import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.item.type.ItemType;
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class ItemBehaviourRegistry {

    private static final Map<ItemType, ItemBehaviour> ITEM_MAPPING = Map.of(
            ItemTypes.GOAT_HORN, AlwaysUseItem.INSTANCE,
            ItemTypes.SHIELD, AlwaysUseItem.INSTANCE,
            ItemTypes.SPYGLASS, AlwaysUseItem.INSTANCE,
            ItemTypes.CROSSBOW, UnsupportedItem.INSTANCE,
            ItemTypes.BOW, UnsupportedItem.INSTANCE,
            ItemTypes.TRIDENT, TridentItem.INSTANCE
    );

    private static final boolean RELIABLE_COMPONENT_SYSTEM = PacketEvents.getAPI().getServerManager().getVersion().isNewerThanOrEquals(ServerVersion.V_1_21_4);

    public static @NotNull ItemBehaviour getItemBehaviour(GrimPlayer player, ItemType type) {
        if (!RELIABLE_COMPONENT_SYSTEM || player.getClientVersion().isOlderThan(ClientVersion.V_1_21_4)) {
            return LegacyItem.INSTANCE;
        }

        return ITEM_MAPPING.getOrDefault(type, ItemBehaviour.INSTANCE);
    }

}
