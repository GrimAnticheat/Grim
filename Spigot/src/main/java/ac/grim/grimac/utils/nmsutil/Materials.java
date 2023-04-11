package ac.grim.grimac.utils.nmsutil;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.item.type.ItemType;
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import com.github.retrooper.packetevents.protocol.world.states.defaulttags.BlockTags;
import com.github.retrooper.packetevents.protocol.world.states.type.StateType;
import com.github.retrooper.packetevents.protocol.world.states.type.StateTypes;
import com.github.retrooper.packetevents.protocol.world.states.type.StateValue;

import java.util.HashSet;
import java.util.Set;

public class Materials {
    private static final Set<StateType> NO_PLACE_LIQUIDS = new HashSet<>();
    // Includes iron panes in addition to glass panes
    private static final Set<StateType> PANES = new HashSet<>();
    private static final Set<StateType> WATER_LIQUIDS = new HashSet<>();
    private static final Set<StateType> WATER_LIQUIDS_LEGACY = new HashSet<>();
    private static final Set<StateType> WATER_SOURCES = new HashSet<>();
    private static final Set<StateType> WATER_SOURCES_LEGACY = new HashSet<>();

    private static final Set<StateType> CLIENT_SIDE = new HashSet<>();

    static {
        // Base water, flowing on 1.12- but not on 1.13+ servers
        WATER_LIQUIDS.add(StateTypes.WATER);
        WATER_LIQUIDS_LEGACY.add(StateTypes.WATER);

        // Becomes grass for legacy versions
        WATER_LIQUIDS.add(StateTypes.KELP);
        WATER_SOURCES.add(StateTypes.KELP);
        WATER_LIQUIDS.add(StateTypes.KELP_PLANT);
        WATER_SOURCES.add(StateTypes.KELP_PLANT);

        // Is translated to air for legacy versions
        WATER_SOURCES.add(StateTypes.BUBBLE_COLUMN);
        WATER_LIQUIDS_LEGACY.add(StateTypes.BUBBLE_COLUMN);
        WATER_LIQUIDS.add(StateTypes.BUBBLE_COLUMN);
        WATER_SOURCES_LEGACY.add(StateTypes.BUBBLE_COLUMN);

        // This is not water on 1.12- players
        WATER_SOURCES.add(StateTypes.SEAGRASS);
        WATER_LIQUIDS.add(StateTypes.SEAGRASS);

        // This is not water on 1.12- players`
        WATER_SOURCES.add(StateTypes.TALL_SEAGRASS);
        WATER_LIQUIDS.add(StateTypes.TALL_SEAGRASS);

        NO_PLACE_LIQUIDS.add(StateTypes.WATER);
        NO_PLACE_LIQUIDS.add(StateTypes.LAVA);

        // Important blocks where we need to ignore right-clicking on for placing blocks
        // We can ignore stuff like right-clicking a pumpkin with shears...
        CLIENT_SIDE.add(StateTypes.BARREL);
        CLIENT_SIDE.add(StateTypes.BEACON);
        CLIENT_SIDE.add(StateTypes.BREWING_STAND);
        CLIENT_SIDE.add(StateTypes.CARTOGRAPHY_TABLE);
        CLIENT_SIDE.add(StateTypes.CHEST);
        CLIENT_SIDE.add(StateTypes.TRAPPED_CHEST);
        CLIENT_SIDE.add(StateTypes.COMPARATOR);
        CLIENT_SIDE.add(StateTypes.CRAFTING_TABLE);
        CLIENT_SIDE.add(StateTypes.DAYLIGHT_DETECTOR);
        CLIENT_SIDE.add(StateTypes.DISPENSER);
        CLIENT_SIDE.add(StateTypes.DRAGON_EGG);
        CLIENT_SIDE.add(StateTypes.ENCHANTING_TABLE);
        CLIENT_SIDE.add(StateTypes.ENDER_CHEST);
        CLIENT_SIDE.add(StateTypes.GRINDSTONE);
        CLIENT_SIDE.add(StateTypes.HOPPER);
        CLIENT_SIDE.add(StateTypes.LEVER);
        CLIENT_SIDE.add(StateTypes.LIGHT);
        CLIENT_SIDE.add(StateTypes.LOOM);
        CLIENT_SIDE.add(StateTypes.NOTE_BLOCK);
        CLIENT_SIDE.add(StateTypes.REPEATER);
        CLIENT_SIDE.add(StateTypes.SMITHING_TABLE);
        CLIENT_SIDE.add(StateTypes.STONECUTTER);
        CLIENT_SIDE.add(StateTypes.LECTERN);
        CLIENT_SIDE.add(StateTypes.FURNACE);
        CLIENT_SIDE.add(StateTypes.BLAST_FURNACE);

        CLIENT_SIDE.addAll(BlockTags.FENCE_GATES.getStates());
        CLIENT_SIDE.addAll(BlockTags.ANVIL.getStates());
        CLIENT_SIDE.addAll(BlockTags.BEDS.getStates());
        CLIENT_SIDE.addAll(BlockTags.BUTTONS.getStates());
        CLIENT_SIDE.addAll(BlockTags.SHULKER_BOXES.getStates());
        CLIENT_SIDE.addAll(BlockTags.SIGNS.getStates());
        CLIENT_SIDE.addAll(BlockTags.FLOWER_POTS.getStates());
        CLIENT_SIDE.addAll(BlockTags.WOODEN_TRAPDOORS.getStates());
        CLIENT_SIDE.addAll(BlockTags.WOODEN_DOORS.getStates());

        PANES.addAll(BlockTags.GLASS_PANES.getStates());
        PANES.add(StateTypes.IRON_BARS);
    }

