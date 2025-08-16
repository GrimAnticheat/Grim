package ac.grim.grimac.utils.item;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.latency.CompensatedWorld;
import com.github.retrooper.packetevents.protocol.component.ComponentTypes;
import com.github.retrooper.packetevents.protocol.component.builtin.item.FoodProperties;
import com.github.retrooper.packetevents.protocol.component.builtin.item.ItemBlocksAttacks;
import com.github.retrooper.packetevents.protocol.component.builtin.item.ItemConsumable;
import com.github.retrooper.packetevents.protocol.component.builtin.item.ItemEquippable;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.player.GameMode;
import com.github.retrooper.packetevents.protocol.player.InteractionHand;

public class ItemBehaviour {

    public static final ItemBehaviour INSTANCE = new ItemBehaviour();

    public boolean canUse(ItemStack item, CompensatedWorld world, GrimPlayer player, InteractionHand hand) {
        ItemConsumable consumable = item.getComponentOr(ComponentTypes.CONSUMABLE, null);
        if (consumable != null) {
            return this.testConsumableComponent(item, world, player, hand, consumable);
        } else {
            if (player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_21_5)) {
                final ItemBlocksAttacks blocksAttacks = item.getComponentOr(ComponentTypes.BLOCKS_ATTACKS, null);
                final ItemEquippable equippable = item.getComponentOr(ComponentTypes.EQUIPPABLE, null);

                return (equippable == null || !equippable.isSwappable()) && blocksAttacks != null;
            }

            return false;
        }
    }

    protected boolean testConsumableComponent(ItemStack item, CompensatedWorld world, GrimPlayer player, InteractionHand hand, ItemConsumable consumable) {
        if (!this.testFoodComponent(item, world, player, hand)) {
            return false;
        }

        return (consumable.getConsumeSeconds() * 20.0F) > 0;
    }

    protected boolean testFoodComponent(ItemStack item, CompensatedWorld world, GrimPlayer player, InteractionHand hand) {
        FoodProperties foodProperties = item.getComponentOr(ComponentTypes.FOOD, null);
        return foodProperties != null ? foodProperties.isCanAlwaysEat() || player.food < 20 || player.gamemode == GameMode.CREATIVE : true;
    }
}
