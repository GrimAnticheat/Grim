package ac.grim.grimac.utils.blockstate;

import org.bukkit.Material;

public interface BaseBlockState {
    Material getMaterial();

    int getCombinedId();
}
