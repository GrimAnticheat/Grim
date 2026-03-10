package ac.grim.grimac.utils.item;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.latency.CompensatedWorld;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.player.InteractionHand;

public class AlwaysUseItem extends ItemBehaviour {

    public static final AlwaysUseItem INSTANCE = new AlwaysUseItem();

    @Override
    public boolean canUse(ItemStack item, CompensatedWorld world, GrimPlayer player, InteractionHand hand) {
        return true;
    }

}
