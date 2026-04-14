package ac.grim.grimac.platform.fabric.mc1194.player;

import ac.grim.grimac.platform.fabric.mc1161.player.Fabric1161PlatformInventory;
import ac.grim.grimac.platform.fabric.player.AbstractFabricPlatformPlayer;

/** Uses {@link Fabric1161PlatformInventory} registry-based menu key logic (no extra override). */
public class Fabric1193PlatformInventory extends Fabric1161PlatformInventory {
    public Fabric1193PlatformInventory(AbstractFabricPlatformPlayer player) {
        super(player);
    }
}
