package ac.grim.grimac.utils.inventory.inventory;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.inventory.Inventory;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerOpenWindow;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.Contract;

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
    HORSE(-1),
    UNKNOWN(-1);

    private static final MenuType[] MENU_BY_ID_ARRAY;

    static {
        MenuType[] menuTypes = MenuType.values();

        // Don't iterate the UNKNOWN or HORSE menu type
        MENU_BY_ID_ARRAY = new MenuType[menuTypes.length - 2];

        System.arraycopy(menuTypes, 0, MENU_BY_ID_ARRAY, 0, MENU_BY_ID_ARRAY.length);
    }

    private final int id;

    public static MenuType getMenuType(int id) {
        return getMenuType(id, 27, PacketEvents.getAPI().getServerManager().getVersion().toClientVersion());
    }

    public static MenuType getMenuType(int id, int legacySlots, ClientVersion version) {
        if (version.isOlderThan(ClientVersion.V_1_8)) {
            return switch (id) {
                case 0 -> getGenericContainerType(legacySlots);
                case 1 -> CRAFTING;
                case 2 -> FURNACE;
                case 3, 10 -> GENERIC_3x3;
                case 4 -> ENCHANTMENT;
                case 5 -> BREWING_STAND;
                case 6 -> MERCHANT;
                case 7 -> BEACON;
                case 8 -> ANVIL;
                case 9 -> HOPPER;
                case 11 -> HORSE;
                default -> UNKNOWN;
            };
        }

        if (id < 0) {
            return UNKNOWN;
        }

        int menuIdLimit;
        if (version.isOlderThan(ClientVersion.V_1_20_3)) {
            menuIdLimit = 23;
            if (id >= 7) id++;
        } else {
            menuIdLimit = MENU_BY_ID_ARRAY.length;
        }

        if (id >= menuIdLimit) {
            return UNKNOWN;
        }

        return MENU_BY_ID_ARRAY[id];
    }

    @Contract(pure = true)
    public static MenuType getMenuType(WrapperPlayServerOpenWindow packet, ClientVersion version) {
        if (version.isNewerThanOrEquals(ClientVersion.V_1_8)
                && version.isOlderThanOrEquals(ClientVersion.V_1_13_2)) {
            String legacyType = packet.getLegacyType();

            // yes, these are different
            return packet.getLegacySlots() > 0 ? switch (legacyType) {
                case "minecraft:container", "minecraft:chest" -> getGenericContainerType(packet.getLegacySlots());
                case "minecraft:beacon" -> BEACON;
                case "minecraft:villager" -> MERCHANT;
                case "EntityHorse" -> HORSE;
                case "minecraft:hopper" -> HOPPER;
                case "minecraft:furnace" -> FURNACE;
                case "minecraft:brewing_stand" -> BREWING_STAND;
                case "minecraft:dispenser", "minecraft:dropper" -> GENERIC_3x3;
                case "minecraft:shulker_box" -> version.isNewerThanOrEquals(ClientVersion.V_1_11) ? SHULKER_BOX : UNKNOWN;
                default -> UNKNOWN;
            } : switch (legacyType) {
                case "minecraft:crafting_table" -> CRAFTING;
                case "minecraft:enchanting_table" -> ENCHANTMENT;
                case "minecraft:anvil" -> ANVIL;
                default -> UNKNOWN;
            };
        } else {
            return getMenuType(packet.getType(), packet.getLegacySlots(), version);
        }
    }

    public static MenuType getGenericContainerType(int legacySlots) {
        return switch (legacySlots) {
            case 9 -> GENERIC_9x1;
            case 18 -> GENERIC_9x2;
            case 27 -> GENERIC_9x3;
            case 36 -> GENERIC_9x4;
            case 45 -> GENERIC_9x5;
            case 54 -> GENERIC_9x6;
            default -> UNKNOWN;
        };
    }

    public static AbstractContainerMenu getMenuFromID(GrimPlayer player, Inventory playerInventory, MenuType type) {
        return switch (type) {
            case GENERIC_9x1, GENERIC_9x2, GENERIC_9x3, GENERIC_9x4, GENERIC_9x5, GENERIC_9x6 ->
                    new BasicInventoryMenu(player, playerInventory, type.getId() + 1);
            case SHULKER_BOX ->  new BasicInventoryMenu(player, playerInventory, 3);
            case GENERIC_3x3 -> new DispenserMenu(player, playerInventory);
            case HOPPER -> new HopperMenu(player, playerInventory);
            default -> new NotImplementedMenu(player, playerInventory);
        };
    }

    public static AbstractContainerMenu getMenuFromString(GrimPlayer player, Inventory inventory, String legacyType, int slots, int horse) {
        return switch (legacyType) {
            case "minecraft:chest", "minecraft:container" ->
                    new BasicInventoryMenu(player, inventory, slots / 9);
            case "minecraft:dispenser", "minecraft:dropper" -> new DispenserMenu(player, inventory);
            case "minecraft:hopper" -> new HopperMenu(player, inventory);
            case "minecraft:shulker_box" -> new BasicInventoryMenu(player, inventory, 3);
            default -> // Villager menu
                    new NotImplementedMenu(player, inventory);
        };
    }

}
