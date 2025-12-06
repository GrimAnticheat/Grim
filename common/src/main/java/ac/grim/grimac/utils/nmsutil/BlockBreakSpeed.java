package ac.grim.grimac.utils.nmsutil;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.data.tags.SyncedTag;
import ac.grim.grimac.utils.data.tags.SyncedTags;
import ac.grim.grimac.utils.enums.FluidTag;
import ac.grim.grimac.utils.inventory.EnchantmentHelper;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.attribute.Attributes;
import com.github.retrooper.packetevents.protocol.component.ComponentTypes;
import com.github.retrooper.packetevents.protocol.component.builtin.item.ItemTool;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.item.enchantment.type.EnchantmentTypes;
import com.github.retrooper.packetevents.protocol.item.type.ItemType;
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes;
import com.github.retrooper.packetevents.protocol.mapper.MappedEntitySet;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.player.GameMode;
import com.github.retrooper.packetevents.protocol.potion.PotionTypes;
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import com.github.retrooper.packetevents.protocol.world.states.defaulttags.BlockTags;
import com.github.retrooper.packetevents.protocol.world.states.type.StateType;
import com.github.retrooper.packetevents.protocol.world.states.type.StateTypes;
import com.github.retrooper.packetevents.resources.ResourceLocation;
import com.google.common.collect.Sets;
import lombok.experimental.UtilityClass;

import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;

@UtilityClass
public class BlockBreakSpeed {
    // temporary hardcode to workaround PE bug https://github.com/retrooper/packetevents/issues/1217; see https://github.com/GrimAnticheat/Grim/issues/2117
    private static final Set<StateType> HARVESTABLE_TYPES_1_21_4 = Sets.newHashSet(
            StateTypes.BELL,
            StateTypes.LANTERN,
            StateTypes.SOUL_LANTERN,
            StateTypes.COPPER_DOOR,
            StateTypes.EXPOSED_COPPER_DOOR,
            StateTypes.OXIDIZED_COPPER_DOOR,
            StateTypes.WEATHERED_COPPER_DOOR,
            StateTypes.WAXED_COPPER_DOOR,
            StateTypes.WAXED_EXPOSED_COPPER_DOOR,
            StateTypes.WAXED_OXIDIZED_COPPER_DOOR,
            StateTypes.WAXED_WEATHERED_COPPER_DOOR,
            StateTypes.IRON_DOOR,
            StateTypes.HEAVY_WEIGHTED_PRESSURE_PLATE,
            StateTypes.LIGHT_WEIGHTED_PRESSURE_PLATE,
            StateTypes.POLISHED_BLACKSTONE_PRESSURE_PLATE,
            StateTypes.STONE_PRESSURE_PLATE,
            StateTypes.BREWING_STAND,
            StateTypes.ENDER_CHEST
    );

    private static final boolean SERVER_USES_COMPONENTS_AND_RULES = PacketEvents.getAPI().getServerManager().getVersion().isNewerThanOrEquals(ServerVersion.V_1_20_5);

    record ToolSpeedData(float speedMultiplier, boolean isCorrectToolForDrop) {
    }

    public static double getBlockDamage(GrimPlayer player, WrappedBlockState block) {
        ItemStack tool = player.inventory.getHeldItem();
        return getBlockDamage(player, tool, block.getType());
    }

