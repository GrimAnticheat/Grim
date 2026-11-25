package ac.grim.grimac.utils.nmsutil;

import com.github.retrooper.packetevents.protocol.world.states.type.StateType;

import java.util.HashSet;
import java.util.Set;

/**
 * What a piston does to each block, as vanilla's {@code BlockBehaviour.Properties#pushReaction}, and which blocks have a
 * block entity (and so never move). Both are taken from {@code Blocks} and {@code BlockEntityType(s)} of 1.21.11 and
 * 26.1 to 26.3, merged: no block's reaction changed between them, or since 1.8's mobility flags, only blocks were added.
 * Names a server's version doesn't have are skipped.
 *
 * <p>A block missing from these is pushed normally. That errs towards moving more blocks than the client does, which only
 * widens the area Grim allows a push in.
 */
public final class PistonPushReaction {

    public enum Reaction {
        NORMAL,
        /** Broken by a piston pushing it, and never pulled. */
        DESTROY,
        /** Never moved, and stops a push. */
        BLOCK,
        /** Pushed, never pulled: glazed terracotta. */
        PUSH_ONLY
    }

    private static final Set<StateType> DESTROY = of("""
            acacia_button acacia_door acacia_leaves acacia_pressure_plate acacia_sapling allium amethyst_cluster
            attached_melon_stem attached_pumpkin_stem azalea azalea_leaves azure_bluet bamboo bamboo_button
            bamboo_door bamboo_pressure_plate bamboo_sapling beetroots bell big_dripleaf big_dripleaf_stem
            birch_button birch_door birch_leaves birch_pressure_plate birch_sapling black_bed black_candle
            black_candle_cake black_shulker_box blue_bed blue_candle blue_candle_cake blue_orchid blue_shulker_box
            brain_coral brain_coral_fan brain_coral_wall_fan brown_bed brown_candle brown_candle_cake brown_mushroom
            brown_shulker_box bubble_column bubble_coral bubble_coral_fan bubble_coral_wall_fan budding_amethyst
            bush cactus cactus_flower cake candle candle_cake carrots carved_pumpkin cave_vines cave_vines_plant
            cherry_button cherry_door cherry_leaves cherry_pressure_plate cherry_sapling chorus_flower chorus_plant
            closed_eyeblossom cobweb cocoa comparator copper_door copper_golem_statue copper_lantern copper_torch
            copper_wall_torch cornflower creeper_head creeper_wall_head crimson_button crimson_door crimson_fungus
            crimson_pressure_plate crimson_roots cyan_bed cyan_candle cyan_candle_cake cyan_shulker_box dandelion
            dark_oak_button dark_oak_door dark_oak_leaves dark_oak_pressure_plate dark_oak_sapling dead_bush
            decorated_pot dragon_egg dragon_head dragon_wall_head exposed_copper_door exposed_copper_golem_statue
            exposed_copper_lantern fern fire fire_coral fire_coral_fan fire_coral_wall_fan firefly_bush flower_pot
            flowering_azalea flowering_azalea_leaves frogspawn glow_lichen golden_dandelion gray_bed gray_candle
            gray_candle_cake gray_shulker_box green_bed green_candle green_candle_cake green_shulker_box
            hanging_roots heavy_weighted_pressure_plate horn_coral horn_coral_fan horn_coral_wall_fan iron_door
            jack_o_lantern jungle_button jungle_door jungle_leaves jungle_pressure_plate jungle_sapling kelp
            kelp_plant ladder lantern large_amethyst_bud large_fern lava leaf_litter lever light_blue_bed
            light_blue_candle light_blue_candle_cake light_blue_shulker_box light_gray_bed light_gray_candle
            light_gray_candle_cake light_gray_shulker_box light_weighted_pressure_plate lilac lily_of_the_valley
            lily_pad lime_bed lime_candle lime_candle_cake lime_shulker_box magenta_bed magenta_candle
            magenta_candle_cake magenta_shulker_box mangrove_button mangrove_door mangrove_leaves
            mangrove_pressure_plate mangrove_propagule medium_amethyst_bud melon melon_stem moss_block moss_carpet
            nether_sprouts nether_wart oak_button oak_door oak_leaves oak_pressure_plate oak_sapling open_eyeblossom
            orange_bed orange_candle orange_candle_cake orange_poplar_leaves orange_shulker_box orange_tulip
            oxeye_daisy oxidized_copper_door oxidized_copper_golem_statue oxidized_copper_lantern pale_hanging_moss
            pale_moss_block pale_moss_carpet pale_oak_button pale_oak_door pale_oak_leaves pale_oak_pressure_plate
            pale_oak_sapling peony piglin_head piglin_wall_head pink_bed pink_candle pink_candle_cake pink_petals
            pink_shulker_box pink_tulip pitcher_crop pitcher_plant player_head player_wall_head pointed_dripstone
            polished_blackstone_button polished_blackstone_pressure_plate poplar_button poplar_door
            poplar_pressure_plate poplar_sapling poppy potatoes potted_acacia_sapling potted_allium
            potted_azalea_bush potted_azure_bluet potted_bamboo potted_birch_sapling potted_blue_orchid
            potted_brown_mushroom potted_cactus potted_cherry_sapling potted_closed_eyeblossom potted_cornflower
            potted_crimson_fungus potted_crimson_roots potted_dandelion potted_dark_oak_sapling potted_dead_bush
            potted_fern potted_flowering_azalea_bush potted_golden_dandelion potted_jungle_sapling
            potted_lily_of_the_valley potted_mangrove_propagule potted_oak_sapling potted_open_eyeblossom
            potted_orange_tulip potted_oxeye_daisy potted_pale_oak_sapling potted_pink_tulip potted_poplar_sapling
            potted_poppy potted_red_mushroom potted_red_tulip potted_spruce_sapling potted_torchflower
            potted_warped_fungus potted_warped_roots potted_white_tulip potted_wither_rose pumpkin pumpkin_stem
            purple_bed purple_candle purple_candle_cake purple_shulker_box red_bed red_candle red_candle_cake
            red_mushroom red_poplar_leaves red_shrub red_shulker_box red_tulip redstone_torch redstone_wall_torch
            redstone_wire repeater resin_clump rose_bush scaffolding sculk_vein sea_pickle seagrass shelf_mushroom
            short_dry_grass short_grass shulker_box skeleton_skull skeleton_wall_skull small_amethyst_bud
            small_dripleaf snow soul_fire soul_lantern soul_torch soul_wall_torch spore_blossom spruce_button
            spruce_door spruce_leaves spruce_pressure_plate spruce_sapling stone_button stone_pressure_plate
            straw_bed structure_void sugar_cane sulfur_spike sunflower suspicious_gravel suspicious_sand
            sweet_berry_bush tall_dry_grass tall_grass tall_seagrass torch torchflower torchflower_crop tripwire
            tripwire_hook tube_coral tube_coral_fan tube_coral_wall_fan turtle_egg twisting_vines
            twisting_vines_plant vine wall_torch warped_button warped_door warped_fungus warped_pressure_plate
            warped_roots water waxed_copper_door waxed_copper_golem_statue waxed_copper_lantern
            waxed_exposed_copper_door waxed_exposed_copper_golem_statue waxed_exposed_copper_lantern
            waxed_oxidized_copper_door waxed_oxidized_copper_golem_statue waxed_oxidized_copper_lantern
            waxed_weathered_copper_door waxed_weathered_copper_golem_statue waxed_weathered_copper_lantern
            weathered_copper_door weathered_copper_golem_statue weathered_copper_lantern weeping_vines
            weeping_vines_plant wheat white_bed white_candle white_candle_cake white_shulker_box white_tulip
            wildflowers wither_rose wither_skeleton_skull wither_skeleton_wall_skull yellow_bed yellow_candle
            yellow_candle_cake yellow_poplar_leaves yellow_shulker_box zombie_head zombie_wall_head
            """);

