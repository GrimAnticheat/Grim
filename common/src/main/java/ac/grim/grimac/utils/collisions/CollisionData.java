package ac.grim.grimac.utils.collisions;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.predictionengine.movementtick.MovementTickerStrider;
import ac.grim.grimac.utils.collisions.blocks.DoorHandler;
import ac.grim.grimac.utils.collisions.blocks.DynamicChest;
import ac.grim.grimac.utils.collisions.blocks.DynamicChorusPlant;
import ac.grim.grimac.utils.collisions.blocks.DynamicStair;
import ac.grim.grimac.utils.collisions.blocks.PistonBaseCollision;
import ac.grim.grimac.utils.collisions.blocks.PistonHeadCollision;
import ac.grim.grimac.utils.collisions.blocks.TrapDoorHandler;
import ac.grim.grimac.utils.collisions.blocks.connecting.DynamicCollisionFence;
import ac.grim.grimac.utils.collisions.blocks.connecting.DynamicCollisionPane;
import ac.grim.grimac.utils.collisions.blocks.connecting.DynamicCollisionWall;
import ac.grim.grimac.utils.collisions.datatypes.CollisionBox;
import ac.grim.grimac.utils.collisions.datatypes.CollisionFactory;
import ac.grim.grimac.utils.collisions.datatypes.ComplexCollisionBox;
import ac.grim.grimac.utils.collisions.datatypes.DynamicCollisionBox;
import ac.grim.grimac.utils.collisions.datatypes.HexCollisionBox;
import ac.grim.grimac.utils.collisions.datatypes.HexOffsetCollisionBox;
import ac.grim.grimac.utils.collisions.datatypes.NoCollisionBox;
import ac.grim.grimac.utils.collisions.datatypes.SimpleCollisionBox;
import ac.grim.grimac.utils.data.packetentity.PacketEntityStrider;
import ac.grim.grimac.utils.nmsutil.Materials;
import ac.grim.grimac.utils.viaversion.ViaVersionUtil;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.world.BlockFace;
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import com.github.retrooper.packetevents.protocol.world.states.defaulttags.BlockTags;
import com.github.retrooper.packetevents.protocol.world.states.enums.Attachment;
import com.github.retrooper.packetevents.protocol.world.states.enums.Axis;
import com.github.retrooper.packetevents.protocol.world.states.enums.Face;
import com.github.retrooper.packetevents.protocol.world.states.enums.Half;
import com.github.retrooper.packetevents.protocol.world.states.enums.Part;
import com.github.retrooper.packetevents.protocol.world.states.enums.Thickness;
import com.github.retrooper.packetevents.protocol.world.states.enums.Tilt;
import com.github.retrooper.packetevents.protocol.world.states.enums.Type;
import com.github.retrooper.packetevents.protocol.world.states.enums.VerticalDirection;
import com.github.retrooper.packetevents.protocol.world.states.type.StateType;
import com.github.retrooper.packetevents.protocol.world.states.type.StateTypes;
import com.viaversion.viaversion.api.Via;

import java.util.Arrays;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

// Warning for major game updates!
// Do not use an enum for stuff like Axis and other data types not in 1.7
// Meaning only stuff like getDirection() should have enums
//
// An enum will break support for all previous versions which is very bad
// An if statement for new data types is perfectly safe and should be used instead
//
// This is actually meant to be put into PacketEvents, but I don't like proprietary plugins stealing my code...
public enum CollisionData implements CollisionFactory {
    LAVA((player, version, block, x, y, z) -> {
        if (MovementTickerStrider.isAbove(player) && player.compensatedEntities.self.getRiding() instanceof PacketEntityStrider) {
            if (block.getLevel() == 0) {
                return new HexCollisionBox(0.0D, 0.0D, 0.0D, 16.0D, 8.0D, 16.0D);
            }
        }

        return NoCollisionBox.INSTANCE;
    }, StateTypes.LAVA),

    BREWING_STAND((player, version, block, x, y, z) -> {
        int base = 0;
        // maxIndex is 3 instead of 2 for legacy clients because for 1.8 players there is a very rare bug
        // That we handle later in the code requiring us to add a box https://bugs.mojang.com/browse/MC-85109 For 1.8 PLAYERS
        // 1.8 Brewing Stand hitbox is a fullblock until it is hit sometimes, can be caused be restarting client and joining server
        int maxIndex = 3;

        // Yes I know we only need maxIndex = 3 for 1.8 specifically
        // No I'm not adding a special clause for which would require another if check, I'll take compute > memory any day
        if (version.isNewerThanOrEquals(ClientVersion.V_1_13)) {
            maxIndex = 2;
            base = 1;
        }

        return new ComplexCollisionBox(maxIndex,
                new HexCollisionBox(base, 0, base, 16 - base, 2, 16 - base),
                new SimpleCollisionBox(0.4375, 0.0, 0.4375, 0.5625, 0.875, 0.5625, false));

    }, StateTypes.BREWING_STAND),

    BAMBOO((player, version, block, x, y, z) -> {
        // ViaVersion replacement, sugarcane
        if (version.isOlderThan(ClientVersion.V_1_14)) {
            return NoCollisionBox.INSTANCE;
        }
        return new HexOffsetCollisionBox(block.getType(), 6.5D, 0.0D, 6.5D, 9.5D, 16.0D, 9.5D);
    }, StateTypes.BAMBOO),

    COMPOSTER((player, version, block, x, y, z) -> {
        double height = 0.125;

        if (version.isOlderThanOrEquals(ClientVersion.V_1_13_2))
            height = 0.25;

        if (version.isOlderThanOrEquals(ClientVersion.V_1_12_2))
            height = 0.3125;

        return new ComplexCollisionBox(5,
                new SimpleCollisionBox(0, 0, 0, 1, height, 1, false),
                new SimpleCollisionBox(0, height, 0, 0.125, 1, 1, false),
                new SimpleCollisionBox(1 - 0.125, height, 0, 1, 1, 1, false),
                new SimpleCollisionBox(0, height, 0, 1, 1, 0.125, false),
                new SimpleCollisionBox(0, height, 1 - 0.125, 1, 1, 1, false));
    }, StateTypes.COMPOSTER),

    RAIL(new SimpleCollisionBox(0, 0, 0, 1, 0.125, 0, false),
            StateTypes.RAIL, StateTypes.ACTIVATOR_RAIL,
            StateTypes.DETECTOR_RAIL, StateTypes.POWERED_RAIL),

    ANVIL((player, version, data, x, y, z) -> {
        BlockFace face = data.getFacing();
        // Anvil collision box was changed in 1.13 to be more accurate
        // https://www.mcpk.wiki/wiki/Version_Differences
        // The base is 0.75×0.75, and its floor is 0.25b high.
        // The top is 1×0.625, and its ceiling is 0.375b low.
        if (version.isNewerThanOrEquals(ClientVersion.V_1_13)) {
            ComplexCollisionBox complexAnvil = new ComplexCollisionBox(4);
            // Base of the anvil
            complexAnvil.add(new HexCollisionBox(2, 0, 2, 14, 4, 14));
            if (face == BlockFace.NORTH || face == BlockFace.SOUTH) {
                complexAnvil.add(new HexCollisionBox(4.0D, 4.0D, 3.0D, 12.0D, 5.0D, 13.0D));
                complexAnvil.add(new HexCollisionBox(6.0D, 5.0D, 4.0D, 10.0D, 10.0D, 12.0D));
                complexAnvil.add(new HexCollisionBox(3.0D, 10.0D, 0.0D, 13.0D, 16.0D, 16.0D));
            } else {
                complexAnvil.add(new HexCollisionBox(3.0D, 4.0D, 4.0D, 13.0D, 5.0D, 12.0D));
                complexAnvil.add(new HexCollisionBox(4.0D, 5.0D, 6.0D, 12.0D, 10.0D, 10.0D));
                complexAnvil.add(new HexCollisionBox(0.0D, 10.0D, 3.0D, 16.0D, 16.0D, 13.0D));
            }

            return complexAnvil;
        } else {
            // Just a single solid collision box with 1.12
            if (face == BlockFace.NORTH || face == BlockFace.SOUTH) {
                return new SimpleCollisionBox(0.125F, 0.0F, 0.0F, 0.875F, 1.0F, 1.0F, false);
            } else {
                return new SimpleCollisionBox(0.0F, 0.0F, 0.125F, 1.0F, 1.0F, 0.875F, false);
            }
        }
    }, BlockTags.ANVIL.getStates().toArray(new StateType[0])),

    WALL(new DynamicCollisionWall(), BlockTags.WALLS.getStates().toArray(new StateType[0])),

    SLAB((player, version, data, x, y, z) -> {
        Type slabType = data.getTypeData();
        if (slabType == Type.DOUBLE) {
            return new SimpleCollisionBox(0, 0, 0, 1, 1, 1, true);
        } else if (slabType == Type.BOTTOM) {
            return new SimpleCollisionBox(0, 0, 0, 1, 0.5, 1, false);
        }

        return new SimpleCollisionBox(0, 0.5, 0, 1, 1, 1, false);
        // 1.13 can handle double slabs as it's in the block data
        // 1.12 has double slabs as a separate block, no block data to differentiate it
    }, BlockTags.SLABS.getStates().toArray(new StateType[0])),

