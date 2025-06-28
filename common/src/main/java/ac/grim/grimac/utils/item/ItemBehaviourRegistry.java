package ac.grim.grimac.utils.item;

import com.github.retrooper.packetevents.protocol.item.type.ItemType;
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes;
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

    public static @NotNull ItemBehaviour getItemBehaviour(ItemType type) {
        return ITEM_MAPPING.getOrDefault(type, ItemBehaviour.INSTANCE);
    }

}
