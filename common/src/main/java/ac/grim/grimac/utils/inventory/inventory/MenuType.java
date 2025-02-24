package ac.grim.grimac.utils.inventory.inventory;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.inventory.Inventory;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum MenuType {
    GENERIC_9x1(0),
    GENERIC_9x2(1),
    GENERIC_9x3(2),
    GENERIC_9x4(3),
    GENERIC_9x5(4),
    GENERIC_9x6(5),
    GENERIC_3x3(6),
    CRAFTER_3x3(7), // only in versions 1.20.3 & greater
    ANVIL(8),
    BEACON(9),
    BLAST_FURNACE(10),
    BREWING_STAND(11),
    CRAFTING(12),
    ENCHANTMENT(13),
    FURNACE(14),
    GRINDSTONE(15),
    HOPPER(16),
    LECTERN(17),
    LOOM(18),
    MERCHANT(19),
    SHULKER_BOX(20),
    SMITHING(21),
    SMOKER(22),
    CARTOGRAPHY_TABLE(23),
    STONECUTTER(24),
    UNKNOWN(-1);

    private final int id;

    private static final MenuType[] MENU_BY_ID_ARRAY;
    static {
        ServerVersion version = PacketEvents.getAPI().getServerManager().getVersion();
        MenuType[] menuTypes = MenuType.values();

        int menuIdLimit;

        if (version.isOlderThan(ServerVersion.V_1_20_3)) {
            // versions under 1.20.3
            menuIdLimit = 23;
        } else {
            // 1.20.3 & greater
            menuIdLimit = menuTypes.length - 1; // Don't iterate the UNKNOWN menu type
        }

        MENU_BY_ID_ARRAY = new MenuType[menuIdLimit];

        for (int itr = 0; itr < menuIdLimit; itr++) {
            MENU_BY_ID_ARRAY[itr] = menuTypes[itr];
        }
    }

    public static MenuType getMenuType(int id) {
        if (id < 0) {
            return UNKNOWN;
        }

        ServerVersion version = PacketEvents.getAPI().getServerManager().getVersion();
        // versions under 1.20.3
        if (version.isOlderThan(ServerVersion.V_1_20_3)) { // TODO: Can this be moved to the static block?
            if (id >= 7) {
                id++;
            }
        }

        if (id >= MENU_BY_ID_ARRAY.length) {
            return UNKNOWN;
        }

        return MENU_BY_ID_ARRAY[id];
    }

    public static AbstractContainerMenu getMenuFromID(GrimPlayer player, Inventory playerInventory, MenuType type) {
        return switch (type) {
            case GENERIC_9x1, GENERIC_9x2, GENERIC_9x3, GENERIC_9x4, GENERIC_9x5, GENERIC_9x6 ->
                    new BasicInventoryMenu(player, playerInventory, type.getId() + 1);
            case GENERIC_3x3 -> new DispenserMenu(player, playerInventory);
            case HOPPER -> new HopperMenu(player, playerInventory);
            default -> new NotImplementedMenu(player, playerInventory);
        };
    }

    public static AbstractContainerMenu getMenuFromString(GrimPlayer player, Inventory inventory, String legacyType, int slots, int horse) {
        return switch (legacyType) {
            case "minecraft:chest", "minecraft:container" -> new BasicInventoryMenu(player, inventory, slots / 9);
            case "minecraft:dispenser", "minecraft:dropper" -> new DispenserMenu(player, inventory);
            case "minecraft:hopper" -> new HopperMenu(player, inventory);
            case "minecraft:shulker_box" -> new BasicInventoryMenu(player, inventory, 3);
            default -> // Villager menu
                    new NotImplementedMenu(player, inventory);
        };
    }

}