    SKULL(new SimpleCollisionBox(0.25F, 0.0F, 0.25F, 0.75F, 0.5F, 0.75F, false),
            StateTypes.CREEPER_HEAD, StateTypes.ZOMBIE_HEAD, StateTypes.DRAGON_HEAD, StateTypes.PLAYER_HEAD,
            StateTypes.SKELETON_SKULL, StateTypes.WITHER_SKELETON_SKULL, StateTypes.HEAVY_CORE),

    PIGLIN_HEAD(new HexCollisionBox(3.0D, 0.0D, 3.0D, 13.0D, 8.0D, 13.0D), StateTypes.PIGLIN_HEAD),

    // Overwrite previous SKULL enum for legacy, where head and wall skull isn't separate
    WALL_SKULL((player, version, data, x, y, z) -> switch (data.getFacing()) {
        case SOUTH -> new SimpleCollisionBox(0.25F, 0.25F, 0.0F, 0.75F, 0.75F, 0.5F, false);
        case WEST -> new SimpleCollisionBox(0.5F, 0.25F, 0.25F, 1.0F, 0.75F, 0.75F, false);
        case EAST -> new SimpleCollisionBox(0.0F, 0.25F, 0.25F, 0.5F, 0.75F, 0.75F, false);
        default -> new SimpleCollisionBox(0.25F, 0.25F, 0.5F, 0.75F, 0.75F, 1.0F, false);
    }, StateTypes.CREEPER_WALL_HEAD, StateTypes.DRAGON_WALL_HEAD, StateTypes.PLAYER_WALL_HEAD, StateTypes.ZOMBIE_WALL_HEAD,
            StateTypes.SKELETON_WALL_SKULL, StateTypes.WITHER_SKELETON_WALL_SKULL),

    PIGLIN_WALL_HEAD((player, version, data, x, y, z) -> switch (data.getFacing()) {
        case SOUTH -> new HexCollisionBox(3.0D, 4.0D, 0.0D, 13.0D, 12.0D, 8.0D);
        case EAST -> new HexCollisionBox(0.0D, 4.0D, 3.0D, 8.0D, 12.0D, 13.0D);
        case WEST -> new HexCollisionBox(8.0D, 4.0D, 3.0D, 16.0D, 12.0D, 13.0D);
        default -> new HexCollisionBox(3.0D, 4.0D, 8.0D, 13.0D, 12.0D, 16.0D);
    }, StateTypes.PIGLIN_WALL_HEAD),

    DOOR(new DoorHandler(), BlockTags.DOORS.getStates().toArray(new StateType[0])),

    HOPPER((player, version, data, x, y, z) -> {
        if (version.isNewerThanOrEquals(ClientVersion.V_1_13)) {
            ComplexCollisionBox hopperBox = new ComplexCollisionBox(7);

            switch (data.getFacing()) {
                case DOWN:
                    hopperBox.add(new HexCollisionBox(6.0D, 0.0D, 6.0D, 10.0D, 4.0D, 10.0D));
                    break;
                case EAST:
                    hopperBox.add(new HexCollisionBox(12.0D, 4.0D, 6.0D, 16.0D, 8.0D, 10.0D));
                    break;
                case NORTH:
                    hopperBox.add(new HexCollisionBox(6.0D, 4.0D, 0.0D, 10.0D, 8.0D, 4.0D));
                    break;
                case SOUTH:
                    hopperBox.add(new HexCollisionBox(6.0D, 4.0D, 12.0D, 10.0D, 8.0D, 16.0D));
                    break;
                case WEST:
                    hopperBox.add(new HexCollisionBox(0.0D, 4.0D, 6.0D, 4.0D, 8.0D, 10.0D));
                    break;
            }

            hopperBox.add(new SimpleCollisionBox(0, 0.625, 0, 1.0, 0.6875, 1.0, false));
            hopperBox.add(new SimpleCollisionBox(0, 0.6875, 0, 0.125, 1, 1, false));
            hopperBox.add(new SimpleCollisionBox(0.125, 0.6875, 0, 1, 1, 0.125, false));
            hopperBox.add(new SimpleCollisionBox(0.125, 0.6875, 0.875, 1, 1, 1, false));
            hopperBox.add(new SimpleCollisionBox(0.25, 0.25, 0.25, 0.75, 0.625, 0.75, false));
            hopperBox.add(new SimpleCollisionBox(0.875, 0.6875, 0.125, 1, 1, 0.875, false));

            return hopperBox;
        } else {
            double height = 0.125 * 5;

            return new ComplexCollisionBox(5,
                    new SimpleCollisionBox(0, 0, 0, 1, height, 1, false),
                    new SimpleCollisionBox(0, height, 0, 0.125, 1, 1, false),
                    new SimpleCollisionBox(1 - 0.125, height, 0, 1, 1, 1, false),
                    new SimpleCollisionBox(0, height, 0, 1, 1, 0.125, false),
                    new SimpleCollisionBox(0, height, 1 - 0.125, 1, 1, 1, false));
        }

    }, StateTypes.HOPPER),

    CAKE((player, version, data, x, y, z) -> {
        double height = 0.5;
        if (version.isOlderThan(ClientVersion.V_1_8))
            height = 0.4375;
        double eatenPosition = (1 + (data.getBites()) * 2) / 16D;
        return new SimpleCollisionBox(eatenPosition, 0, 0.0625, 1 - 0.0625, height, 1 - 0.0625, false);
    }, StateTypes.CAKE),

    COCOA_BEANS((player, version, data, x, y, z) -> getCocoa(version, data.getAge(), data.getFacing()), StateTypes.COCOA),

    STONE_CUTTER((player, version, data, x, y, z) -> {
        if (version.isOlderThanOrEquals(ClientVersion.V_1_13_2))
            return new SimpleCollisionBox(0, 0, 0, 1, 1, 1, true);

        return new HexCollisionBox(0.0D, 0.0D, 0.0D, 16.0D, 9.0D, 16.0D);
    }, StateTypes.STONECUTTER),

    CORAL_FAN(NoCollisionBox.INSTANCE, BlockTags.CORALS.getStates().toArray(new StateType[0])),

    RAILS(NoCollisionBox.INSTANCE, BlockTags.RAILS.getStates().toArray(new StateType[0])),

    BANNER(NoCollisionBox.INSTANCE, BlockTags.BANNERS.getStates().toArray(new StateType[0])),

    SMALL_FLOWER(NoCollisionBox.INSTANCE, BlockTags.SMALL_FLOWERS.getStates().toArray(new StateType[0])),

    TALL_FLOWER(NoCollisionBox.INSTANCE, BlockTags.TALL_FLOWERS.getStates().toArray(new StateType[0])),

    SAPLING(NoCollisionBox.INSTANCE, BlockTags.SAPLINGS.getStates().toArray(new StateType[0])),

    BUTTON(NoCollisionBox.INSTANCE, BlockTags.BUTTONS.getStates().toArray(new StateType[0])),

    // All states that unconditionally have no collision and are not in a group where every member also has no collision.
    NO_COLLISION(NoCollisionBox.INSTANCE, StateTypes.TWISTING_VINES_PLANT, StateTypes.WEEPING_VINES_PLANT,
            StateTypes.TWISTING_VINES, StateTypes.WEEPING_VINES, StateTypes.CAVE_VINES, StateTypes.CAVE_VINES_PLANT,
            StateTypes.TALL_SEAGRASS, StateTypes.SEAGRASS, StateTypes.SHORT_GRASS, StateTypes.FERN, StateTypes.NETHER_SPROUTS,
            StateTypes.DEAD_BUSH, StateTypes.SUGAR_CANE, StateTypes.SWEET_BERRY_BUSH, StateTypes.WARPED_ROOTS,
            StateTypes.CRIMSON_ROOTS, StateTypes.TORCHFLOWER_CROP, StateTypes.PINK_PETALS, StateTypes.TALL_GRASS,
            StateTypes.LARGE_FERN, StateTypes.BAMBOO_SAPLING, StateTypes.HANGING_ROOTS, StateTypes.VINE,
            StateTypes.SMALL_DRIPLEAF, StateTypes.END_PORTAL, StateTypes.LEVER, StateTypes.PUMPKIN_STEM, StateTypes.MELON_STEM,
            StateTypes.ATTACHED_MELON_STEM, StateTypes.ATTACHED_PUMPKIN_STEM, StateTypes.BEETROOTS, StateTypes.POTATOES,
            StateTypes.WHEAT, StateTypes.CARROTS, StateTypes.NETHER_WART, StateTypes.MOVING_PISTON, StateTypes.AIR, StateTypes.CAVE_AIR,
            StateTypes.VOID_AIR, StateTypes.LIGHT, StateTypes.WATER, StateTypes.BUBBLE_COLUMN, StateTypes.FIRE, StateTypes.SOUL_FIRE),

    KELP(new HexCollisionBox(0.0D, 0.0D, 0.0D, 16.0D, 9.0D, 16.0D), StateTypes.KELP),
    // Kelp block is a full block, so it by default is correct

