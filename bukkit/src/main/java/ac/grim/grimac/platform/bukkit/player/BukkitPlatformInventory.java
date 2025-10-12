package ac.grim.grimac.platform.bukkit.player;

import ac.grim.grimac.platform.api.player.PlatformInventory;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import lombok.RequiredArgsConstructor;
import org.bukkit.entity.Player;

@RequiredArgsConstructor
public class BukkitPlatformInventory implements PlatformInventory {

    private final Player bukkitPlayer;

    @Override
    public ItemStack getItemInHand() {
        return SpigotConversionUtil.fromBukkitItemStack(bukkitPlayer.getInventory().getItemInHand());
    }

    @Override
    public ItemStack getItemInOffHand() {
        return SpigotConversionUtil.fromBukkitItemStack(bukkitPlayer.getInventory().getItemInOffHand());
    }

    @Override
    public ItemStack getStack(int bukkitSlot, int vanillaSlot) {
        return SpigotConversionUtil.fromBukkitItemStack(bukkitPlayer.getInventory().getItem(bukkitSlot));
    }

    @Override
    public ItemStack getHelmet() {
        return SpigotConversionUtil.fromBukkitItemStack(bukkitPlayer.getInventory().getHelmet());
    }

    @Override
    public ItemStack getChestplate() {
        return SpigotConversionUtil.fromBukkitItemStack(bukkitPlayer.getInventory().getChestplate());
    }

    @Override
    public ItemStack getLeggings() {
        return SpigotConversionUtil.fromBukkitItemStack(bukkitPlayer.getInventory().getLeggings());
    }

    @Override
    public ItemStack getBoots() {
        return SpigotConversionUtil.fromBukkitItemStack(bukkitPlayer.getInventory().getBoots());
    }

    @Override
    public ItemStack[] getContents() {
        org.bukkit.inventory.ItemStack[] bukkitItems = bukkitPlayer.getInventory().getContents();
        ItemStack[] items = new ItemStack[bukkitItems.length];
        for (int i = 0; i < bukkitItems.length; i++) {
            if (bukkitItems[i] == null) continue;
            items[i] = SpigotConversionUtil.fromBukkitItemStack(bukkitItems[i]);
        }
        return items;
    }

    @Override
    public String getOpenInventoryKey() {
        return bukkitPlayer.getOpenInventory().getType().toString();
    }
}
