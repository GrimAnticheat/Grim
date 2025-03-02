package ac.grim.grimac.utils.nmsutil;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.data.tags.SyncedTags;
import ac.grim.grimac.utils.enums.FluidTag;
import ac.grim.grimac.utils.inventory.EnchantmentHelper;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.attribute.Attributes;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.item.enchantment.type.EnchantmentTypes;
import com.github.retrooper.packetevents.protocol.item.type.ItemType;
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.player.GameMode;
import com.github.retrooper.packetevents.protocol.potion.PotionTypes;
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import com.github.retrooper.packetevents.protocol.world.states.defaulttags.BlockTags;
import com.github.retrooper.packetevents.protocol.world.states.type.StateTypes;
import com.github.retrooper.packetevents.util.Vector3i;

import java.util.OptionalInt;

public class BlockBreakSpeed {
    public static double getBlockDamage(GrimPlayer player, Vector3i position) {
        // GET destroy speed
        // Starts with itemstack get destroy speed
        ItemStack tool = player.getInventory().getHeldItem();
        ItemType toolType = tool.getType();

        WrappedBlockState block = player.compensatedWorld.getBlock(position);
        float blockHardness = block.getType().getHardness();

        // 1.15.2 and below need this hack
        if ((block.getType() == StateTypes.PISTON || block.getType() == StateTypes.PISTON_HEAD || block.getType() == StateTypes.STICKY_PISTON) && player.getClientVersion().isOlderThanOrEquals(ClientVersion.V_1_15_2)) {
            blockHardness = 0.5f;
        }

        if (player.gamemode == GameMode.CREATIVE) {
            // players in creative mode cannot mine with certain items
            if (toolType.hasAttribute(ItemTypes.ItemAttribute.SWORD) || toolType == ItemTypes.TRIDENT
                    || toolType == ItemTypes.DEBUG_STICK && player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_13)
                    || toolType == ItemTypes.MACE && player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_20_5)) {
                return 0;
            }
            // Instabreak
            return 1;
        }

        if (blockHardness == -1) return 0; // Unbreakable block

        boolean isCorrectToolForDrop = false;
        float speedMultiplier = 1.0F;

        // 1.13 and below need their own huge methods to support this...
        if (toolType.hasAttribute(ItemTypes.ItemAttribute.AXE)) {
            isCorrectToolForDrop = player.tagManager.block(SyncedTags.MINEABLE_AXE).contains(block.getType());
        } else if (toolType.hasAttribute(ItemTypes.ItemAttribute.PICKAXE)) {
            isCorrectToolForDrop = player.tagManager.block(SyncedTags.MINEABLE_PICKAXE).contains(block.getType());
        } else if (toolType.hasAttribute(ItemTypes.ItemAttribute.SHOVEL)) {
            isCorrectToolForDrop = player.tagManager.block(SyncedTags.MINEABLE_SHOVEL).contains(block.getType());
        } else if (toolType.hasAttribute(ItemTypes.ItemAttribute.HOE)) {
            isCorrectToolForDrop = player.tagManager.block(SyncedTags.MINEABLE_HOE).contains(block.getType());
        }

        if (isCorrectToolForDrop) {
            int tier = 0;
            if (toolType.hasAttribute(ItemTypes.ItemAttribute.WOOD_TIER)) { // Tier 0
                speedMultiplier = 2.0f;
            } else if (toolType.hasAttribute(ItemTypes.ItemAttribute.STONE_TIER)) { // Tier 1
                speedMultiplier = 4.0f;
                tier = 1;
            } else if (toolType.hasAttribute(ItemTypes.ItemAttribute.IRON_TIER)) { // Tier 2
                speedMultiplier = 6.0f;
                tier = 2;
            } else if (toolType.hasAttribute(ItemTypes.ItemAttribute.DIAMOND_TIER)) { // Tier 3
                speedMultiplier = 8.0f;
                tier = 3;
            } else if (toolType.hasAttribute(ItemTypes.ItemAttribute.GOLD_TIER)) { // Tier 0
                speedMultiplier = 12.0f;
            } else if (toolType.hasAttribute(ItemTypes.ItemAttribute.NETHERITE_TIER)) { // Tier 4
                speedMultiplier = 9.0f;
                tier = 4;
            }

            if (tier < 3 && player.tagManager.block(SyncedTags.NEEDS_DIAMOND_TOOL).contains(block.getType())) {
                isCorrectToolForDrop = false;
            } else if (tier < 2 && player.tagManager.block(SyncedTags.NEEDS_IRON_TOOL).contains(block.getType())) {
                isCorrectToolForDrop = false;
            } else if (tier < 1 && player.tagManager.block(SyncedTags.NEEDS_STONE_TOOL).contains(block.getType())) {
                isCorrectToolForDrop = false;
            }
        }

        // Shears can mine some blocks faster
        if (toolType == ItemTypes.SHEARS) {
            isCorrectToolForDrop = true;

            if (block.getType() == StateTypes.COBWEB || Materials.isLeaves(block.getType())) {
                speedMultiplier = 15.0f;
            } else if (BlockTags.WOOL.contains(block.getType())) {
                speedMultiplier = 5.0f;
            } else if (block.getType() == StateTypes.VINE ||
                    block.getType() == StateTypes.GLOW_LICHEN) {
                speedMultiplier = 2.0f;
            } else {
                isCorrectToolForDrop = block.getType() == StateTypes.COBWEB ||
                        block.getType() == StateTypes.REDSTONE_WIRE ||
                        block.getType() == StateTypes.TRIPWIRE;
            }
        }

        // Swords can also mine some blocks faster
        if (toolType.hasAttribute(ItemTypes.ItemAttribute.SWORD)) {
            if (block.getType() == StateTypes.COBWEB) {
                speedMultiplier = 15.0f;
            } else if (player.tagManager.block(SyncedTags.SWORD_EFFICIENT).contains(block.getType())) {
                speedMultiplier = 1.5f;
            }

            isCorrectToolForDrop = block.getType() == StateTypes.COBWEB;
        }

        if (speedMultiplier > 1.0f) {
            if (player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_21) && PacketEvents.getAPI().getServerManager().getVersion().isNewerThanOrEquals(ServerVersion.V_1_21)) {
                speedMultiplier += (float) player.compensatedEntities.self.getAttributeValue(Attributes.MINING_EFFICIENCY);
            } else {
                int digSpeed = tool.getEnchantmentLevel(EnchantmentTypes.BLOCK_EFFICIENCY, PacketEvents.getAPI().getServerManager().getVersion().toClientVersion());
                if (digSpeed > 0) {
                    speedMultiplier += digSpeed * digSpeed + 1;
                }
            }
        }

        OptionalInt digSpeed = player.compensatedEntities.getPotionLevelForPlayer(PotionTypes.HASTE);
        OptionalInt conduit = player.compensatedEntities.getPotionLevelForPlayer(PotionTypes.CONDUIT_POWER);

        if (digSpeed.isPresent() || conduit.isPresent()) {
            int hasteLevel = Math.max(digSpeed.isEmpty() ? 0 : digSpeed.getAsInt(), conduit.isEmpty() ? 0 : conduit.getAsInt());
            speedMultiplier *= (float) (1 + (0.2 * (hasteLevel + 1)));
        }

        OptionalInt miningFatigue = player.compensatedEntities.getPotionLevelForPlayer(PotionTypes.MINING_FATIGUE);

        if (miningFatigue.isPresent()) {
            switch (miningFatigue.getAsInt()) {
                case 0:
                    speedMultiplier *= 0.3f;
                    break;
                case 1:
                    speedMultiplier *= 0.09f;
                    break;
                case 2:
                    speedMultiplier *= 0.0027f;
                    break;
                default:
                    speedMultiplier *= 0.00081f;
            }
        }

        speedMultiplier *= (float) player.compensatedEntities.self.getAttributeValue(Attributes.BLOCK_BREAK_SPEED);

        if (player.fluidOnEyes == FluidTag.WATER) {
            if (player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_21) && PacketEvents.getAPI().getServerManager().getVersion().isNewerThanOrEquals(ServerVersion.V_1_21)) {
                speedMultiplier *= (float) player.compensatedEntities.self.getAttributeValue(Attributes.SUBMERGED_MINING_SPEED);
            } else {
                if (EnchantmentHelper.getMaximumEnchantLevel(player.getInventory(), EnchantmentTypes.AQUA_AFFINITY, PacketEvents.getAPI().getServerManager().getVersion().toClientVersion()) == 0) {
                    speedMultiplier /= 5;
                }
            }
        }

        if (!player.packetStateData.packetPlayerOnGround) {
            speedMultiplier /= 5;
        }

        float damage = speedMultiplier / blockHardness;

        boolean canHarvest = !block.getType().isRequiresCorrectTool() || isCorrectToolForDrop;
        if (canHarvest) {
            damage /= 30F;
        } else {
            damage /= 100F;
        }

        return damage;
    }
}