    public static boolean isStairs(StateType type) {
        return BlockTags.STAIRS.contains(type);
    }

    public static boolean isSlab(StateType type) {
        return BlockTags.SLABS.contains(type);
    }

    public static boolean isWall(StateType type) {
        return BlockTags.WALLS.contains(type);
    }

    public static boolean isButton(StateType type) {
        return BlockTags.BUTTONS.contains(type);
    }

    public static boolean isFence(StateType type) {
        return BlockTags.FENCES.contains(type);
    }

    public static boolean isGate(StateType type) {
        return BlockTags.FENCE_GATES.contains(type);
    }

    public static boolean isBed(StateType type) {
        return BlockTags.BEDS.contains(type);
    }

    public static boolean isAir(StateType type) {
        return type.isAir();
    }

    public static boolean isLeaves(StateType type) {
        return BlockTags.LEAVES.contains(type);
    }

    public static boolean isDoor(StateType type) {
        return BlockTags.DOORS.contains(type);
    }

    public static boolean isShulker(StateType type) {
        return BlockTags.SHULKER_BOXES.contains(type);
    }

    public static boolean isGlassBlock(StateType type) {
        return BlockTags.GLASS_BLOCKS.contains(type);
    }

    public static Set<StateType> getPanes() {
        return new HashSet<>(PANES);
    }

    public static boolean isGlassPane(StateType type) {
        return PANES.contains(type);
    }

    public static boolean isClimbable(StateType type) {
        return BlockTags.CLIMBABLE.contains(type);
    }

    public static boolean isCauldron(StateType type) {
        return BlockTags.CAULDRONS.contains(type);
    }

    public static boolean isWaterModern(StateType type) {
        return WATER_LIQUIDS.contains(type);
    }

    public static boolean isWaterLegacy(StateType type) {
        return WATER_LIQUIDS_LEGACY.contains(type);
    }

    public static boolean isShapeExceedsCube(StateType type) {
        return type.exceedsCube();
    }

    public static boolean isUsable(ItemType material) {
        return material != null && (material.hasAttribute(ItemTypes.ItemAttribute.EDIBLE) || material == ItemTypes.POTION || material == ItemTypes.MILK_BUCKET
                || material == ItemTypes.CROSSBOW || material == ItemTypes.BOW || material.toString().endsWith("SWORD")
                || material == ItemTypes.TRIDENT || material == ItemTypes.SHIELD);
    }

    public static boolean isWater(ClientVersion clientVersion, WrappedBlockState state) {
        boolean modern = clientVersion.isNewerThanOrEquals(ClientVersion.V_1_13);

        if (modern && isWaterModern(state.getType())) {
            return true;
        }

        if (!modern && isWaterLegacy(state.getType())) {
            return true;
        }

        return isWaterlogged(clientVersion, state);
    }

    public static boolean isWaterSource(ClientVersion clientVersion, WrappedBlockState state) {
        if (isWaterlogged(clientVersion, state)) {
            return true;
        }
        if (state.getType() == StateTypes.WATER && state.getLevel() == 0) {
            return true;
        }
        boolean modern = clientVersion.isNewerThanOrEquals(ClientVersion.V_1_13);
        return modern ? WATER_SOURCES.contains(state.getType()) : WATER_SOURCES_LEGACY.contains(state.getType());
    }