    BELL((player, version, data, x, y, z) -> {
        if (version.isOlderThanOrEquals(ClientVersion.V_1_13_2))
            return new SimpleCollisionBox(0, 0, 0, 1, 1, 1, true);

        BlockFace direction = data.getFacing();

        if (data.getAttachment() == Attachment.FLOOR) {
            return direction != BlockFace.NORTH && direction != BlockFace.SOUTH ?
                    new HexCollisionBox(4.0D, 0.0D, 0.0D, 12.0D, 16.0D, 16.0D) :
                    new HexCollisionBox(0.0D, 0.0D, 4.0D, 16.0D, 16.0D, 12.0D);

        }

        ComplexCollisionBox complex = new ComplexCollisionBox(3,
                new HexCollisionBox(5.0D, 6.0D, 5.0D, 11.0D, 13.0D, 11.0D),
                new HexCollisionBox(4.0D, 4.0D, 4.0D, 12.0D, 6.0D, 12.0D));

        if (data.getAttachment() == Attachment.CEILING) {
            complex.add(new HexCollisionBox(7.0D, 13.0D, 7.0D, 9.0D, 16.0D, 9.0D));
        } else if (data.getAttachment() == Attachment.DOUBLE_WALL) {
            if (direction != BlockFace.NORTH && direction != BlockFace.SOUTH) {
                complex.add(new HexCollisionBox(0.0D, 13.0D, 7.0D, 16.0D, 15.0D, 9.0D));
            } else {
                complex.add(new HexCollisionBox(7.0D, 13.0D, 0.0D, 9.0D, 15.0D, 16.0D));
            }
        } else if (direction == BlockFace.NORTH) {
            complex.add(new HexCollisionBox(7.0D, 13.0D, 0.0D, 9.0D, 15.0D, 13.0D));
        } else if (direction == BlockFace.SOUTH) {
            complex.add(new HexCollisionBox(7.0D, 13.0D, 3.0D, 9.0D, 15.0D, 16.0D));
        } else {
            if (direction == BlockFace.EAST) {
                complex.add(new HexCollisionBox(3.0D, 13.0D, 7.0D, 16.0D, 15.0D, 9.0D));
            } else {
                complex.add(new HexCollisionBox(0.0D, 13.0D, 7.0D, 13.0D, 15.0D, 9.0D));
            }
        }

        return complex;

    }, StateTypes.BELL),

    SCAFFOLDING((player, version, data, x, y, z) -> {
        // ViaVersion replacement block - hay block
        if (version.isOlderThanOrEquals(ClientVersion.V_1_13_2))
            return new SimpleCollisionBox(0, 0, 0, 1, 1, 1, true);

        if (player.lastY > y + 1 - 1e-5 && !player.isSneaking) {
            return new ComplexCollisionBox(5,
                    new HexCollisionBox(0.0D, 14.0D, 0.0D, 16.0D, 16.0D, 16.0D),
                    new HexCollisionBox(0.0D, 0.0D, 0.0D, 2.0D, 16.0D, 2.0D),
                    new HexCollisionBox(14.0D, 0.0D, 0.0D, 16.0D, 16.0D, 2.0D),
                    new HexCollisionBox(0.0D, 0.0D, 14.0D, 2.0D, 16.0D, 16.0),
                    new HexCollisionBox(14.0D, 0.0D, 14.0D, 16.0D, 16.0D, 16.0D));
        }

        return data.getDistance() != 0 && data.isBottom() && player.lastY > y - 1e-5 ?
                new HexCollisionBox(0.0D, 0.0D, 0.0D, 16.0D, 2.0D, 16.0D) :
                NoCollisionBox.INSTANCE;
    }, StateTypes.SCAFFOLDING),

    LADDER((player, version, data, x, y, z) -> {
        int width = 3;
        if (version.isOlderThanOrEquals(ClientVersion.V_1_8))
            width = 2;

        return switch (data.getFacing()) {
            case NORTH -> new HexCollisionBox(0.0D, 0.0D, 16.0D - width, 16.0D, 16.0D, 16.0D);
            case SOUTH -> new HexCollisionBox(0.0D, 0.0D, 0.0D, 16.0D, 16.0D, width);
            case WEST -> new HexCollisionBox(16.0D - width, 0.0D, 0.0D, 16.0D, 16.0D, 16.0D);
            default -> new HexCollisionBox(0.0D, 0.0D, 0.0D, width, 16.0D, 16.0D);
        };
    }, StateTypes.LADDER),

    CAMPFIRE((player, version, data, x, y, z) -> {
        // ViaVersion replacement block - slab if not lit or fire if lit
        if (version.isOlderThanOrEquals(ClientVersion.V_1_13_2)) {

            if (data.isLit()) {
                return NoCollisionBox.INSTANCE;
            }

            return new HexCollisionBox(0, 0, 0, 16, 8, 16);
        }

        return new HexCollisionBox(0.0D, 0.0D, 0.0D, 16.0D, 7.0D, 16.0D);
    }, StateTypes.CAMPFIRE, StateTypes.SOUL_CAMPFIRE),

    LANTERN((player, version, data, x, y, z) -> {
        if (version.isOlderThanOrEquals(ClientVersion.V_1_12_2))
            return new SimpleCollisionBox(0, 0, 0, 1, 1, 1, true);

        if (data.isHanging()) {
            return new ComplexCollisionBox(2,
                    new HexCollisionBox(5.0D, 1.0D, 5.0D, 11.0D, 8.0D, 11.0D),
                    new HexCollisionBox(6.0D, 8.0D, 6.0D, 10.0D, 10.0D, 10.0D));
        }

        return new ComplexCollisionBox(2,
                new HexCollisionBox(5.0D, 0.0D, 5.0D, 11.0D, 7.0D, 11.0D),
                new HexCollisionBox(6.0D, 7.0D, 6.0D, 10.0D, 9.0D, 10.0D));

    }, BlockTags.LANTERNS.getStates().toArray(new StateType[0])),


    LECTERN((player, version, data, x, y, z) -> {
        if (version.isOlderThanOrEquals(ClientVersion.V_1_13_2))
            return new SimpleCollisionBox(0, 0, 0, 1, 1, 1, true);

        return new ComplexCollisionBox(2,
                new HexCollisionBox(0.0D, 0.0D, 0.0D, 16.0D, 2.0D, 16.0D), // base
                new HexCollisionBox(4.0D, 2.0D, 4.0D, 12.0D, 14.0D, 12.0D)); // post
    }, StateTypes.LECTERN),


    HONEY_BLOCK((player, version, data, x, y, z) -> {
        if (version.isOlderThanOrEquals(ClientVersion.V_1_14_4))
            return new SimpleCollisionBox(0, 0, 0, 1, 1, 1, true);

        return new HexCollisionBox(1.0D, 0.0D, 1.0D, 15.0D, 15.0D, 15.0D); // post
    }, StateTypes.HONEY_BLOCK),

    DRAGON_EGG_BLOCK(new HexCollisionBox(1.0D, 0.0D, 1.0D, 15.0D, 16.0D, 15.0D), StateTypes.DRAGON_EGG),

