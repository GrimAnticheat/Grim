package ac.grim.grimac.utils.inventory;

import ac.grim.grimac.utils.nmsutil.XMaterial;
import org.bukkit.Material;

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

    public static EquipmentType getEquipmentSlotForItem(WrappedStack p_147234_) {
        Material item = p_147234_.getItem();
        if (item == XMaterial.CARVED_PUMPKIN.parseMaterial() || (item.name().contains("SKULL") ||
                (item.name().contains("HEAD") && !item.name().contains("PISTON")))) {
            return HEAD;
        }
        if (item == XMaterial.ELYTRA.parseMaterial()) {
            return CHEST;
        }
        if (item == XMaterial.LEATHER_BOOTS.parseMaterial() || item == XMaterial.CHAINMAIL_BOOTS.parseMaterial()
                || item == XMaterial.IRON_BOOTS.parseMaterial() || item == XMaterial.DIAMOND_BOOTS.parseMaterial()
                || item == XMaterial.GOLDEN_BOOTS.parseMaterial() || item == XMaterial.NETHERITE_BOOTS.parseMaterial()) {
            return FEET;
        }
        if (item == XMaterial.LEATHER_LEGGINGS.parseMaterial() || item == XMaterial.CHAINMAIL_LEGGINGS.parseMaterial()
                || item == XMaterial.IRON_LEGGINGS.parseMaterial() || item == XMaterial.DIAMOND_LEGGINGS.parseMaterial()
                || item == XMaterial.GOLDEN_LEGGINGS.parseMaterial() || item == XMaterial.NETHERITE_LEGGINGS.parseMaterial()) {
            return LEGS;
        }
        if (item == XMaterial.LEATHER_CHESTPLATE.parseMaterial() || item == XMaterial.CHAINMAIL_CHESTPLATE.parseMaterial()
                || item == XMaterial.IRON_CHESTPLATE.parseMaterial() || item == XMaterial.DIAMOND_CHESTPLATE.parseMaterial()
                || item == XMaterial.GOLDEN_CHESTPLATE.parseMaterial() || item == XMaterial.NETHERITE_CHESTPLATE.parseMaterial()) {
            return CHEST;
        }
        if (item == XMaterial.LEATHER_HELMET.parseMaterial() || item == XMaterial.CHAINMAIL_HELMET.parseMaterial()
                || item == XMaterial.IRON_HELMET.parseMaterial() || item == XMaterial.DIAMOND_HELMET.parseMaterial()
                || item == XMaterial.GOLDEN_HELMET.parseMaterial() || item == XMaterial.NETHERITE_HELMET.parseMaterial()) {
            return HEAD;
        }
        return XMaterial.SHIELD.parseMaterial() == item ? OFFHAND : MAINHAND;
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
