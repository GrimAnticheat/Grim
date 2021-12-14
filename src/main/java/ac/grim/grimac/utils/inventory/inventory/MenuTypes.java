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

            case 7: // anvil - Painful

            case 8: // beacon - Not too painful

            case 9: // blast furnace (AbstractFurnaceMenu)
            case 10: // brewing stand - Lots of lines for items allowed but not too bad

            case 11: // crafting table - somewhat annoying

            case 12: // enchantment table - Seems difficult

            case 13: // furnace (AbstractFurnaceMenu)
            case 14: // grindstone
            case 15: // hopper - SimpleContainer

            case 16: // lectern
            case 17: // loom
            case 18: // merchant
            case 19: // smithing
            case 20: // smoker
            case 21: // cartographer
            case 22: // stonecutter - not bad
        }

        return null;
    }
}