    GRINDSTONE((player, version, data, x, y, z) -> {
        BlockFace facing = data.getFacing();

        // ViaVersion replacement block - Anvil
        if (version.isOlderThanOrEquals(ClientVersion.V_1_12_2)) {
            // Just a single solid collision box with 1.12
            if (facing == BlockFace.NORTH || facing == BlockFace.SOUTH) {
                return new SimpleCollisionBox(0.125F, 0.0F, 0.0F, 0.875F, 1.0F, 1.0F, false);
            } else {
                return new SimpleCollisionBox(0.0F, 0.0F, 0.125F, 1.0F, 1.0F, 0.875F, false);
            }
        }

        if (version.isOlderThanOrEquals(ClientVersion.V_1_13_2)) {
            ComplexCollisionBox complexAnvil = new ComplexCollisionBox(4);
            // Base of the anvil
            complexAnvil.add(new HexCollisionBox(2, 0, 2, 14, 4, 14));

            if (facing == BlockFace.NORTH || facing == BlockFace.SOUTH) {
                complexAnvil.add(new HexCollisionBox(4.0D, 4.0D, 3.0D, 12.0D, 5.0D, 13.0D));
                complexAnvil.add(new HexCollisionBox(6.0D, 5.0D, 4.0D, 10.0D, 10.0D, 12.0D));
                complexAnvil.add(new HexCollisionBox(3.0D, 10.0D, 0.0D, 13.0D, 16.0D, 16.0D));
            } else {
                complexAnvil.add(new HexCollisionBox(3.0D, 4.0D, 4.0D, 13.0D, 5.0D, 12.0D));
                complexAnvil.add(new HexCollisionBox(4.0D, 5.0D, 6.0D, 12.0D, 10.0D, 10.0D));
                complexAnvil.add(new HexCollisionBox(0.0D, 10.0D, 3.0D, 16.0D, 16.0D, 13.0D));
            }

            return complexAnvil;
        }

        Face attachment = data.getFace();
        if (attachment == Face.FLOOR) {
            if (facing == BlockFace.NORTH || facing == BlockFace.SOUTH) {
                return new ComplexCollisionBox(5,
                        new HexCollisionBox(2.0D, 0.0D, 6.0D, 4.0D, 7.0D, 10.0D),
                        new HexCollisionBox(12.0D, 0.0D, 6.0D, 14.0D, 7.0D, 10.0D),
                        new HexCollisionBox(2.0D, 7.0D, 5.0D, 4.0D, 13.0D, 11.0D),
                        new HexCollisionBox(12.0D, 7.0D, 5.0D, 14.0D, 13.0D, 11.0D),
                        new HexCollisionBox(4.0D, 4.0D, 2.0D, 12.0D, 16.0D, 14.0D));
            } else {
                return new ComplexCollisionBox(5,
                        new HexCollisionBox(6.0D, 0.0D, 2.0D, 10.0D, 7.0D, 4.0D),
                        new HexCollisionBox(6.0D, 0.0D, 12.0D, 10.0D, 7.0D, 14.0D),
                        new HexCollisionBox(5.0D, 7.0D, 2.0D, 11.0D, 13.0D, 4.0D),
                        new HexCollisionBox(5.0D, 7.0D, 12.0D, 11.0D, 13.0D, 14.0D),
                        new HexCollisionBox(2.0D, 4.0D, 4.0D, 14.0D, 16.0D, 12.0D));
            }
        } else if (attachment == Face.WALL) {
            switch (facing) {
                case NORTH:
                    return new ComplexCollisionBox(5,
                            new HexCollisionBox(2.0D, 6.0D, 7.0D, 4.0D, 10.0D, 16.0D),
                            new HexCollisionBox(12.0D, 6.0D, 7.0D, 14.0D, 10.0D, 16.0D),
                            new HexCollisionBox(2.0D, 5.0D, 3.0D, 4.0D, 11.0D, 9.0D),
                            new HexCollisionBox(12.0D, 5.0D, 3.0D, 14.0D, 11.0D, 9.0D),
                            new HexCollisionBox(4.0D, 2.0D, 0.0D, 12.0D, 14.0D, 12.0D));
                case WEST:
                    return new ComplexCollisionBox(5,
                            new HexCollisionBox(7.0D, 6.0D, 2.0D, 16.0D, 10.0D, 4.0D),
                            new HexCollisionBox(7.0D, 6.0D, 12.0D, 16.0D, 10.0D, 14.0D),
                            new HexCollisionBox(3.0D, 5.0D, 2.0D, 9.0D, 11.0D, 4.0D),
                            new HexCollisionBox(3.0D, 5.0D, 12.0D, 9.0D, 11.0D, 14.0D),
                            new HexCollisionBox(0.0D, 2.0D, 4.0D, 12.0D, 14.0D, 12.0D));
                case SOUTH:
                    return new ComplexCollisionBox(5,
                            new HexCollisionBox(2.0D, 6.0D, 0.0D, 4.0D, 10.0D, 7.0D),
                            new HexCollisionBox(12.0D, 6.0D, 0.0D, 14.0D, 10.0D, 7.0D),
                            new HexCollisionBox(2.0D, 5.0D, 7.0D, 4.0D, 11.0D, 13.0D),
                            new HexCollisionBox(12.0D, 5.0D, 7.0D, 14.0D, 11.0D, 13.0D),
                            new HexCollisionBox(4.0D, 2.0D, 4.0D, 12.0D, 14.0D, 16.0D));
                case EAST:
                    return new ComplexCollisionBox(5,
                            new HexCollisionBox(0.0D, 6.0D, 2.0D, 9.0D, 10.0D, 4.0D),
                            new HexCollisionBox(0.0D, 6.0D, 12.0D, 9.0D, 10.0D, 14.0D),
                            new HexCollisionBox(7.0D, 5.0D, 2.0D, 13.0D, 11.0D, 4.0D),
                            new HexCollisionBox(7.0D, 5.0D, 12.0D, 13.0D, 11.0D, 14.0D),
                            new HexCollisionBox(4.0D, 2.0D, 4.0D, 16.0D, 14.0D, 12.0D));
            }
        } else {
            if (facing == BlockFace.NORTH || facing == BlockFace.SOUTH) {
                return new ComplexCollisionBox(5,
                        new HexCollisionBox(2.0D, 9.0D, 6.0D, 4.0D, 16.0D, 10.0D),
                        new HexCollisionBox(12.0D, 9.0D, 6.0D, 14.0D, 16.0D, 10.0D),
                        new HexCollisionBox(2.0D, 3.0D, 5.0D, 4.0D, 9.0D, 11.0D),
                        new HexCollisionBox(12.0D, 3.0D, 5.0D, 14.0D, 9.0D, 11.0D),
                        new HexCollisionBox(4.0D, 0.0D, 2.0D, 12.0D, 12.0D, 14.0D));
            } else {
                return new ComplexCollisionBox(5,
                        new HexCollisionBox(6.0D, 9.0D, 2.0D, 10.0D, 16.0D, 4.0D),
                        new HexCollisionBox(6.0D, 9.0D, 12.0D, 10.0D, 16.0D, 14.0D),
                        new HexCollisionBox(5.0D, 3.0D, 2.0D, 11.0D, 9.0D, 4.0D),
                        new HexCollisionBox(5.0D, 3.0D, 12.0D, 11.0D, 9.0D, 14.0D),
                        new HexCollisionBox(2.0D, 0.0D, 4.0D, 14.0D, 12.0D, 12.0D));
            }
        }

        return NoCollisionBox.INSTANCE;

    }, StateTypes.GRINDSTONE),

    PANE(new DynamicCollisionPane(), Materials.getPanes().toArray(new StateType[0])),

    CHAIN_BLOCK((player, version, data, x, y, z) -> {
        if (version.isOlderThan(ClientVersion.V_1_16)) {
            // viaversion replacement - iron bars
            return PANE.fetch(player, version, data, x, y, z);
        }

        if (data.getAxis() == Axis.X) {
            return new HexCollisionBox(0.0D, 6.5D, 6.5D, 16.0D, 9.5D, 9.5D);
        } else if (data.getAxis() == Axis.Y) {
            return new HexCollisionBox(6.5D, 0.0D, 6.5D, 9.5D, 16.0D, 9.5D);
        }

        return new HexCollisionBox(6.5D, 6.5D, 0.0D, 9.5D, 9.5D, 16.0D);
    }, Materials.getChains().toArray(new StateType[0])),

    CHORUS_PLANT(new DynamicChorusPlant(), StateTypes.CHORUS_PLANT),

    FENCE_GATE((player, version, data, x, y, z) -> {
        if (data.isOpen())
            return NoCollisionBox.INSTANCE;

        return switch (data.getFacing()) {
            case NORTH, SOUTH ->
                    new SimpleCollisionBox(0.0F, 0.0F, 0.375F, 1.0F, 1.5F, 0.625F, false);
            case WEST, EAST ->
                    new SimpleCollisionBox(0.375F, 0.0F, 0.0F, 0.625F, 1.5F, 1.0F, false);
            default -> // This code is unreachable but the compiler does not know this
                    NoCollisionBox.INSTANCE;
        };
    }, BlockTags.FENCE_GATES.getStates().toArray(new StateType[0])),

    FENCE(new DynamicCollisionFence(), BlockTags.FENCES.getStates().toArray(new StateType[0])),

    SNOW((player, version, data, x, y, z) -> {
        int layers = data.getLayers();
        if (layers == 1 && version.isNewerThanOrEquals(ClientVersion.V_1_13)) {
            if (PacketEvents.getAPI().getServerManager().getVersion().isNewerThanOrEquals(ServerVersion.V_1_13)
                    || !ViaVersionUtil.isAvailable || !Via.getConfig().isSnowCollisionFix()) {
                return NoCollisionBox.INSTANCE;
            }

            layers++;
        }

        return new SimpleCollisionBox(0, 0, 0, 1, (layers - 1) * 0.125, 1);
    }, StateTypes.SNOW),

    STAIR(new DynamicStair(), BlockTags.STAIRS.getStates().toArray(new StateType[0])),

    CHEST(new DynamicChest(), Materials.getChests().toArray(new StateType[0])),

    ENDER_CHEST(new SimpleCollisionBox(0.0625F, 0.0F, 0.0625F,
            0.9375F, 0.875F, 0.9375F, false),
            StateTypes.ENDER_CHEST),

    ENCHANTING_TABLE(new SimpleCollisionBox(0, 0, 0, 1, 1 - 0.25, 1, false),
            StateTypes.ENCHANTING_TABLE),

    FRAME((player, version, data, x, y, z) -> {
        ComplexCollisionBox complexCollisionBox = new ComplexCollisionBox(2, new HexCollisionBox(0.0D, 0.0D, 0.0D, 16.0D, 13.0D, 16.0D));

        if (data.isEye()) {
            if (version.isNewerThanOrEquals(ClientVersion.V_1_13)) {  // 1.13 players have a 0.5x0.5 eye
                complexCollisionBox.add(new HexCollisionBox(4.0D, 13.0D, 4.0D, 12.0D, 16.0D, 12.0D));
            } else { // 1.12 and below players have a 0.375x0.375 eye
                complexCollisionBox.add(new HexCollisionBox(5.0D, 13.0D, 5.0D, 11.0D, 16.0D, 11.0D));
            }
        }

        return complexCollisionBox;

    }, StateTypes.END_PORTAL_FRAME),

    CARPET((player, version, data, x, y, z) -> {
        if (version.isOlderThanOrEquals(ClientVersion.V_1_7_10))
            return new SimpleCollisionBox(0.0F, 0.0F, 0.0F, 1.0F, 0.0F, 1.0F, false);

        return new SimpleCollisionBox(0.0F, 0.0F, 0.0F, 1.0F, 0.0625F, 1.0F, false);
    }, BlockTags.WOOL_CARPETS.getStates().toArray(new StateType[0])),

    MOSS_CARPET(CARPET, StateTypes.MOSS_CARPET),

    PALE_MOSS_CARPET((player, version, data, x, y, z) -> {
        if (!data.isBottom()) {
            return NoCollisionBox.INSTANCE;
        }

        if (version.isOlderThan(ClientVersion.V_1_21_2)) {
            return MOSS_CARPET.fetch(player, version, data, x, y, z);
        }

        return new SimpleCollisionBox(0.0F, 0.0F, 0.0F, 1.0F, 0.0625F, 1.0F, false);
    }, StateTypes.PALE_MOSS_CARPET),