    public static boolean isWaterlogged(ClientVersion clientVersion, WrappedBlockState state) {
        if (clientVersion.isOlderThanOrEquals(ClientVersion.V_1_12_2)) return false;
        if (PacketEvents.getAPI().getServerManager().getVersion().isOlderThan(ServerVersion.V_1_13)) return false;

        StateType type = state.getType();

        // Waterlogged lanterns were added in 1.16.2
        if (clientVersion.isOlderThan(ClientVersion.V_1_16_2) && (type == StateTypes.LANTERN || type == StateTypes.SOUL_LANTERN))
            return false;
        // ViaVersion small dripleaf -> fern (not waterlogged)
        if (clientVersion.isOlderThan(ClientVersion.V_1_17) && type == StateTypes.SMALL_DRIPLEAF)
            return false;
        // Waterlogged rails were added in 1.17
        if (clientVersion.isOlderThan(ClientVersion.V_1_17) && BlockTags.RAILS.contains(type))
            return false;
        // Nice check to see if waterlogged :)
        return (boolean) state.getInternalData().getOrDefault(StateValue.WATERLOGGED, false);
    }

    public static boolean isPlaceableWaterBucket(ItemType mat) {
        return mat == ItemTypes.AXOLOTL_BUCKET || mat == ItemTypes.COD_BUCKET || mat == ItemTypes.PUFFERFISH_BUCKET
                || mat == ItemTypes.SALMON_BUCKET || mat == ItemTypes.TROPICAL_FISH_BUCKET || mat == ItemTypes.WATER_BUCKET
                || mat == ItemTypes.TADPOLE_BUCKET;
    }

    public static StateType transformBucketMaterial(ItemType mat) {
        if (mat == ItemTypes.LAVA_BUCKET) return StateTypes.LAVA;
        if (isPlaceableWaterBucket(mat)) return StateTypes.WATER;
        return null;
    }

    // We are taking a shortcut here for the sake of speed and reducing world lookups
    // As we have already assumed that the player does not have water at this block
    // We do not have to track all the version differences in terms of looking for water
    // For 1.7-1.12 clients, it is safe to check SOLID_BLACKLIST directly
    public static boolean isSolidBlockingBlacklist(StateType mat, ClientVersion ver) {
        // Thankfully Mojang has not changed this code much across versions
        // There very likely is a few lurking issues though, I've done my best but can't thoroughly compare 11 versions
        // but from a look, Mojang seems to keep this definition consistent throughout their game (thankfully)
        //
        // What I do is look at 1.8, 1.12, and 1.17 source code, and when I see a difference, I find the version
        // that added it.  I could have missed something if something was added to the blacklist in 1.9 but
        // was removed from it in 1.10 (although this is unlikely as the blacklist rarely changes)
        if (!mat.isBlocking()) return true;

        // 1.13-1.15 had banners on the blacklist - removed in 1.16, not implemented in 1.12 and below
        if (BlockTags.BANNERS.contains(mat))
            return ver.isNewerThanOrEquals(ClientVersion.V_1_13) && ver.isOlderThan(ClientVersion.V_1_16);

        return false;
    }

    public static boolean isAnvil(StateType mat) {
        return BlockTags.ANVIL.contains(mat);
    }

    public static boolean isWoodenChest(StateType mat) {
        return mat == StateTypes.CHEST || mat == StateTypes.TRAPPED_CHEST;
    }

    public static boolean isNoPlaceLiquid(StateType material) {
        return NO_PLACE_LIQUIDS.contains(material);
    }

    public static boolean isWaterIgnoringWaterlogged(ClientVersion clientVersion, WrappedBlockState state) {
        if (clientVersion.isNewerThanOrEquals(ClientVersion.V_1_13)) return isWaterModern(state.getType());
        return isWaterLegacy(state.getType());
    }

    public static boolean isClientSideInteractable(StateType material) {
        return CLIENT_SIDE.contains(material);
    }