    private static final Set<StateType> BLOCK = of("""
            anvil barrier chipped_anvil damaged_anvil end_gateway end_portal grindstone lodestone moving_piston
            nether_portal piston piston_head sticky_piston
            """);

    private static final Set<StateType> PUSH_ONLY = of("""
            black_glazed_terracotta blue_glazed_terracotta brown_glazed_terracotta cyan_glazed_terracotta
            gray_glazed_terracotta green_glazed_terracotta light_blue_glazed_terracotta light_gray_glazed_terracotta
            lime_glazed_terracotta magenta_glazed_terracotta orange_glazed_terracotta pink_glazed_terracotta
            purple_glazed_terracotta red_glazed_terracotta white_glazed_terracotta yellow_glazed_terracotta
            """);

    // PistonBaseBlock#isPushable refuses these by name, whatever their reaction; 26.3 made it their reaction instead
    private static final Set<StateType> IMMOVABLE = of("""
            obsidian crying_obsidian respawn_anchor reinforced_deepslate
            """);

    // Only those that would otherwise be pushed: a block entity is never moved, whatever its reaction
    private static final Set<StateType> BLOCK_ENTITIES = of("""
            acacia_hanging_sign acacia_shelf acacia_sign acacia_wall_hanging_sign acacia_wall_sign
            bamboo_hanging_sign bamboo_shelf bamboo_sign bamboo_wall_hanging_sign bamboo_wall_sign barrel beacon
            bee_nest beehive birch_hanging_sign birch_shelf birch_sign birch_wall_hanging_sign birch_wall_sign
            black_banner black_wall_banner blast_furnace blue_banner blue_wall_banner brewing_stand brown_banner
            brown_wall_banner calibrated_sculk_sensor campfire chain_command_block cherry_hanging_sign cherry_shelf
            cherry_sign cherry_wall_hanging_sign cherry_wall_sign chest chiseled_bookshelf command_block conduit
            copper_chest crafter creaking_heart crimson_hanging_sign crimson_shelf crimson_sign
            crimson_wall_hanging_sign crimson_wall_sign cyan_banner cyan_wall_banner dark_oak_hanging_sign
            dark_oak_shelf dark_oak_sign dark_oak_wall_hanging_sign dark_oak_wall_sign daylight_detector dispenser
            dropper enchanting_table ender_chest exposed_copper_chest furnace gray_banner gray_wall_banner
            green_banner green_wall_banner hopper jigsaw jukebox jungle_hanging_sign jungle_shelf jungle_sign
            jungle_wall_hanging_sign jungle_wall_sign lectern light_blue_banner light_blue_wall_banner
            light_gray_banner light_gray_wall_banner lime_banner lime_wall_banner magenta_banner magenta_wall_banner
            mangrove_hanging_sign mangrove_shelf mangrove_sign mangrove_wall_hanging_sign mangrove_wall_sign
            oak_hanging_sign oak_shelf oak_sign oak_wall_hanging_sign oak_wall_sign orange_banner orange_wall_banner
            oxidized_copper_chest pale_oak_hanging_sign pale_oak_shelf pale_oak_sign pale_oak_wall_hanging_sign
            pale_oak_wall_sign pink_banner pink_wall_banner poplar_hanging_sign poplar_shelf poplar_sign
            poplar_wall_hanging_sign poplar_wall_sign potent_sulfur purple_banner purple_wall_banner red_banner
            red_wall_banner repeating_command_block sculk_catalyst sculk_sensor sculk_shrieker smoker soul_campfire
            spawner spruce_hanging_sign spruce_shelf spruce_sign spruce_wall_hanging_sign spruce_wall_sign
            structure_block test_block test_instance_block trapped_chest trial_spawner vault warped_hanging_sign
            warped_shelf warped_sign warped_wall_hanging_sign warped_wall_sign waxed_copper_chest
            waxed_exposed_copper_chest waxed_oxidized_copper_chest waxed_weathered_copper_chest
            weathered_copper_chest white_banner white_wall_banner yellow_banner yellow_wall_banner
            """);

    private PistonPushReaction() {
    }

    public static Reaction of(StateType type) {
        if (DESTROY.contains(type)) return Reaction.DESTROY;
        if (BLOCK.contains(type)) return Reaction.BLOCK;
        if (PUSH_ONLY.contains(type)) return Reaction.PUSH_ONLY;
        return Reaction.NORMAL;
    }

    public static boolean isImmovable(StateType type) {
        return IMMOVABLE.contains(type);
    }

    public static boolean hasBlockEntity(StateType type) {
        return BLOCK_ENTITIES.contains(type);
    }

    private static Set<StateType> of(String names) {
        Set<StateType> types = new HashSet<>();
        for (String name : names.trim().split("\s+")) {
            StateType type = com.github.retrooper.packetevents.protocol.world.states.type.StateTypes.getByName(name);
            if (type != null) types.add(type);
        }
        return types;
    }
}