    DAYLIGHT(new SimpleCollisionBox(0.0F, 0.0F, 0.0F, 1.0F, 0.375, 1.0F, false),
            StateTypes.DAYLIGHT_DETECTOR),

    FARMLAND((player, version, data, x, y, z) -> {
        // Thanks Mojang for changing block collisions without changing protocol version!
        // Anyways, let a 1.10/1.10.1/1.10.2 client decide what farmland collision box it uses
        if (version == ClientVersion.V_1_10) {
            if (Math.abs(player.y % 1.0) < 0.001) {
                return new SimpleCollisionBox(0, 0, 0, 1, 1, 1, true);
            }
            return new HexCollisionBox(0.0D, 0.0D, 0.0D, 16.0D, 15.0D, 16.0D);
        }

        if (version.isNewerThanOrEquals(ClientVersion.V_1_10))
            return new HexCollisionBox(0.0D, 0.0D, 0.0D, 16.0D, 15.0D, 16.0D);

        return new SimpleCollisionBox(0, 0, 0, 1, 1, 1, true);

    }, StateTypes.FARMLAND),

    GRASS_PATH((player, version, data, x, y, z) -> {
        if (version.isNewerThanOrEquals(ClientVersion.V_1_9))
            return new HexCollisionBox(0.0D, 0.0D, 0.0D, 16.0D, 15.0D, 16.0D);

        return new SimpleCollisionBox(0, 0, 0, 1, 1, 1, true);

        // TODO, replace this hacky patch for 1.16.5 with versioned state types later. DIRT_PATH is new name for GRASS_PATH
    }, StateTypes.DIRT_PATH, StateTypes.GRASS_PATH),

    LILYPAD((player, version, data, x, y, z) -> {
        // Boats break lilypads client sided on 1.12- clients.
        if (player.inVehicle() && player.compensatedEntities.self.getRiding().isBoat && version.isOlderThanOrEquals(ClientVersion.V_1_12_2))
            return NoCollisionBox.INSTANCE;

        if (version.isOlderThan(ClientVersion.V_1_9))
            return new SimpleCollisionBox(0.0f, 0.0F, 0.0f, 1.0f, 0.015625F, 1.0f, false);
        return new HexCollisionBox(1.0D, 0.0D, 1.0D, 15.0D, 1.5D, 15.0D);
    }, StateTypes.LILY_PAD),

    BED((player, version, data, x, y, z) -> {
        // It's all the same box on 1.14 clients
        if (version.isOlderThan(ClientVersion.V_1_14))
            return new SimpleCollisionBox(0.0F, 0.0F, 0.0F, 1.0F, 0.5625, 1.0F, false);

        ComplexCollisionBox baseBox = new ComplexCollisionBox(3, new HexCollisionBox(0.0D, 3.0D, 0.0D, 16.0D, 9.0D, 16.0D));

        BlockFace facing = data.getPart() == Part.HEAD ? data.getFacing() : data.getFacing().getOppositeFace();

        switch (facing) {
            case NORTH:
                baseBox.add(new HexCollisionBox(0.0D, 0.0D, 0.0D, 3.0D, 3.0D, 3.0D));
                baseBox.add(new HexCollisionBox(13.0D, 0.0D, 0.0D, 16.0D, 3.0D, 3.0D));
                break;
            case SOUTH:
                baseBox.add(new HexCollisionBox(0.0D, 0.0D, 13.0D, 3.0D, 3.0D, 16.0D));
                baseBox.add(new HexCollisionBox(13.0D, 0.0D, 13.0D, 16.0D, 3.0D, 16.0D));
                break;
            case WEST:
                baseBox.add(new HexCollisionBox(0.0D, 0.0D, 0.0D, 3.0D, 3.0D, 3.0D));
                baseBox.add(new HexCollisionBox(0.0D, 0.0D, 13.0D, 3.0D, 3.0D, 16.0D));
                break;
            case EAST:
                baseBox.add(new HexCollisionBox(13.0D, 0.0D, 0.0D, 16.0D, 3.0D, 3.0D));
                baseBox.add(new HexCollisionBox(13.0D, 0.0D, 13.0D, 16.0D, 3.0D, 16.0D));
                break;
        }

        return baseBox;
    }, BlockTags.BEDS.getStates().toArray(new StateType[0])),

    TRAPDOOR(new TrapDoorHandler(), BlockTags.TRAPDOORS.getStates().toArray(new StateType[0])),


    DIODES(new SimpleCollisionBox(0.0F, 0.0F, 0.0F, 1.0F, 0.125F, 1.0F, false),
            StateTypes.REPEATER, StateTypes.COMPARATOR),

    STRUCTURE_VOID(new SimpleCollisionBox(0.375, 0.375, 0.375,
            0.625, 0.625, 0.625, false),
            StateTypes.STRUCTURE_VOID),

    END_ROD((player, version, data, x, y, z) -> getEndRod(version, data.getFacing()), Materials.getRods().toArray(new StateType[0])),

    CAULDRON((player, version, data, x, y, z) -> {
        if (version.isNewerThan(ClientVersion.V_1_13_2)) { // changed in 19w13a, 1.14 Snapshot
            return new ComplexCollisionBox(15,
                    new SimpleCollisionBox(0.0, 0.0, 0.0, 0.125, 1.0, 0.25, false),
                    new SimpleCollisionBox(0.0, 0.0, 0.75, 0.125, 1.0, 1.0, false),
                    new SimpleCollisionBox(0.125, 0.0, 0.0, 0.25, 1.0, 0.125, false),
                    new SimpleCollisionBox(0.125, 0.0, 0.875, 0.25, 1.0, 1.0, false),
                    new SimpleCollisionBox(0.75, 0.0, 0.0, 1.0, 1.0, 0.125, false),
                    new SimpleCollisionBox(0.75, 0.0, 0.875, 1.0, 1.0, 1.0, false),
                    new SimpleCollisionBox(0.875, 0.0, 0.125, 1.0, 1.0, 0.25, false),
                    new SimpleCollisionBox(0.875, 0.0, 0.75, 1.0, 1.0, 0.875, false),
                    new SimpleCollisionBox(0.0, 0.1875, 0.25, 1.0, 0.25, 0.75, false),
                    new SimpleCollisionBox(0.125, 0.1875, 0.125, 0.875, 0.25, 0.25, false),
                    new SimpleCollisionBox(0.125, 0.1875, 0.75, 0.875, 0.25, 0.875, false),
                    new SimpleCollisionBox(0.25, 0.1875, 0.0, 0.75, 1.0, 0.125, false),
                    new SimpleCollisionBox(0.25, 0.1875, 0.875, 0.75, 1.0, 1.0, false),
                    new SimpleCollisionBox(0.0, 0.25, 0.25, 0.125, 1.0, 0.75, false),
                    new SimpleCollisionBox(0.875, 0.25, 0.25, 1.0, 1.0, 0.75, false)
            );
        } else {
            double height = 0.25;
            if (version.isOlderThan(ClientVersion.V_1_13))
                height = 0.3125;

            return new ComplexCollisionBox(5,
                    new SimpleCollisionBox(0, 0, 0, 1, height, 1, false),
                    new SimpleCollisionBox(0, height, 0, 0.125, 1, 1, false),
                    new SimpleCollisionBox(1 - 0.125, height, 0, 1, 1, 1, false),
                    new SimpleCollisionBox(0, height, 0, 1, 1, 0.125, false),
                    new SimpleCollisionBox(0, height, 1 - 0.125, 1, 1, 1, false));
        }
    }, BlockTags.CAULDRONS.getStates().toArray(new StateType[0])),

    CACTUS(new SimpleCollisionBox(0.0625, 0, 0.0625,
            1 - 0.0625, 1 - 0.0625, 1 - 0.0625, false), StateTypes.CACTUS),


    PISTON_BASE(new PistonBaseCollision(), StateTypes.PISTON, StateTypes.STICKY_PISTON),

    PISTON_HEAD(new PistonHeadCollision(), StateTypes.PISTON_HEAD),

    SOULSAND(new SimpleCollisionBox(0, 0, 0, 1, 0.875, 1, false),
            StateTypes.SOUL_SAND),

    PICKLE((player, version, data, x, y, z) -> getPicklesBox(version, data.getPickles()), StateTypes.SEA_PICKLE),

    TURTLEEGG((player, version, data, x, y, z) -> {
        // ViaVersion replacement block (West facing cocoa beans)
        if (version.isOlderThanOrEquals(ClientVersion.V_1_12_2)) {
            return getCocoa(version, data.getEggs(), BlockFace.WEST);
        }

        if (data.getEggs() == 1) {
            return new HexCollisionBox(3.0D, 0.0D, 3.0D, 12.0D, 7.0D, 12.0D);
        }

        return new HexCollisionBox(1.0D, 0.0D, 1.0D, 15.0D, 7.0D, 15.0D);
    }, StateTypes.TURTLE_EGG),

    CONDUIT((player, version, data, x, y, z) -> {
        // ViaVersion replacement block - Beacon
        if (version.isOlderThanOrEquals(ClientVersion.V_1_12_2))
            return new SimpleCollisionBox(0, 0, 0, 1, 1, 1, true);

        return new HexCollisionBox(5.0D, 5.0D, 5.0D, 11.0D, 11.0D, 11.0D);
    }, StateTypes.CONDUIT),

