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
            case 8: // beacon - Not too painful - COMPLETE!
                return new BeaconMenu(player, playerInventory);
            case 9: // blast furnace
            case 13: // furnace (AbstractFurnaceMenu)
            case 20: // smoker
                return new FurnaceMenu(player, playerInventory); // don't repeat yourself, furnaces act the same without server sided logic
            case 10: // brewing stand
                return new BrewingMenu(player, playerInventory);
            case 12: // enchantment table
                return new EnchantmentMenu(player, playerInventory);
            case 15: // hopper - SimpleContainer
                return new HopperMenu(player, playerInventory);
            case 16: // lectern
                return new LecternMenu(player, playerInventory);
            case 11: // crafting table - somewhat annoying // TODO: Not complete!
                return new CraftingMenu(player, playerInventory);
            case 14: // grindstone // TODO: createResult() not hooked into anything
                //return new GrindstoneMenu(player, playerInventory);
            case 7: // anvil - Painful // TODO: Not complete!
                //return new AnvilMenu(player, playerInventory);
            case 17: // loom // TODO: This requires a LOT of NBT modification
            case 18: // merchant // TODO: Somewhat complicated due to client sided buttons
            case 19: // smithing // TODO: Annoying similar to anvils with crafting recipes
            case 21: // cartographer - // TODO: onCraftedThing, which requires tags
            case 22: // stonecutter - // TODO: Not complete, more annoying recipes
            default:
                return new NotImplementedMenu(player, playerInventory);
        }
    }

    public static AbstractContainerMenu getMenuFromString(GrimPlayer player, Inventory inventory, String legacyType, int slots, int horse) {
        switch (legacyType) {
            case "minecraft:chest":
            case "minecraft:container":
                return new BasicInventoryMenu(player, inventory, slots / 9);
            case "minecraft:crafting_table":
                return new CraftingMenu(player, inventory);
            case "minecraft:dispenser":
            case "minecraft:dropper":
                return new DispenserMenu(player, inventory);
            case "minecraft:enchanting_table":
                return new EnchantmentMenu(player, inventory);
            case "minecraft:brewing_stand":
                return new BrewingMenu(player, inventory);
            case "minecraft:beacon":
                return new BeaconMenu(player, inventory);
            case "minecraft:hopper":
                return new HopperMenu(player, inventory);
            case "minecraft:shulker_box":
                return new BasicInventoryMenu(player, inventory, 3);
            case "EntityHorse":
                return new HorseMenu(player, inventory, slots, horse);
            default: // Villager menu
                return new NotImplementedMenu(player, inventory);
        }
    }
}
