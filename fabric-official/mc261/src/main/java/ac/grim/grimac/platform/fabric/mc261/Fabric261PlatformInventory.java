package ac.grim.grimac.platform.fabric.mc261;

import ac.grim.grimac.platform.fabric.player.AbstractFabricPlatformInventory;
import ac.grim.grimac.platform.fabric.player.AbstractFabricPlatformPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.InventoryMenu;

public class Fabric261PlatformInventory extends AbstractFabricPlatformInventory {
    public Fabric261PlatformInventory(AbstractFabricPlatformPlayer player) {
        super(player);
    }

    @Override
    public String getOpenInventoryKey() {
        // Minimal 26.X stub mirroring the bukkit-key convention. Phase C will
        // map the full MenuType registry the way intermediary's
        // Fabric1161PlatformInventory does (Registry.MENU.getKey(type) →
        // Identifier.path), once we wire BuiltInRegistries / Registries lookup
        // on the 26.X namespace. For now: distinguish CRAFTING / CREATIVE /
        // fallback to handler class name so the engine has stable strings.
        AbstractContainerMenu menu = fabricPlatformPlayer.getNative().containerMenu;
        if (menu instanceof InventoryMenu) return "CRAFTING";
        if (fabricPlatformPlayer.getNative().isCreative()) return "CREATIVE";
        return menu.getClass().getSimpleName();
    }
}
