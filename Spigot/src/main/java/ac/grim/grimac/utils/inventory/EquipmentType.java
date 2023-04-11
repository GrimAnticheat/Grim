package ac.grim.grimac.utils.inventory;

import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.item.type.ItemType;
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes;

public enum EquipmentType {
    MAINHAND,
    OFFHAND,
    FEET,
    LEGS,
    CHEST,
    HEAD;

    public static EquipmentType byArmorID(int id) {
        switch (id) {
            case 0:
                return HEAD;
            case 1:
                return CHEST;
            case 2:
                return LEGS;
            case 3:
                return FEET;
            default:
                return MAINHAND;
        }
    }

    public static EquipmentType getEquipmentSlotForItem(ItemStack p_147234_) {
        ItemType item = p_147234_.getType();
        if (item == ItemTypes.CARVED_PUMPKIN || (item.getName().getKey().contains("SKULL") ||
                (item.getName().getKey().contains("HEAD") && !item.getName().getKey().contains("PISTON")))) {
            return HEAD;
        }
        if (item == ItemTypes.ELYTRA) {
            return CHEST;
        }
        if (item == ItemTypes.LEATHER_BOOTS || item == ItemTypes.CHAINMAIL_BOOTS
                || item == ItemTypes.IRON_BOOTS || item == ItemTypes.DIAMOND_BOOTS
                || item == ItemTypes.GOLDEN_BOOTS || item == ItemTypes.NETHERITE_BOOTS) {
            return FEET;
        }
        if (item == ItemTypes.LEATHER_LEGGINGS || item == ItemTypes.CHAINMAIL_LEGGINGS
                || item == ItemTypes.IRON_LEGGINGS || item == ItemTypes.DIAMOND_LEGGINGS
                || item == ItemTypes.GOLDEN_LEGGINGS || item == ItemTypes.NETHERITE_LEGGINGS) {
            return LEGS;
        }
        if (item == ItemTypes.LEATHER_CHESTPLATE || item == ItemTypes.CHAINMAIL_CHESTPLATE
                || item == ItemTypes.IRON_CHESTPLATE || item == ItemTypes.DIAMOND_CHESTPLATE
                || item == ItemTypes.GOLDEN_CHESTPLATE || item == ItemTypes.NETHERITE_CHESTPLATE) {
            return CHEST;
        }
        if (item == ItemTypes.LEATHER_HELMET || item == ItemTypes.CHAINMAIL_HELMET
                || item == ItemTypes.IRON_HELMET || item == ItemTypes.DIAMOND_HELMET
                || item == ItemTypes.GOLDEN_HELMET || item == ItemTypes.NETHERITE_HELMET) {
            return HEAD;
        }
        return ItemTypes.SHIELD == item ? OFFHAND : MAINHAND;
    }

    public boolean isArmor() {
        return this == FEET || this == LEGS || this == CHEST || this == HEAD;
    }

    public int getIndex() {
        switch (this) {
            case MAINHAND:
                return 0;
            case OFFHAND:
                return 1;
            case FEET:
                return 0;
            case LEGS:
                return 1;
            case CHEST:
                return 2;
            case HEAD:
                return 3;
            default:
                return -1;
        }
    }
}
