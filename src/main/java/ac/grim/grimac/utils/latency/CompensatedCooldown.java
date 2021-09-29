package ac.grim.grimac.utils.latency;

import ac.grim.grimac.checks.type.PositionCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.update.PositionUpdate;
import org.bukkit.Material;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// Applies a cooldown period to all items with the given type. Used by the Notchian server with enderpearls.
// This packet should be sent when the cooldown starts and also when the cooldown ends (to compensate for lag),
// although the client will end the cooldown automatically. Can be applied to any item,
// note that interactions still get sent to the server with the item but the client does not play the animation
// nor attempt to predict results (i.e block placing).
public class CompensatedCooldown extends PositionCheck {
    private final ConcurrentHashMap<Material, Integer> itemCooldownMap = new ConcurrentHashMap<>();

    public CompensatedCooldown(GrimPlayer playerData) {
        super(playerData);
    }

    @Override
    public void onPositionUpdate(final PositionUpdate positionUpdate) {
        for (Iterator<Map.Entry<Material, Integer>> it = itemCooldownMap.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<Material, Integer> entry = it.next();
            entry.setValue(entry.getValue() - 1);
            // The client will automatically remove cooldowns after enough time
            if (entry.getValue() <= 0) it.remove();
        }
    }

    // all the same to us... having a cooldown or not having one
    public boolean hasMaterial(Material item) {
        return itemCooldownMap.containsKey(item);
    }

    public void addCooldown(Material item, int cooldown) {
        if (cooldown == 0) {
            removeCooldown(item);
            return;
        }

        itemCooldownMap.put(item, cooldown);
    }

    private void removeCooldown(Material item) {
        itemCooldownMap.remove(item);
    }
}