    POT(new HexCollisionBox(5.0D, 0.0D, 5.0D, 11.0D, 6.0D, 11.0D),
            BlockTags.FLOWER_POTS.getStates().toArray(new StateType[0])),

    WALL_SIGN((player, version, data, x, y, z) -> switch (data.getFacing()) {
        case NORTH -> new HexCollisionBox(0.0D, 4.5D, 14.0D, 16.0D, 12.5D, 16.0D);
        case SOUTH -> new HexCollisionBox(0.0D, 4.5D, 0.0D, 16.0D, 12.5D, 2.0D);
        case WEST -> new HexCollisionBox(14.0D, 4.5D, 0.0D, 16.0D, 12.5D, 16.0D);
        case EAST -> new HexCollisionBox(0.0D, 4.5D, 0.0D, 2.0D, 12.5D, 16.0D);
        default -> NoCollisionBox.INSTANCE;
    }, BlockTags.WALL_SIGNS.getStates().toArray(new StateType[0])),

    WALL_FAN((player, version, data, x, y, z) -> switch (data.getFacing()) {
        case NORTH -> new HexCollisionBox(0.0D, 4.0D, 5.0D, 16.0D, 12.0D, 16.0D);
        case SOUTH -> new HexCollisionBox(0.0D, 4.0D, 0.0D, 16.0D, 12.0D, 11.0D);
        case WEST -> new HexCollisionBox(5.0D, 4.0D, 0.0D, 16.0D, 12.0D, 16.0D);
        default -> new HexCollisionBox(0.0D, 4.0D, 0.0D, 11.0D, 12.0D, 16.0D);
    }, BlockTags.WALL_CORALS.getStates().toArray(new StateType[0])),

    CORAL_PLANT((player, version, data, x, y, z) -> new HexCollisionBox(2.0D, 0.0D, 2.0D, 14.0D, 15.0D, 14.0D), Stream.concat(
                    Arrays.stream(BlockTags.CORAL_PLANTS.getStates().toArray(new StateType[0])),
                    Stream.of(StateTypes.DEAD_HORN_CORAL, StateTypes.DEAD_TUBE_CORAL, StateTypes.DEAD_BRAIN_CORAL,
                            StateTypes.DEAD_BUBBLE_CORAL, StateTypes.DEAD_FIRE_CORAL)
            )
            .distinct()  // This will remove duplicates
            .toArray(StateType[]::new)
    ),

    SIGN(new SimpleCollisionBox(0.25, 0.0, 0.25, 0.75, 1.0, 0.75, false),
            BlockTags.STANDING_SIGNS.getStates().toArray(new StateType[0])),

    STONE_PRESSURE_PLATE((player, version, data, x, y, z) -> {
        if (data.isPowered()) { // Pressed
            return new HexCollisionBox(1.0D, 0.0D, 1.0D, 15.0D, 0.5D, 15.0D);
        }

        return new HexCollisionBox(1.0D, 0.0D, 1.0D, 15.0D, 1.0D, 15.0D);
    }, BlockTags.STONE_PRESSURE_PLATES.getStates().toArray(new StateType[0])),

    WOOD_PRESSURE_PLATE((player, version, data, x, y, z) -> {
        if (data.isPowered()) { // Pressed
            return new HexCollisionBox(1.0D, 0.0D, 1.0D, 15.0D, 0.5D, 15.0D);
        }

        return new HexCollisionBox(1.0D, 0.0D, 1.0D, 15.0D, 1.0D, 15.0D);
    }, BlockTags.WOODEN_PRESSURE_PLATES.getStates().toArray(new StateType[0])),

    OTHER_PRESSURE_PLATE((player, version, data, x, y, z) -> {
        if (data.getPower() > 0) { // Pressed
            return new HexCollisionBox(1.0D, 0.0D, 1.0D, 15.0D, 0.5D, 15.0D);
        }

        return new HexCollisionBox(1.0D, 0.0D, 1.0D, 15.0D, 1.0D, 15.0D);
    }, StateTypes.LIGHT_WEIGHTED_PRESSURE_PLATE, StateTypes.HEAVY_WEIGHTED_PRESSURE_PLATE),

    TRIPWIRE((player, version, data, x, y, z) -> {
        if (data.isAttached()) {
            return new HexCollisionBox(0.0D, 1.0D, 0.0D, 16.0D, 2.5D, 16.0D);
        }
        return new HexCollisionBox(0.0D, 0.0D, 0.0D, 16.0D, 8.0D, 16.0D);
    }, StateTypes.TRIPWIRE),

    TRIPWIRE_HOOK((player, version, data, x, y, z) -> switch (data.getFacing()) {
        case NORTH -> new HexCollisionBox(5.0D, 0.0D, 10.0D, 11.0D, 10.0D, 16.0D);
        case SOUTH -> new HexCollisionBox(5.0D, 0.0D, 0.0D, 11.0D, 10.0D, 6.0D);
        case WEST -> new HexCollisionBox(10.0D, 0.0D, 5.0D, 16.0D, 10.0D, 11.0D);
        default -> new HexCollisionBox(0.0D, 0.0D, 5.0D, 6.0D, 10.0D, 11.0D);
    }, StateTypes.TRIPWIRE_HOOK),

    TORCH(new HexCollisionBox(6.0D, 0.0D, 6.0D, 10.0D, 10.0D, 10.0D),
            StateTypes.TORCH, StateTypes.REDSTONE_TORCH, StateTypes.COPPER_TORCH),

    WALL_TORCH((player, version, data, x, y, z) -> switch (data.getFacing()) {
        case NORTH -> new HexCollisionBox(5.5D, 3.0D, 11.0D, 10.5D, 13.0D, 16.0D);
        case SOUTH -> new HexCollisionBox(5.5D, 3.0D, 0.0D, 10.5D, 13.0D, 5.0D);
        case WEST -> new HexCollisionBox(11.0D, 3.0D, 5.5D, 16.0D, 13.0D, 10.5D);
        case EAST -> new HexCollisionBox(0.0D, 3.0D, 5.5D, 5.0D, 13.0D, 10.5D);
        // 1.13 separates wall and normal torches, 1.12 does not
        default -> new HexCollisionBox(6.0D, 0.0D, 6.0D, 10.0D, 10.0D, 10.0D);
    }, StateTypes.WALL_TORCH, StateTypes.REDSTONE_WALL_TORCH, StateTypes.COPPER_WALL_TORCH),

    // 1.17 blocks
    CANDLE((player, version, data, x, y, z) -> {
        if (version.isNewerThanOrEquals(ClientVersion.V_1_17)) {
            return switch (data.getCandles()) {
                case 1 -> new HexCollisionBox(7.0, 0.0, 7.0, 9.0, 6.0, 9.0);
                case 2 -> new HexCollisionBox(5.0, 0.0, 6.0, 11.0, 6.0, 9.0);
                case 3 -> new HexCollisionBox(5.0, 0.0, 6.0, 10.0, 6.0, 11.0);
                default -> new HexCollisionBox(5.0, 0.0, 5.0, 11.0, 6.0, 10.0);
            };
        }

        return getPicklesBox(version, data.getCandles());
    }, BlockTags.CANDLES.getStates().toArray(new StateType[0])),

    CANDLE_CAKE((player, version, data, x, y, z) -> {
        SimpleCollisionBox cake = new HexCollisionBox(1.0, 0.0, 1.0, 15.0, 8.0, 15.0);
        if (version.isOlderThan(ClientVersion.V_1_17)) {
            return cake;
        } else {
            return new ComplexCollisionBox(2,
                    cake,
                    new HexCollisionBox(7.0, 8.0, 7.0, 9.0, 14.0, 9.0));
        }
    }, BlockTags.CANDLE_CAKES.getStates().toArray(new StateType[0])),

    SCULK_SENSOR(new HexCollisionBox(0.0, 0.0, 0.0, 16.0, 8.0, 16.0), StateTypes.SCULK_SENSOR, StateTypes.CALIBRATED_SCULK_SENSOR),

    DECORATED_POT((player, version, data, x, y, z) -> {
        if (version.isNewerThan(ClientVersion.V_1_19_3)) {
            return new HexCollisionBox(1.0D, 0.0D, 1.0D, 15.0D, 16.0D, 15.0);
        } else { // ViaVersion replacement is a Brick
            return new SimpleCollisionBox(0, 0, 0, 1, 1, 1, true);
        }
    }, StateTypes.DECORATED_POT),

    BIG_DRIPLEAF((player, version, data, x, y, z) -> {
        Tilt tilt = data.getTilt();
        if (version.isOlderThanOrEquals(ClientVersion.V_1_16_4)) {
            if (tilt == Tilt.FULL) {
                return new SimpleCollisionBox(0, 0, 0, 1, 0.5, 1, false);
            } else {
                return new SimpleCollisionBox(0, 0, 0, 1, 1, 1, true);
            }
        } else {
            if (tilt == Tilt.NONE || tilt == Tilt.UNSTABLE) {
                return new HexCollisionBox(0.0, 11.0, 0.0, 16.0, 15.0, 16.0);
            } else if (tilt == Tilt.PARTIAL) {
                return new HexCollisionBox(0.0, 11.0, 0.0, 16.0, 13.0, 16.0);
            } else {
                return NoCollisionBox.INSTANCE;
            }
        }
    }, StateTypes.BIG_DRIPLEAF),