    public static double getBlockDamage(GrimPlayer player, ItemStack tool, StateType block) {
        // GET destroy speed
        // Starts with itemstack get destroy speed
        ItemType toolType = tool.getType();

        if (player.gamemode == GameMode.CREATIVE) {
            if (SERVER_USES_COMPONENTS_AND_RULES && player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_21_5)) {
                return tool.getComponent(ComponentTypes.TOOL)
                        .map(ItemTool::isCanDestroyBlocksInCreative)
                        .orElse(true) ? 1 : 0;
            } else {
                if (toolType.hasAttribute(ItemTypes.ItemAttribute.SWORD) || toolType == ItemTypes.TRIDENT
                        || (toolType == ItemTypes.DEBUG_STICK && player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_13))
                        || (toolType == ItemTypes.MACE && player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_20_5))) {
                    return 0;
                }
                return 1;
            }
        }

        float blockHardness = block.getHardness();

        // 1.15.2 and below need this hack
        if ((block == StateTypes.PISTON || block == StateTypes.PISTON_HEAD || block == StateTypes.STICKY_PISTON) && player.getClientVersion().isOlderThanOrEquals(ClientVersion.V_1_15_2)) {
            blockHardness = 0.5f;
        }

        if (blockHardness == -1) return 0; // Unbreakable block

        final ToolSpeedData toolSpeedData;
        if (SERVER_USES_COMPONENTS_AND_RULES && player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_20_5)) {
            toolSpeedData = getModernToolSpeedData(player, tool, block);
        } else {
            toolSpeedData = getLegacyToolSpeedData(player, tool, block);
        }

        final float speedMultiplier = getSpeedMultiplierFromToolData(player, tool, toolSpeedData);

        final boolean canHarvest = !block.isRequiresCorrectTool() || toolSpeedData.isCorrectToolForDrop
                // temporary hardcode to workaround PE bug https://github.com/retrooper/packetevents/issues/1217; see https://github.com/GrimAnticheat/Grim/issues/2091
                || player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_21_4) && HARVESTABLE_TYPES_1_21_4.contains(block);

        float damage = speedMultiplier / blockHardness;
        damage /= canHarvest ? 30F : 100F;
        return damage;
    }

    private static float getSpeedMultiplierFromToolData(GrimPlayer player, ItemStack tool, ToolSpeedData data) {
        float speedMultiplier = data.speedMultiplier;

        if (speedMultiplier > 1.0f) {
            if (player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_21) && PacketEvents.getAPI().getServerManager().getVersion().isNewerThanOrEquals(ServerVersion.V_1_21)) {
                speedMultiplier += (float) player.compensatedEntities.self.getAttributeValue(Attributes.MINING_EFFICIENCY);
            } else {
                int digSpeed = tool.getEnchantmentLevel(EnchantmentTypes.BLOCK_EFFICIENCY);
                if (digSpeed > 0) {
                    speedMultiplier += digSpeed * digSpeed + 1;
                }
            }
        }

        OptionalInt digSpeed = player.compensatedEntities.getPotionLevelForSelfPlayer(PotionTypes.HASTE);
        OptionalInt conduit = player.compensatedEntities.getPotionLevelForSelfPlayer(PotionTypes.CONDUIT_POWER);

        if (digSpeed.isPresent() || conduit.isPresent()) {
            int hasteLevel = Math.max(digSpeed.isEmpty() ? 0 : digSpeed.getAsInt(), conduit.isEmpty() ? 0 : conduit.getAsInt());
            speedMultiplier *= (float) (1 + (0.2 * (hasteLevel + 1)));
        }

        OptionalInt miningFatigue = player.compensatedEntities.getPotionLevelForSelfPlayer(PotionTypes.MINING_FATIGUE);

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
                if (EnchantmentHelper.getMaximumEnchantLevel(player.inventory, EnchantmentTypes.AQUA_AFFINITY) == 0) {
                    speedMultiplier /= 5;
                }
            }
        }

        if (!player.packetStateData.packetPlayerOnGround) {
            speedMultiplier /= 5;
        }

        return speedMultiplier;
    }

    // TODO technically its possible to use packet level manipulation to enforce Tool rules on newer clients on older servers
    // But I've yet to hear of anyone even trying to do such a thing rather than just update the server
    // And we can't support this because we don't see the tool components/data before Via
    private static ToolSpeedData getModernToolSpeedData(GrimPlayer player, ItemStack tool, StateType block) {
        Optional<ItemTool> toolComponentOpt = tool.getComponent(ComponentTypes.TOOL);
        float speedMultiplier = 1.0f;
        boolean isCorrectToolForDrop = false;
        if (toolComponentOpt.isPresent()) {
            ItemTool itemTool = toolComponentOpt.get();

            // Initialize with final default values. These will be used if the loop doesn't find a value.
            // isCorrectToolForDrop is already set to false, no need to set again as default
            speedMultiplier = itemTool.getDefaultMiningSpeed();

            boolean speedFound = false;
            boolean dropsFound = false;

            for (ItemTool.Rule rule : itemTool.getRules()) {
                MappedEntitySet<StateType.Mapped> predicate = rule.getBlocks();
                ResourceLocation tagKey = predicate.getTagKey();
                boolean isMatch;

                // First, determine if the current rule even applies to this block.
                if (tagKey != null) {
                    SyncedTag<StateType> playerTag = player.tagManager.block(tagKey);
                    isMatch = (playerTag != null && playerTag.contains(block))
                            || BlockTags.getByName(tagKey.getKey()).contains(block);
                } else {
                    isMatch = predicate.getEntities().contains(block.getMapped());
                }

                // If the rule matches the block, check if we still need its properties.
                if (isMatch) {
                    // Check for speed if we haven't found it yet.
                    if (!speedFound && rule.getSpeed() != null) {
                        speedMultiplier = rule.getSpeed();
                        speedFound = true;
                    }

                    // Check for drops if we haven't found it yet.
                    if (!dropsFound && rule.getCorrectForDrops() != null) {
                        isCorrectToolForDrop = rule.getCorrectForDrops();
                        dropsFound = true;
                    }
                }

                if (speedFound && dropsFound) {
                    break;
                }
            }
        }
        return new ToolSpeedData(speedMultiplier, isCorrectToolForDrop);
    }

    private static ToolSpeedData getLegacyToolSpeedData(GrimPlayer player, ItemStack tool, StateType block) {
        ItemType toolType = tool.getType();
        float speedMultiplier = 1.0f;
        boolean isCorrectToolForDrop = false;
        // 1.13 and below need their own huge methods to support this...
        if (toolType.hasAttribute(ItemTypes.ItemAttribute.AXE)) {
            isCorrectToolForDrop = player.tagManager.block(SyncedTags.MINEABLE_AXE).contains(block);
        } else if (toolType.hasAttribute(ItemTypes.ItemAttribute.PICKAXE)) {
            isCorrectToolForDrop = player.tagManager.block(SyncedTags.MINEABLE_PICKAXE).contains(block);
        } else if (toolType.hasAttribute(ItemTypes.ItemAttribute.SHOVEL)) {
            isCorrectToolForDrop = player.tagManager.block(SyncedTags.MINEABLE_SHOVEL).contains(block);
        } else if (toolType.hasAttribute(ItemTypes.ItemAttribute.HOE)) {
            isCorrectToolForDrop = player.tagManager.block(SyncedTags.MINEABLE_HOE).contains(block);
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

            if (tier < 3 && player.tagManager.block(SyncedTags.NEEDS_DIAMOND_TOOL).contains(block)) {
                isCorrectToolForDrop = false;
            } else if (tier < 2 && player.tagManager.block(SyncedTags.NEEDS_IRON_TOOL).contains(block)) {
                isCorrectToolForDrop = false;
            } else if (tier < 1 && player.tagManager.block(SyncedTags.NEEDS_STONE_TOOL).contains(block)) {
                isCorrectToolForDrop = false;
            }
        }

        // Shears can mine some blocks faster
        if (toolType == ItemTypes.SHEARS) {
            isCorrectToolForDrop = true;

            if (block == StateTypes.COBWEB || Materials.isLeaves(block)) {
                speedMultiplier = 15.0f;
            } else if (BlockTags.WOOL.contains(block)) {
                speedMultiplier = 5.0f;
            } else if (block == StateTypes.VINE ||
                    block == StateTypes.GLOW_LICHEN) {
                speedMultiplier = 2.0f;
            } else {
                isCorrectToolForDrop = block == StateTypes.COBWEB ||
                        block == StateTypes.REDSTONE_WIRE ||
                        block == StateTypes.TRIPWIRE;
            }
        }

        // Swords can also mine some blocks faster
        if (toolType.hasAttribute(ItemTypes.ItemAttribute.SWORD)) {
            if (block == StateTypes.COBWEB) {
                speedMultiplier = 15.0f;
            } else if (player.tagManager.block(SyncedTags.SWORD_EFFICIENT).contains(block)) {
                speedMultiplier = 1.5f;
            }

            isCorrectToolForDrop = block == StateTypes.COBWEB;
        }
        //
        return new ToolSpeedData(speedMultiplier, isCorrectToolForDrop);
    }


}