    public static boolean isCompostable(ItemType material) {
        // This 3772 character line was auto generated
        return ItemTypes.JUNGLE_LEAVES.equals(material) || ItemTypes.OAK_LEAVES.equals(material) || ItemTypes.SPRUCE_LEAVES.equals(material) || ItemTypes.DARK_OAK_LEAVES.equals(material) || ItemTypes.ACACIA_LEAVES.equals(material) || ItemTypes.BIRCH_LEAVES.equals(material) || ItemTypes.AZALEA_LEAVES.equals(material) || ItemTypes.OAK_SAPLING.equals(material) || ItemTypes.SPRUCE_SAPLING.equals(material) || ItemTypes.BIRCH_SAPLING.equals(material) || ItemTypes.JUNGLE_SAPLING.equals(material) || ItemTypes.ACACIA_SAPLING.equals(material) || ItemTypes.DARK_OAK_SAPLING.equals(material) || ItemTypes.BEETROOT_SEEDS.equals(material) || ItemTypes.DRIED_KELP.equals(material) || ItemTypes.GRASS.equals(material) || ItemTypes.KELP.equals(material) || ItemTypes.MELON_SEEDS.equals(material) || ItemTypes.PUMPKIN_SEEDS.equals(material) || ItemTypes.SEAGRASS.equals(material) || ItemTypes.SWEET_BERRIES.equals(material) || ItemTypes.GLOW_BERRIES.equals(material) || ItemTypes.WHEAT_SEEDS.equals(material) || ItemTypes.MOSS_CARPET.equals(material) || ItemTypes.SMALL_DRIPLEAF.equals(material) || ItemTypes.HANGING_ROOTS.equals(material) || ItemTypes.DRIED_KELP_BLOCK.equals(material) || ItemTypes.TALL_GRASS.equals(material) || ItemTypes.AZALEA.equals(material) || ItemTypes.CACTUS.equals(material) || ItemTypes.SUGAR_CANE.equals(material) || ItemTypes.VINE.equals(material) || ItemTypes.NETHER_SPROUTS.equals(material) || ItemTypes.WEEPING_VINES.equals(material) || ItemTypes.TWISTING_VINES.equals(material) || ItemTypes.MELON_SLICE.equals(material) || ItemTypes.GLOW_LICHEN.equals(material) || ItemTypes.SEA_PICKLE.equals(material) || ItemTypes.LILY_PAD.equals(material) || ItemTypes.PUMPKIN.equals(material) || ItemTypes.CARVED_PUMPKIN.equals(material) || ItemTypes.MELON.equals(material) || ItemTypes.APPLE.equals(material) || ItemTypes.BEETROOT.equals(material) || ItemTypes.CARROT.equals(material) || ItemTypes.COCOA_BEANS.equals(material) || ItemTypes.POTATO.equals(material) || ItemTypes.WHEAT.equals(material) || ItemTypes.BROWN_MUSHROOM.equals(material) || ItemTypes.RED_MUSHROOM.equals(material) || ItemTypes.MUSHROOM_STEM.equals(material) || ItemTypes.CRIMSON_FUNGUS.equals(material) || ItemTypes.WARPED_FUNGUS.equals(material) || ItemTypes.NETHER_WART.equals(material) || ItemTypes.CRIMSON_ROOTS.equals(material) || ItemTypes.WARPED_ROOTS.equals(material) || ItemTypes.SHROOMLIGHT.equals(material) || ItemTypes.DANDELION.equals(material) || ItemTypes.POPPY.equals(material) || ItemTypes.BLUE_ORCHID.equals(material) || ItemTypes.ALLIUM.equals(material) || ItemTypes.AZURE_BLUET.equals(material) || ItemTypes.RED_TULIP.equals(material) || ItemTypes.ORANGE_TULIP.equals(material) || ItemTypes.WHITE_TULIP.equals(material) || ItemTypes.PINK_TULIP.equals(material) || ItemTypes.OXEYE_DAISY.equals(material) || ItemTypes.CORNFLOWER.equals(material) || ItemTypes.LILY_OF_THE_VALLEY.equals(material) || ItemTypes.WITHER_ROSE.equals(material) || ItemTypes.FERN.equals(material) || ItemTypes.SUNFLOWER.equals(material) || ItemTypes.LILAC.equals(material) || ItemTypes.ROSE_BUSH.equals(material) || ItemTypes.PEONY.equals(material) || ItemTypes.LARGE_FERN.equals(material) || ItemTypes.SPORE_BLOSSOM.equals(material) || ItemTypes.MOSS_BLOCK.equals(material) || ItemTypes.BIG_DRIPLEAF.equals(material) || ItemTypes.HAY_BLOCK.equals(material) || ItemTypes.BROWN_MUSHROOM_BLOCK.equals(material) || ItemTypes.RED_MUSHROOM_BLOCK.equals(material) || ItemTypes.NETHER_WART_BLOCK.equals(material) || ItemTypes.WARPED_WART_BLOCK.equals(material) || ItemTypes.FLOWERING_AZALEA.equals(material) || ItemTypes.BREAD.equals(material) || ItemTypes.BAKED_POTATO.equals(material) || ItemTypes.COOKIE.equals(material) || ItemTypes.CAKE.equals(material) || ItemTypes.PUMPKIN_PIE.equals(material);
    }
}