    POINTED_DRIPSTONE((player, version, data, x, y, z) -> {
        if (version.isOlderThan(ClientVersion.V_1_17))
            return getEndRod(version, BlockFace.UP);

        SimpleCollisionBox box;

        if (data.getThickness() == Thickness.TIP_MERGE) {
            box = new HexOffsetCollisionBox(data.getType(), 5.0, 0.0, 5.0, 11.0, 16.0, 11.0);
        } else if (data.getThickness() == Thickness.TIP) {
            if (data.getVerticalDirection() == VerticalDirection.DOWN) {
                box = new HexOffsetCollisionBox(data.getType(), 5.0, 5.0, 5.0, 11.0, 16.0, 11.0);
            } else {
                box = new HexOffsetCollisionBox(data.getType(), 5.0, 0.0, 5.0, 11.0, 11.0, 11.0);
            }
        } else if (data.getThickness() == Thickness.FRUSTUM) {
            box = new HexOffsetCollisionBox(data.getType(), 4.0, 0.0, 4.0, 12.0, 16.0, 12.0);
        } else if (data.getThickness() == Thickness.MIDDLE) {
            box = new HexOffsetCollisionBox(data.getType(), 3.0, 0.0, 3.0, 13.0, 16.0, 13.0);
        } else {
            box = new HexOffsetCollisionBox(data.getType(), 2.0, 0.0, 2.0, 14.0, 16.0, 14.0);
        }

        return box;
    }, StateTypes.POINTED_DRIPSTONE),

    POWDER_SNOW((player, version, data, x, y, z) -> {
        if (version.isOlderThanOrEquals(ClientVersion.V_1_16_4))
            return new SimpleCollisionBox(0, 0, 0, 1, 1, 1, true);

        // If fall distance greater than 2.5, 0.899999 box
        if (player.fallDistance > 2.5) {
            // TODO: this is technically incorrect (1.21.4)
            return player.getClientVersion() == ClientVersion.V_1_21_4 ?
                    new SimpleCollisionBox(0, 0, 0, 1, 1, 1, true)
                    : new SimpleCollisionBox(0.0, 0.0, 0.0, 1.0, 0.9, 1.0, false);
        }

        ItemStack boots = player.inventory.getBoots();
        if (player.lastY > y + 1 - 1e-5 && boots != null && boots.getType() == ItemTypes.LEATHER_BOOTS && !player.isSneaking && !player.inVehicle())
            return new SimpleCollisionBox(0, 0, 0, 1, 1, 1, true);

        return NoCollisionBox.INSTANCE;

    }, StateTypes.POWDER_SNOW),

    NETHER_PORTAL((player, version, data, x, y, z) -> {
        if (data.getAxis() == Axis.X) {
            return new HexCollisionBox(0.0D, 0.0D, 6.0D, 16.0D, 16.0D, 10.0D);
        }
        return new HexCollisionBox(6.0D, 0.0D, 0.0D, 10.0D, 16.0D, 16.0D);
    }, StateTypes.NETHER_PORTAL),

    AZALEA((player, version, data, x, y, z) -> new ComplexCollisionBox(2,
            new HexCollisionBox(0.0, 8.0, 0.0, 16.0, 16.0, 16.0),
            new HexCollisionBox(6.0, 0.0, 6.0, 10.0, 8.0, 10.0)), StateTypes.AZALEA, StateTypes.FLOWERING_AZALEA),

    AMETHYST_CLUSTER((player, version, data, x, y, z) -> getAmethystBox(version, data.getFacing(), 7, 3), StateTypes.AMETHYST_CLUSTER),

    SMALL_AMETHYST_BUD((player, version, data, x, y, z) -> getAmethystBox(version, data.getFacing(), 3, 4), StateTypes.SMALL_AMETHYST_BUD),

    MEDIUM_AMETHYST_BUD((player, version, data, x, y, z) -> getAmethystBox(version, data.getFacing(), 4, 3), StateTypes.MEDIUM_AMETHYST_BUD),

    LARGE_AMETHYST_BUD((player, version, data, x, y, z) -> getAmethystBox(version, data.getFacing(), 5, 3), StateTypes.LARGE_AMETHYST_BUD),

    MUD_BLOCK((player, version, data, x, y, z) -> {
        if (version.isNewerThanOrEquals(ClientVersion.V_1_19)) {
            return new HexCollisionBox(0.0D, 0.0D, 0.0D, 16.0D, 14.0D, 16.0D);
        }
        return new SimpleCollisionBox(0, 0, 0, 1, 1, 1);
    }, StateTypes.MUD),

    MANGROVE_PROPAGULE_BLOCK((player, version, data, x, y, z) -> {
        if (!data.isHanging()) {
            return new HexCollisionBox(7.0D, 0.0D, 7.0D, 9.0D, 16.0D, 9.0D);
        }
        return switch (data.getAge()) {
            case 0 -> new HexCollisionBox(7.0D, 13.0D, 7.0D, 9.0D, 16.0D, 9.0D);
            case 1 -> new HexCollisionBox(7.0D, 10.0D, 7.0D, 9.0D, 16.0D, 9.0D);
            case 2 -> new HexCollisionBox(7.0D, 7.0D, 7.0D, 9.0D, 16.0D, 9.0D);
            case 3 -> new HexCollisionBox(7.0D, 3.0D, 7.0D, 9.0D, 16.0D, 9.0D);
            default -> new HexCollisionBox(7.0D, 0.0D, 7.0D, 9.0D, 16.0D, 9.0D);
        };
    }, StateTypes.MANGROVE_PROPAGULE),

    SCULK_SHRIKER((player, version, data, x, y, z) -> {
        if (version.isNewerThan(ClientVersion.V_1_18_2)) {
            return new HexCollisionBox(0.0D, 0.0D, 0.0D, 16.0D, 8.0D, 16.0D);
        } else {
            return new SimpleCollisionBox(0, 0, 0, 1, 1, 1, true);
        }
    }, StateTypes.SCULK_SHRIEKER),

    SNIFFER_EGG((player, version, data, x, y, z) -> {
        if (version.isNewerThan(ClientVersion.V_1_19_4)) {
            return new HexCollisionBox(1.0D, 0.0D, 2.0D, 15.0D, 16.0D, 14.0D);
        } else {
            return new SimpleCollisionBox(0, 0, 0, 1, 1, 1, true);
        }
    }, StateTypes.SNIFFER_EGG),

    PITCHER_CROP((player, version, data, x, y, z) -> {
        if (version.isNewerThan(ClientVersion.V_1_19_4)) {
            final SimpleCollisionBox COLLISION_SHAPE_BULB = new HexCollisionBox(5.0D, -1.0D, 5.0D, 11.0D, 3.0D, 11.0D);
            final SimpleCollisionBox COLLISION_SHAPE_CROP = new HexCollisionBox(3.0D, -1.0D, 3.0D, 13.0D, 5.0D, 13.0D);

            if (data.getAge() == 0) {
                return COLLISION_SHAPE_BULB;
            } else {
                return data.getHalf() == Half.LOWER ? COLLISION_SHAPE_CROP : NoCollisionBox.INSTANCE;
            }
        } else {
            return NoCollisionBox.INSTANCE;
        }
    }, StateTypes.PITCHER_CROP),

    WALL_HANGING_SIGNS((player, version, data, x, y, z) -> switch (data.getFacing()) {
        case NORTH, SOUTH -> new HexCollisionBox(0.0, 14.0, 6.0, 16.0, 16.0, 10.0);
        case WEST, EAST -> new HexCollisionBox(6.0, 14.0, 0.0, 10.0, 16.0, 16.0);
        default -> NoCollisionBox.INSTANCE;
    }, BlockTags.WALL_HANGING_SIGNS.getStates().toArray(new StateType[0])),

    DRIED_GHAST((player, version, data, x, y, z) -> {
        if (player.getClientVersion().isNewerThan(ClientVersion.V_1_21_5)) {
            return new HexCollisionBox(3.0, 0.0, 3.0, 13.0, 10.0, 13.0);
        // ViaVersion replacement block - chorus plant (down: true, up: false, east: false, south: false, west: false)
        } else if (player.getClientVersion().isNewerThan(ClientVersion.V_1_12_2)) {
            // While the 2nd SimpleCollisionBox clearly encompasses the first, it's unclear if Mojang's collision code on any version
            // May give a different result if the vanilla boxes aren't replicated perfectly, even the inefficiencies like the code below
            return new ComplexCollisionBox(2,
                    new SimpleCollisionBox(0.1875, 0.1875, 0.1875, 0.8125, 0.8125, 0.8125),
                    new SimpleCollisionBox(0.1875, 0, 0.1875, 0.8125, 0.8125, 0.8125)
            );
        } else if (player.getClientVersion().isNewerThan(ClientVersion.V_1_8)) {
            return new SimpleCollisionBox(0.1875F, 0.0F, 0.1875F, 0.8125F, 0.8125F, 0.8125F);
        } else {
            // ViaVersion replacement block (Purple wool)
            return new SimpleCollisionBox(0, 0, 0, 1, 1, 1, true);
        }
    }, StateTypes.DRIED_GHAST),

