package ac.grim.grimac.utils.inventory.inventory;

import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientClickWindow;
import io.github.retrooper.packetevents.utils.SpigotDataHelper;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.ItemStack;

public class NotImplementedMenu extends AbstractContainerMenu {
    @Override
    public void doClick(int button, int slotID, WrapperPlayClientClickWindow.WindowClickType clickType) {
        resync(player);
    }

    public static void resync(GrimPlayer player) {
        // 0 to 5 is crafting grid
        if (player.bukkitPlayer.getOpenInventory().getTopInventory() instanceof CraftingInventory) {
            CraftingInventory inv = (CraftingInventory) player.bukkitPlayer.getOpenInventory().getTopInventory();
            for (int i = 0; i < 4; i++) {
                player.getInventory().inventory.getPlayerInventory().setItem(i, toItem(inv.getItem(i)));
            }
            player.getInventory().inventory.getPlayerInventory().setItem(4, toItem(inv.getResult()));
        } else {
            for (int i = 0; i < 5; i++) {
                player.getInventory().inventory.getPlayerInventory().setItem(i, com.github.retrooper.packetevents.protocol.item.ItemStack.EMPTY);
            }
        }

        // 5-8 is armor slots
        player.getInventory().inventory.getPlayerInventory().setItem(5, toItem(player.bukkitPlayer.getInventory().getHelmet()));
        player.getInventory().inventory.getPlayerInventory().setItem(6, toItem(player.bukkitPlayer.getInventory().getChestplate()));
        player.getInventory().inventory.getPlayerInventory().setItem(7, toItem(player.bukkitPlayer.getInventory().getLeggings()));
        player.getInventory().inventory.getPlayerInventory().setItem(8, toItem(player.bukkitPlayer.getInventory().getBoots()));

        // 9 - 35 is same on both
        for (int i = 9; i < 36; i++) {
            player.getInventory().inventory.getPlayerInventory().setItem(i, toItem(player.bukkitPlayer.getInventory().getItem(i)));
        }

        // 36-44 is hotbar
        for (int i = 36; i < 45; i++) {
            player.getInventory().inventory.getPlayerInventory().setItem(i, toItem(player.bukkitPlayer.getInventory().getItem(i - 36)));
        }

        // Offhand (for 1.9+)
        if (PacketEvents.getAPI().getServerManager().getVersion().isNewerThanOrEquals(ServerVersion.V_1_9)) {
            player.getInventory().inventory.getPlayerInventory().setItem(45, toItem(player.bukkitPlayer.getInventory().getItemInOffHand()));
        }
    }

    private static com.github.retrooper.packetevents.protocol.item.ItemStack toItem(ItemStack item) {
        return SpigotDataHelper.fromBukkitItemStack(item);
    }
}
