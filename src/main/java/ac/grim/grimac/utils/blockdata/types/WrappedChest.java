package ac.grim.grimac.utils.blockdata.types;

import org.bukkit.Material;
import org.bukkit.block.data.type.Chest;

public class WrappedChest extends WrappedDirectional {
    public static final boolean isModern = ItemTypes.isNewVersion();
    public Chest.Type type;
    public boolean isTrapped;

    public boolean isModern() {
        return isModern;
    }

    public Material getMaterial() {
        return isTrapped ? Material.TRAPPED_CHEST : Material.CHEST;
    }

    public Chest.Type getType() {
        return type;
    }

    public void setType(Chest.Type type) {
        this.type = type;
    }

    public void setTrapped(boolean isTrapped) {
        this.isTrapped = isTrapped;
    }
}
