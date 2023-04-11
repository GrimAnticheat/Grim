package ac.grim.grimac.utils.inventory.inventory;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.inventory.Inventory;

public class MenuTypes {
    // I am unsure if I will write this, it might be needed for proxy support, although I am unsure.
    public static AbstractContainerMenu getMenuFromID(GrimPlayer player, Inventory playerInventory, int id) {
        switch (id) {
            // All these use the same menu and a SimpleContainer
            case 0: // 9x1
            case 1: // 9x2
            case 2: // 9x3
            case 3: // 9x4
            case 4: // 9x5
            case 5: // 9x6
                return new BasicInventoryMenu(player, playerInventory, id + 1); // don't repeat yourself :)
            case 6: // 3x3 (SimpleContainer)
                return new DispenserMenu(player, playerInventory);
            case 15: // hopper - SimpleContainer
                return new HopperMenu(player, playerInventory);
            default:
                return new NotImplementedMenu(player, playerInventory);
        }
    }

    public static AbstractContainerMenu getMenuFromString(GrimPlayer player, Inventory inventory, String legacyType, int slots, int horse) {
        switch (legacyType) {
            case "minecraft:chest":
            case "minecraft:container":
                return new BasicInventoryMenu(player, inventory, slots / 9);
            case "minecraft:dispenser":
            case "minecraft:dropper":
                return new DispenserMenu(player, inventory);
            case "minecraft:hopper":
                return new HopperMenu(player, inventory);
            case "minecraft:shulker_box":
                return new BasicInventoryMenu(player, inventory, 3);
            default: // Villager menu
                return new NotImplementedMenu(player, inventory);
        }
    }
}