    SHELF((player, version, data, x, y, z) -> {
        if (version.isOlderThan(ClientVersion.V_1_21_9)) {
            // ViaVersion replacement block (planks)
            return new SimpleCollisionBox(0, 0, 0, 1, 1, 1, true);
        }

        return switch (data.getFacing()) {
            case NORTH -> new ComplexCollisionBox(3, new HexCollisionBox(0, 12, 11, 16, 16, 13), new HexCollisionBox(0, 0, 13, 16, 16, 16), new HexCollisionBox(0, 0, 11, 16, 4, 13));
            case SOUTH -> new ComplexCollisionBox(3, new HexCollisionBox(0, 12, 3, 16, 16, 5), new HexCollisionBox(0, 0, 0, 16, 16, 3), new HexCollisionBox(0, 0, 3, 16, 4, 5));
            case WEST -> new ComplexCollisionBox(3, new HexCollisionBox(11, 12, 0, 13, 16, 16), new HexCollisionBox(13, 0, 0, 16, 16, 16), new HexCollisionBox(11, 0, 0, 13, 4, 16));
            case EAST -> new ComplexCollisionBox(3, new HexCollisionBox(3, 12, 0, 5, 16, 16), new HexCollisionBox(0, 0, 0, 3, 16, 16), new HexCollisionBox(3, 0, 0, 5, 4, 16));
            default -> throw new IllegalStateException("Unexpected value: " + data.getFacing());
        };
    }, BlockTags.WOODEN_SHELVES.getStates().toArray(new StateType[0])),

    COPPER_GOLEM_STATUE((player, version, data, x, y, z) -> {
        if (version.isOlderThan(ClientVersion.V_1_21_9)) {
            // ViaVersion replacement block (copper block)
            return new SimpleCollisionBox(0, 0, 0, 1, 1, 1, true);
        }

        return new HexCollisionBox(3, 0, 3, 13, 14, 13);
    }, BlockTags.COPPER_GOLEM_STATUES.getStates().toArray(new StateType[0])),

    DEFAULT(new SimpleCollisionBox(0, 0, 0, 1, 1, 1, true), StateTypes.STONE);

    // This should be an array... but a hashmap will do for now...
    private static final Map<StateType, CollisionData> rawLookupMap = new IdentityHashMap<>();

    static {
        for (CollisionData data : values()) {
            for (StateType type : data.materials) {
                rawLookupMap.put(type, data);
            }
        }
    }

    public final StateType[] materials;
    public CollisionBox box;
    public CollisionFactory dynamic;

    CollisionData(CollisionBox box, StateType... states) {
        this.box = box;
        Set<StateType> mList = new HashSet<>(Arrays.asList(states));
        mList.remove(null); // Sets can contain one null
        this.materials = mList.toArray(new StateType[0]);
    }

    CollisionData(CollisionFactory dynamic, StateType... states) {
        this.dynamic = dynamic;
        Set<StateType> mList = new HashSet<>(Arrays.asList(states));
        mList.remove(null); // Sets can contain one null
        this.materials = mList.toArray(new StateType[0]);
    }

    private static CollisionBox getAmethystBox(ClientVersion version, BlockFace facing, int param_0, int param_1) {
        if (version.isOlderThanOrEquals(ClientVersion.V_1_16_4))
            return NoCollisionBox.INSTANCE;

        return switch (facing) {
            case DOWN ->
                    new HexCollisionBox(param_1, 16 - param_0, param_1, 16 - param_1, 16.0, 16 - param_1);
            case NORTH ->
                    new HexCollisionBox(param_1, param_1, 16 - param_0, 16 - param_1, 16 - param_1, 16.0);
            case SOUTH ->
                    new HexCollisionBox(param_1, param_1, 0.0, 16 - param_1, 16 - param_1, param_0);
            case EAST ->
                    new HexCollisionBox(0.0, param_1, param_1, param_0, 16 - param_1, 16 - param_1);
            case WEST ->
                    new HexCollisionBox(16 - param_0, param_1, param_1, 16.0, 16 - param_1, 16 - param_1);
            default ->
                    new HexCollisionBox(param_1, 0.0, param_1, 16 - param_1, param_0, 16 - param_1);
        };
    }

    private static CollisionBox getPicklesBox(ClientVersion version, int pickles) {
        // ViaVersion replacement block (West facing cocoa beans)
        if (version.isOlderThanOrEquals(ClientVersion.V_1_12_2)) {
            return getCocoa(version, pickles, BlockFace.WEST);
        }

        return switch (pickles) {
            case 1 -> new HexCollisionBox(6.0D, 0.0D, 6.0D, 10.0D, 6.0D, 10.0D);
            case 2 -> new HexCollisionBox(3.0D, 0.0D, 3.0D, 13.0D, 6.0D, 13.0D);
            case 3 -> new HexCollisionBox(2.0D, 0.0D, 2.0D, 14.0D, 6.0D, 14.0D);
            case 4 -> new HexCollisionBox(2.0D, 0.0D, 2.0D, 14.0D, 7.0D, 14.0D);
            default -> NoCollisionBox.INSTANCE;
        };
    }

    public static CollisionBox getCocoa(ClientVersion version, int age, BlockFace direction) {
        // From 1.9 - 1.10, the large cocoa block is the same as the medium one
        // https://bugs.mojang.com/browse/MC-94274
        if (version.isNewerThanOrEquals(ClientVersion.V_1_9_1) && version.isOlderThan(ClientVersion.V_1_11))
            age = Math.min(age, 1);

        switch (direction) {
            case EAST:
                switch (age) {
                    case 0:
                        return new HexCollisionBox(11.0D, 7.0D, 6.0D, 15.0D, 12.0D, 10.0D);
                    case 1:
                        return new HexCollisionBox(9.0D, 5.0D, 5.0D, 15.0D, 12.0D, 11.0D);
                    case 2:
                        return new HexCollisionBox(7.0D, 3.0D, 4.0D, 15.0D, 12.0D, 12.0D);
                }
            case WEST:
                switch (age) {
                    case 0:
                        return new HexCollisionBox(1.0D, 7.0D, 6.0D, 5.0D, 12.0D, 10.0D);
                    case 1:
                        return new HexCollisionBox(1.0D, 5.0D, 5.0D, 7.0D, 12.0D, 11.0D);
                    case 2:
                        return new HexCollisionBox(1.0D, 3.0D, 4.0D, 9.0D, 12.0D, 12.0D);
                }
            case NORTH:
                switch (age) {
                    case 0:
                        return new HexCollisionBox(6.0D, 7.0D, 1.0D, 10.0D, 12.0D, 5.0D);
                    case 1:
                        return new HexCollisionBox(5.0D, 5.0D, 1.0D, 11.0D, 12.0D, 7.0D);
                    case 2:
                        return new HexCollisionBox(4.0D, 3.0D, 1.0D, 12.0D, 12.0D, 9.0D);
                }
            case SOUTH:
                switch (age) {
                    case 0:
                        return new HexCollisionBox(6.0D, 7.0D, 11.0D, 10.0D, 12.0D, 15.0D);
                    case 1:
                        return new HexCollisionBox(5.0D, 5.0D, 9.0D, 11.0D, 12.0D, 15.0D);
                    case 2:
                        return new HexCollisionBox(4.0D, 3.0D, 7.0D, 12.0D, 12.0D, 15.0D);
                }
        }
        return NoCollisionBox.INSTANCE;
    }

    private static CollisionBox getEndRod(ClientVersion version, BlockFace face) {
        // ViaVersion replacement block - torch
        if (version.isOlderThan(ClientVersion.V_1_9))
            return NoCollisionBox.INSTANCE;

        return switch (face) {
            case NORTH, SOUTH -> new HexCollisionBox(6.0D, 6.0D, 0.0D, 10.0D, 10.0D, 16.0D);
            case EAST, WEST -> new HexCollisionBox(0.0D, 6.0D, 6.0D, 16.0D, 10.0D, 10.0D);
            default -> new HexCollisionBox(6.0D, 0.0D, 6.0D, 10.0D, 16.0D, 10.0);
        };
    }

    // Would pre-computing all states be worth the memory cost? I doubt it
    public static CollisionData getData(StateType state) { // TODO: Find a better hack for lava and scaffolding
        // What the fuck mojang, why put noCollision() and then give PITCHER_CROP collision?
        return state.isSolid() || state == StateTypes.LAVA || state == StateTypes.SCAFFOLDING
                || state == StateTypes.PITCHER_CROP || state == StateTypes.HEAVY_CORE
                || state == StateTypes.PALE_MOSS_CARPET || BlockTags.WALL_HANGING_SIGNS.contains(state)
                || BlockTags.COPPER_GOLEM_STATUES.contains(state)
                ? rawLookupMap.getOrDefault(state, DEFAULT) : NO_COLLISION;
    }

    // TODO: This is wrong if a block doesn't have any hitbox and isn't specified, light block?
    public static CollisionData getRawData(StateType state) {
        return rawLookupMap.getOrDefault(state, DEFAULT);
    }

    public CollisionBox getMovementCollisionBox(GrimPlayer player, ClientVersion version, WrappedBlockState block, int x, int y, int z) {
        return fetch(player, version, block, x, y, z).offset(x, y, z);
    }

    public CollisionBox getMovementCollisionBox(GrimPlayer player, ClientVersion version, WrappedBlockState block) {
        if (this.box != null)
            return this.box.copy();

        return new DynamicCollisionBox(player, version, dynamic, block);
    }

    @Override
    public CollisionBox fetch(GrimPlayer player, ClientVersion version, WrappedBlockState block, int x, int y, int z) {
        return box != null ? box.copy() : new DynamicCollisionBox(player, version, dynamic, block);
    }
}
