package ac.grim.grimac.utils.collisions;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.collisions.blocks.connecting.DynamicHitboxFence;
import ac.grim.grimac.utils.collisions.blocks.connecting.DynamicHitboxPane;
import ac.grim.grimac.utils.collisions.blocks.connecting.DynamicHitboxWall;
import ac.grim.grimac.utils.collisions.datatypes.*;
import ac.grim.grimac.utils.nmsutil.Materials;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.world.BlockFace;
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import com.github.retrooper.packetevents.protocol.world.states.defaulttags.BlockTags;
import com.github.retrooper.packetevents.protocol.world.states.enums.*;
import com.github.retrooper.packetevents.protocol.world.states.type.StateType;
import com.github.retrooper.packetevents.protocol.world.states.type.StateTypes;

import java.util.*;

// Expansion to the CollisionData class, which is different than regular ray tracing hitboxes
public enum HitboxData {

    RAILS((player, item, version, data, x, y, z) -> {
        switch (data.getShape()) {
            case ASCENDING_NORTH:
            case ASCENDING_SOUTH:
            case ASCENDING_EAST:
            case ASCENDING_WEST:
                if (version.isOlderThan(ClientVersion.V_1_8)) {
                    StateType railType = data.getType();
                    // Activator rails always appear as flat detector rails in 1.7.10 because of ViaVersion
                    // Ascending power rails in 1.7 have flat rail hitbox https://bugs.mojang.com/browse/MC-9134
                    if (railType == StateTypes.ACTIVATOR_RAIL || (railType == StateTypes.POWERED_RAIL && data.isPowered())) {
                        return new SimpleCollisionBox(0.0F, 0.0F, 0.0F, 1.0F, 0.125F, 1.0F, false);
                    }
                    return new SimpleCollisionBox(0.0F, 0.0F, 0.0F, 1.0F, 0.625F, 1.0F, false);
                } else if (version.isOlderThan(ClientVersion.V_1_9)) {
                    return new SimpleCollisionBox(0.0F, 0.0F, 0.0F, 1.0F, 0.625F, 1.0F, false);
                } else if (version.isNewerThanOrEquals(ClientVersion.V_1_9) && version.isOlderThan(ClientVersion.V_1_10)) {
                    // https://bugs.mojang.com/browse/MC-89552 sloped rails in 1.9 - it is slightly taller than a regular rail
                    return new SimpleCollisionBox(0.0F, 0.0F, 0.0F, 1.0F, 0.1875F, 1.0F, false);
                } else if (version.isOlderThan(ClientVersion.V_1_11)) {
                    // https://bugs.mojang.com/browse/MC-102638 All sloped rails are full blocks in 1.10
                    return new SimpleCollisionBox(0, 0, 0, 1, 1, 1, true);
                }
                return new HexCollisionBox(0.0D, 0.0D, 0.0D, 16.0D, 8.0D, 16.0D);
            default:
                return new HexCollisionBox(0.0D, 0.0D, 0.0D, 16.0D, 2.0D, 16.0D);
        }
    }, BlockTags.RAILS.getStates().toArray(new StateType[0])),

    END_PORTAL((player, item, version, data, x, y, z) -> {
        if (version.isOlderThan(ClientVersion.V_1_9)) {
            return new SimpleCollisionBox(0.0D, 0.0D, 0.0D, 1.0D, 0.0625D, 1.0D);
        } else if (version.isOlderThan(ClientVersion.V_1_17)) {
            return new SimpleCollisionBox(0.0, 0.0D, 0.0D, 1.0D, 0.75D, 1.0D);
        }
        return new HexCollisionBox(0.0D, 6.0D, 0.0D, 16.0D, 12.0D, 16.0D);
    }, StateTypes.END_PORTAL),

    FENCE_GATE((player, item, version, data, x, y, z) -> {
        // This technically should be taken from the block data/made multi-version/run block updates... but that's too far even for me
        // This way is so much easier and works unless the magic stick wand is used
        boolean isInWall;
        boolean isXAxis = data.getFacing() == BlockFace.WEST || data.getFacing() == BlockFace.EAST;
        if (isXAxis) {
            boolean zPosWall = Materials.isWall(player.compensatedWorld.getStateTypeAt(x, y, z + 1));
            boolean zNegWall = Materials.isWall(player.compensatedWorld.getStateTypeAt(x, y, z - 1));
            isInWall = zPosWall || zNegWall;
        } else {
            boolean xPosWall = Materials.isWall(player.compensatedWorld.getStateTypeAt(x + 1, y, z));
            boolean xNegWall = Materials.isWall(player.compensatedWorld.getStateTypeAt(x - 1, y, z));
            isInWall = xPosWall || xNegWall;
        }

        if (isInWall) {
            return isXAxis ? new HexCollisionBox(6.0D, 0.0D, 0.0D, 10.0D, 13.0D, 16.0D) : new HexCollisionBox(0.0D, 0.0D, 6.0D, 16.0D, 13.0D, 10.0D);
        }

        return isXAxis ? new HexCollisionBox(6.0D, 0.0D, 0.0D, 10.0D, 16.0D, 16.0D) : new HexCollisionBox(0.0D, 0.0D, 6.0D, 16.0D, 16.0D, 10.0D);
    }, BlockTags.FENCE_GATES.getStates().toArray(new StateType[0])),


    FENCE(new DynamicHitboxFence(), BlockTags.FENCES.getStates().toArray(new StateType[0])),

    PANE(new DynamicHitboxPane(), Materials.getPanes().toArray(new StateType[0])),

    LEVER(((player, item, version, data, x, y, z) -> {
        Face face = data.getFace();
        BlockFace facing = data.getFacing();
        if (version.isOlderThan(ClientVersion.V_1_13)) {
            double f = 0.1875;

            switch (face) {
                case WALL:
                    switch (facing) {
                        case WEST:
                            return new SimpleCollisionBox(1.0 - f * 2.0, 0.2, 0.5 - f, 1.0, 0.8, 0.5 + f, false);
                        case EAST:
                            return new SimpleCollisionBox(0.0, 0.2, 0.5 - f, f * 2.0, 0.8, 0.5 + f, false);
                        case NORTH:
                            return new SimpleCollisionBox(0.5 - f, 0.2, 1.0 - f * 2.0, 0.5 + f, 0.8, 1.0, false);
                        case SOUTH:
                            return new SimpleCollisionBox(0.5 - f, 0.2, 0.0, 0.5 + f, 0.8, f * 2.0, false);
                    }
                case CEILING:
                    return new SimpleCollisionBox(0.25, 0.4, 0.25, 0.75, 1.0, 0.75, false);
                case FLOOR:
                    return new SimpleCollisionBox(0.25, 0.0, 0.25, 0.75, 0.6, 0.75, false);
            }
        }

        switch (face) {
            case FLOOR:
                // X-AXIS
                if (facing == BlockFace.EAST || facing == BlockFace.WEST) {
                    return new SimpleCollisionBox(0.25, 0.0, 0.3125, 0.75, 0.375, 0.6875, false);
                }
                // Z-AXIS
                return new SimpleCollisionBox(0.3125, 0.0, 0.25, 0.6875, 0.375, 0.75, false);
            case WALL:
                switch (facing) {
                    case EAST:
                        return new SimpleCollisionBox(0.0, 0.25, 0.3125, 0.375, 0.75, 0.6875, false);
                    case WEST:
                        return new SimpleCollisionBox(0.625, 0.25, 0.3125, 1.0, 0.75, 0.6875, false);
                    case SOUTH:
                        return new SimpleCollisionBox(0.3125, 0.25, 0.0, 0.6875, 0.75, 0.375, false);
                    case NORTH:
                    default:
                        return new SimpleCollisionBox(0.3125, 0.25, 0.625, 0.6875, 0.75, 1.0, false);
                }
            case CEILING:
            default:
                // X-AXIS
                if (facing == BlockFace.EAST || facing == BlockFace.WEST) {
                    return new SimpleCollisionBox(0.25, 0.625, 0.3125, 0.75, 1.0, 0.6875, false);
                }
                // Z-Axis
                return new SimpleCollisionBox(0.3125, 0.625, 0.25, 0.6875, 1.0, 0.75, false);
        }
    }), StateTypes.LEVER),

    BUTTON((player, item, version, data, x, y, z) -> {
        final Face face = data.getFace();
        final BlockFace facing = data.getFacing();
        final boolean powered = data.isPowered();


        if (version.isOlderThan(ClientVersion.V_1_13)) {
            double f2 = (float) (data.isPowered() ? 1 : 2) / 16.0;

            switch (face) {
                case WALL:
                    switch (facing) {
                        case WEST:
                            return new SimpleCollisionBox(0.0, 0.375, 0.3125, f2, 0.625, 0.6875, false);
                        case EAST:
                            return new SimpleCollisionBox(1.0 - f2, 0.375, 0.3125, 1.0, 0.625, 0.6875, false);
                        case NORTH:
                            return new SimpleCollisionBox(0.3125, 0.375, 0.0, 0.6875, 0.625, f2, false);
                        case SOUTH:
                            return new SimpleCollisionBox(0.3125, 0.375, 1.0 - f2, 0.6875, 0.625, 1.0, false);
                    }
                case CEILING:
                    return new SimpleCollisionBox(0.3125, 1.0 - f2, 0.375, 0.6875, 1.0, 0.625, false);
                case FLOOR:
                    return new SimpleCollisionBox(0.3125, 0.0, 0.375, 0.6875, 0.0 + f2, 0.625, false);
            }
        }


        switch (face) {
            case WALL:
                CollisionBox shape;
                switch (facing) {
                    case EAST:
                        shape = powered ? new HexCollisionBox(0.0, 6.0, 5.0, 1.0, 10.0, 11.0) : new HexCollisionBox(0.0, 6.0, 5.0, 2.0, 10.0, 11.0);
                        break;
                    case WEST:
                        shape = powered ? new HexCollisionBox(15.0, 6.0, 5.0, 16.0, 10.0, 11.0) : new HexCollisionBox(14.0, 6.0, 5.0, 16.0, 10.0, 11.0);
                        break;
                    case SOUTH:
                        shape = powered ? new HexCollisionBox(5.0, 6.0, 0.0, 11.0, 10.0, 1.0) : new HexCollisionBox(5.0, 6.0, 0.0, 11.0, 10.0, 2.0);
                        break;
                    case NORTH:
                    case UP:
                    case DOWN:
                        shape = powered ? new HexCollisionBox(5.0, 6.0, 15.0, 11.0, 10.0, 16.0) : new HexCollisionBox(5.0, 6.0, 14.0, 11.0, 10.0, 16.0);
                        break;
                    default:
                        shape = NoCollisionBox.INSTANCE;
                }

                return shape;
            case CEILING:
                // ViaVersion shows lever
                if (player.getClientVersion().isOlderThan(ClientVersion.V_1_8)) {
                    return LEVER.dynamic.fetch(player, item, version, data, x, y, z);
                }
                // x axis
                if (facing == BlockFace.EAST || facing == BlockFace.WEST) {
                    return powered ? new HexCollisionBox(6.0, 15.0, 5.0, 10.0, 16.0, 11.0) : new HexCollisionBox(6.0, 14.0, 5.0, 10.0, 16.0, 11.0);
                } else {
                    return powered ? new HexCollisionBox(5.0, 15.0, 6.0, 11.0, 16.0, 10.0) : new HexCollisionBox(5.0, 14.0, 6.0, 11.0, 16.0, 10.0);
                }
            case FLOOR:
                // ViaVersion shows lever
                if (player.getClientVersion().isOlderThan(ClientVersion.V_1_8)) {
                    return LEVER.dynamic.fetch(player, item, version, data, x, y, z);
                }
                // x axis
                if (facing == BlockFace.EAST || facing == BlockFace.WEST) {
                    return powered ? new HexCollisionBox(6.0, 0.0, 5.0, 10.0, 1.0, 11.0) : new HexCollisionBox(6.0, 0.0, 5.0, 10.0, 2.0, 11.0);
                }

                return powered ? new HexCollisionBox(5.0, 0.0, 6.0, 11.0, 1.0, 10.0) : new HexCollisionBox(5.0, 0.0, 6.0, 11.0, 2.0, 10.0);
            default:
                throw new RuntimeException("Impossible Hitbox State");
        }
    }, BlockTags.BUTTONS.getStates().toArray(new StateType[0])),

    WALL(new DynamicHitboxWall(), BlockTags.WALLS.getStates().toArray(new StateType[0])),

    WALL_SIGN((player, item, version, data, x, y, z) -> {
        switch (data.getFacing()) {
            case NORTH:
                return new HexCollisionBox(0.0, 4.5, 14.0, 16.0, 12.5, 16.0);
            case SOUTH:
                return new HexCollisionBox(0.0, 4.5, 0.0, 16.0, 12.5, 2.0);
            case EAST:
                return new HexCollisionBox(0.0, 4.5, 0.0, 2.0, 12.5, 16.0);
            case WEST:
                return new HexCollisionBox(14.0, 4.5, 0.0, 16.0, 12.5, 16.0);
            default:
                return NoCollisionBox.INSTANCE;
        }
    }, BlockTags.WALL_SIGNS.getStates().toArray(new StateType[0])),

    WALL_HANGING_SIGN((player, item, version, data, x, y, z) -> {
        switch (data.getFacing()) {
            case NORTH:
            case SOUTH:
                return new ComplexCollisionBox(new HexCollisionBox(0.0D, 14.0D, 6.0D, 16.0D, 16.0D, 10.0D),
                        new HexCollisionBox(1.0D, 0.0D, 7.0D, 15.0D, 10.0D, 9.0D));
            default:
                return new ComplexCollisionBox(new HexCollisionBox(6.0D, 14.0D, 0.0D, 10.0D, 16.0D, 16.0D),
                        new HexCollisionBox(7.0D, 0.0D, 1.0D, 9.0D, 10.0D, 15.0D));
        }
    }, BlockTags.WALL_HANGING_SIGNS.getStates().toArray(new StateType[0])),

    STANDING_SIGN((player, item, version, data, x, y, z) ->
            new HexCollisionBox(4.0, 0.0, 4.0, 12.0, 16.0, 12.0),
            BlockTags.STANDING_SIGNS.getStates().toArray(new StateType[0])),

    SAPLING(new HexCollisionBox(2.0D, 0.0D, 2.0D, 14.0D, 12.0D, 14.0D),
            BlockTags.SAPLINGS.getStates().toArray(new StateType[0])),

    ROOTS(new HexCollisionBox(2.0D, 0.0D, 2.0D, 14.0D, 13.0D, 14.0D),
            StateTypes.WARPED_ROOTS, StateTypes.CRIMSON_ROOTS),

    BANNER(new HexCollisionBox(4.0D, 0.0D, 4.0D, 12.0D, 16.0D, 12.0D),
            StateTypes.WHITE_BANNER, StateTypes.ORANGE_BANNER, StateTypes.MAGENTA_BANNER, StateTypes.LIGHT_BLUE_BANNER,
            StateTypes.YELLOW_BANNER, StateTypes.LIME_BANNER, StateTypes.PINK_BANNER, StateTypes.GRAY_BANNER,
            StateTypes.LIGHT_GRAY_BANNER, StateTypes.CYAN_BANNER, StateTypes.PURPLE_BANNER, StateTypes.BLUE_BANNER,
            StateTypes.BROWN_BANNER, StateTypes.GREEN_BANNER, StateTypes.RED_BANNER, StateTypes.BLACK_BANNER),

    WALL_BANNER((player, item, version, data, x, y, z) -> {
        // ViaVersion replacement block
        if (version.isOlderThan(ClientVersion.V_1_8)) {
            return WALL_SIGN.dynamic.fetch(player, item, version, data, x, y, z);
        }

        switch (data.getFacing()) {
            case NORTH:
                return new HexCollisionBox(0.0, 0.0, 14.0, 16.0, 12.5, 16.0);
            case EAST:
                return new HexCollisionBox(0.0, 0.0, 0.0, 2.0, 12.5, 16.0);
            case WEST:
                return new HexCollisionBox(14.0, 0.0, 0.0, 16.0, 12.5, 16.0);
            case SOUTH:
                return new HexCollisionBox(0.0, 0.0, 0.0, 16.0, 12.5, 2.0);
            default:
                throw new RuntimeException("Impossible Banner Facing State; Something very wrong is going on");
        }
    }, StateTypes.WHITE_WALL_BANNER, StateTypes.ORANGE_WALL_BANNER, StateTypes.MAGENTA_WALL_BANNER,
            StateTypes.LIGHT_BLUE_WALL_BANNER, StateTypes.YELLOW_WALL_BANNER, StateTypes.LIME_WALL_BANNER,
            StateTypes.PINK_WALL_BANNER, StateTypes.GRAY_WALL_BANNER, StateTypes.LIGHT_GRAY_WALL_BANNER,
            StateTypes.CYAN_WALL_BANNER, StateTypes.PURPLE_WALL_BANNER, StateTypes.BLUE_WALL_BANNER,
            StateTypes.BROWN_WALL_BANNER, StateTypes.GREEN_WALL_BANNER, StateTypes.RED_WALL_BANNER, StateTypes.BLACK_WALL_BANNER),

    BREWING_STAND((player, item, version, block, x, y, z) -> {
        // BEWARE OF https://bugs.mojang.com/browse/MC-85109 FOR 1.8 PLAYERS
        if (version.isOlderThan(ClientVersion.V_1_13)) {
            return new SimpleCollisionBox(0.0F, 0.0F, 0.0F, 1.0F, 0.125F, 1.0F);
        } else {
            return new ComplexCollisionBox(
                    new HexCollisionBox(1.0, 0.0, 1.0, 15.0, 2.0, 15.0),
                    new SimpleCollisionBox(0.4375, 0.0, 0.4375, 0.5625, 0.875, 0.5625, false));
        }
    }, StateTypes.BREWING_STAND),

    SMALL_FLOWER((player, item, version, data, x, y, z) ->  player.getClientVersion().isOlderThan(ClientVersion.V_1_13)
            ? new SimpleCollisionBox(0.3125D, 0.0D, 0.3125D, 0.6875D, 0.625D, 0.6875D)
            : new OffsetCollisionBox(data.getType(), 0.3125D, 0.0D, 0.3125D, 0.6875D, 0.625D, 0.6875D),
            BlockTags.SMALL_FLOWERS.getStates().toArray(new StateType[0])),

    TALL_FLOWERS((player, item, version, data, x, y, z) -> new SimpleCollisionBox(0, 0, 0, 1, 1, 1, true),
            BlockTags.TALL_FLOWERS.getStates().toArray(new StateType[0])),

    FIRE((player, item, version, data, x, y, z) -> {
        // Since 1.16 fire has a small hitbox
        if (version.isNewerThanOrEquals(ClientVersion.V_1_16)) {
            return new HexCollisionBox(0.0D, 0.0D, 0.0D, 16.0D, 1.0D, 16.0D);
        }
        return NoCollisionBox.INSTANCE;
    }, BlockTags.FIRE.getStates().toArray(new StateType[0])),

    HONEY_BLOCK(new SimpleCollisionBox(0, 0, 0, 1, 1, 1, true), StateTypes.HONEY_BLOCK),

    POWDER_SNOW(new SimpleCollisionBox(0, 0, 0, 1, 1, 1, true), StateTypes.POWDER_SNOW),

    SOUL_SAND(new SimpleCollisionBox(0, 0, 0, 1, 1, 1, true), StateTypes.SOUL_SAND),

    CACTUS((player, item, version, data, x, y, z) -> {
        if (version.isOlderThan(ClientVersion.V_1_13)) {
            // https://bugs.mojang.com/browse/MC-59610
            return new SimpleCollisionBox(0, 0, 0, 1, 1, 1, true);
        }
        return new HexCollisionBox(1.0D, 0.0D, 1.0D, 15.0D, 16.0D, 15.0D);
    }, StateTypes.CACTUS),

    SNOW((player, item, version, data, x, y, z) -> {
        return new SimpleCollisionBox(0, 0, 0, 1, data.getLayers() * 0.125, 1);
    }, StateTypes.SNOW),

    LECTERN_BLOCK((player, item, version, data, x, y, z) -> {
        ComplexCollisionBox common = new ComplexCollisionBox(new HexCollisionBox(0.0D, 0.0D, 0.0D, 16.0D, 2.0D, 16.0D),
                new HexCollisionBox(4.0D, 2.0D, 4.0D, 12.0D, 14.0D, 12.0D));

        if (data.getFacing() == BlockFace.WEST) {
            common.add(new HexCollisionBox(1.0D, 10.0D, 0.0D, 5.333333D, 14.0D, 16.0D));
            common.add(new HexCollisionBox(5.333333D, 12.0D, 0.0D, 9.666667D, 16.0D, 16.0D));
            common.add(new HexCollisionBox(9.666667D, 14.0D, 0.0D, 14.0D, 18.0D, 16.0D));
        } else if (data.getFacing() == BlockFace.NORTH) {
            common.add(new HexCollisionBox(0.0D, 10.0D, 1.0D, 16.0D, 14.0D, 5.333333D));
            common.add(new HexCollisionBox(0.0D, 12.0D, 5.333333D, 16.0D, 16.0D, 9.666667D));
            common.add(new HexCollisionBox(0.0D, 14.0D, 9.666667D, 16.0D, 18.0D, 14.0D));
        } else if (data.getFacing() == BlockFace.EAST) {
            common.add(new HexCollisionBox(10.666667D, 10.0D, 0.0D, 15.0D, 14.0D, 16.0D));
            common.add(new HexCollisionBox(6.333333D, 12.0D, 0.0D, 10.666667D, 16.0D, 16.0D));
            common.add(new HexCollisionBox(2.0D, 14.0D, 0.0D, 6.333333D, 18.0D, 16.0D));
        } else { // SOUTH
            common.add(new HexCollisionBox(0.0D, 10.0D, 10.666667D, 16.0D, 14.0D, 15.0D));
            common.add(new HexCollisionBox(0.0D, 12.0D, 6.333333D, 16.0D, 16.0D, 10.666667D));
            common.add(new HexCollisionBox(0.0D, 14.0D, 2.0D, 16.0D, 18.0D, 6.333333D));
        }

        return common;
    }, StateTypes.LECTERN),

    PITCHER_CROP((player, item, version, data, x, y, z) -> {
        final SimpleCollisionBox FULL_UPPER_SHAPE = new HexCollisionBox(3.0D, 0.0D, 3.0D, 13.0D, 15.0D, 13.0D);
        final SimpleCollisionBox FULL_LOWER_SHAPE = new HexCollisionBox(3.0D, -1.0D, 3.0D, 13.0D, 16.0D, 13.0D);
        final SimpleCollisionBox COLLISION_SHAPE_BULB = new HexCollisionBox(5.0D, -1.0D, 5.0D, 11.0D, 3.0D, 11.0D);
        final SimpleCollisionBox COLLISION_SHAPE_CROP = new HexCollisionBox(3.0D, -1.0D, 3.0D, 13.0D, 5.0D, 13.0D);
        final SimpleCollisionBox[] UPPER_SHAPE_BY_AGE = new SimpleCollisionBox[]{new HexCollisionBox(3.0D, 0.0D, 3.0D, 13.0D, 11.0D, 13.0D), FULL_UPPER_SHAPE};
        final SimpleCollisionBox[] LOWER_SHAPE_BY_AGE = new SimpleCollisionBox[]{COLLISION_SHAPE_BULB, new HexCollisionBox(3.0D, -1.0D, 3.0D, 13.0D, 14.0D, 13.0D), FULL_LOWER_SHAPE, FULL_LOWER_SHAPE, FULL_LOWER_SHAPE};

        return data.getHalf() == Half.UPPER ? UPPER_SHAPE_BY_AGE[Math.min(Math.abs(4 - (data.getAge() + 1)), UPPER_SHAPE_BY_AGE.length - 1)] : LOWER_SHAPE_BY_AGE[data.getAge()];
    }, StateTypes.PITCHER_CROP),

    WHEAT_BEETROOTS((player, item, version, data, x, y, z) -> {
        return new HexCollisionBox(0.0D, 0.0D, 0.0D, 16.0D, (data.getAge() + 1) * 2, 16.0D);
    }, StateTypes.WHEAT, StateTypes.BEETROOTS),

    CARROT_POTATOES((player, item, version, data, x, y, z) -> {
        return new HexCollisionBox(0.0D, 0.0D, 0.0D, 16.0D, data.getAge() + 2, 16.0D);
    }, StateTypes.CARROTS, StateTypes.POTATOES),

    NETHER_WART((player, item, version, data, x, y, z) -> {
        return new HexCollisionBox(0.0D, 0.0D, 0.0D, 16.0, 5 + (data.getAge() * 3), 16.0D);
    }, StateTypes.NETHER_WART),

    ATTACHED_PUMPKIN_STEM((player, item, version, data, x, y, z) -> {
        if (version.isOlderThan(ClientVersion.V_1_13))
            return new HexCollisionBox(7.0D, 0.0D, 7.0D, 9.0D, 16.0D, 9.0D);

        switch (data.getFacing()) {
            case SOUTH:
                return new HexCollisionBox(6.0D, 0.0D, 6.0D, 10.0D, 10.0D, 16.0D);
            case WEST:
                return new HexCollisionBox(0.0D, 0.0D, 6.0D, 10.0D, 10.0D, 10.0D);
            case NORTH:
                return new HexCollisionBox(6.0D, 0.0D, 0.0D, 10.0D, 10.0D, 10.0D);
            case EAST:
            default:
                return new HexCollisionBox(6.0D, 0.0D, 6.0D, 16.0D, 10.0D, 10.0D);
        }
    }, StateTypes.ATTACHED_MELON_STEM, StateTypes.ATTACHED_PUMPKIN_STEM),

    PUMPKIN_STEM((player, item, version, data, x, y, z) -> {
        return new HexCollisionBox(7, 0, 7, 9, 2 * (data.getAge() + 1), 9);
    }, StateTypes.PUMPKIN_STEM, StateTypes.MELON_STEM),

    // Hitbox/Outline is Same as Collision
    COCOA_BEANS((player, item, version, data, x, y, z) -> {
        return CollisionData.getCocoa(version, data.getAge(), data.getFacing());
    }, StateTypes.COCOA),


    REDSTONE_WIRE((player, item, version, data, x, y, z) ->
            // Easier to just use no collision box
            // Redstone wire is very complex with its collision shapes and has many de-syncs
            NoCollisionBox.INSTANCE,
            StateTypes.REDSTONE_WIRE),

    SWEET_BERRY((player, item, version, data, x, y, z) -> {
        if (data.getAge() == 0) {
            return new HexCollisionBox(3.0D, 0.0D, 3.0D, 13.0D, 8.0D, 13.0D);
        } else if (data.getAge() < 3) {
            return new HexCollisionBox(1.0D, 0.0D, 1.0D, 15.0D, 16.0D, 15.0D);
        }
        return new SimpleCollisionBox(0, 0, 0, 1, 1, 1, true);
    }, StateTypes.SWEET_BERRY_BUSH),

    PINK_PETALS_BLOCK(new HexCollisionBox(0.0D, 0.0D, 0.0D, 16.0D, 3.0D, 16.0D), StateTypes.PINK_PETALS),

    TORCHFLOWER_CROP((player, item, version, data, x, y, z) -> {
        if (data.getAge() == 0) {
            return new HexCollisionBox(5.0D, 0.0D, 5.0D, 11.0D, 6.0D, 11.0D);
        }
        // age is 1
        return new HexCollisionBox(5.0D, 0.0D, 5.0D, 11.0D, 10.0D, 11.0D);
    }, StateTypes.TORCHFLOWER_CROP),

    DEAD_BUSH(new HexCollisionBox(2.0D, 0.0D, 2.0D, 14.0D, 13.0D, 14.0D), StateTypes.DEAD_BUSH),

    SUGARCANE(new HexCollisionBox(2.0D, 0.0D, 2.0D, 14.0D, 16.0D, 14.0D), StateTypes.SUGAR_CANE),

    NETHER_SPROUTS(new HexCollisionBox(2.0D, 0.0D, 2.0D, 14.0D, 3.0D, 14.0D), StateTypes.NETHER_SPROUTS),

    HANGING_ROOTS(new HexCollisionBox(2.0D, 10.0D, 2.0D, 14.0D, 16.0D, 14.0D), StateTypes.HANGING_ROOTS),

    GRASS_FERN((player, item, version, data, x ,y, z) -> {
        if (version.isOlderThan(ClientVersion.V_1_13)) {
            float var1 = 0.4F;
            return new SimpleCollisionBox(0.5F - var1, 0.0F, 0.5F - var1, 0.5F + var1, 0.8F, 0.5F + var1);
        }
        return new HexCollisionBox(2.0D, 0.0D, 2.0D, 14.0D, 13.0D, 14.0D);
    }, StateTypes.SHORT_GRASS, StateTypes.FERN),

    SEA_GRASS(new HexCollisionBox(2.0D, 0.0D, 2.0D, 14.0D, 12.0D, 14.0D),
            StateTypes.SEAGRASS),

    TALL_SEAGRASS(new HexCollisionBox(2.0, 0.0, 2.0, 14.0, 16.0, 14.0),
            StateTypes.TALL_SEAGRASS),

    SMALL_DRIPLEAF(new HexCollisionBox(2.0, 0.0, 2.0, 14.0, 13.0, 14.0), StateTypes.SMALL_DRIPLEAF),

    CAVE_VINES(new HexCollisionBox(1.0D, 0.0D, 1.0D, 15.0D, 16.0D, 15.0D), StateTypes.CAVE_VINES, StateTypes.CAVE_VINES_PLANT),

    TWISTING_VINES_BLOCK(new HexCollisionBox(4.0D, 0.0D, 4.0D, 12.0D, 15.0D, 12.0D), StateTypes.TWISTING_VINES, StateTypes.WEEPING_VINES),

    TWISTING_VINES(new HexCollisionBox(4.0D, 0.0D, 4.0D, 12.0D, 16.0D, 12.0D), StateTypes.TWISTING_VINES_PLANT, StateTypes.WEEPING_VINES_PLANT),

    TALL_PLANT(new SimpleCollisionBox(0, 0, 0, 1, 1, 1, true), StateTypes.TALL_GRASS, StateTypes.LARGE_FERN),

    BAMBOO((player, item, version, data, x, y, z) -> data.getLeaves() == Leaves.LARGE
            ? new HexOffsetCollisionBox(data.getType(), 3.0, 0.0, 3.0, 13.0, 16.0, 13.0)
            : new HexOffsetCollisionBox(data.getType(), 5.0, 0.0, 5.0, 11.0, 16.0, 11.0), StateTypes.BAMBOO),

    BAMBOO_SAPLING((player, item, version, data, x, y, z) -> {
        return new HexOffsetCollisionBox(data.getType(), 4.0D, 0.0D, 4.0D, 12.0D, 12.0D, 12.0D);
    }, StateTypes.BAMBOO_SAPLING),

    SCAFFOLDING((player, item, version, data, x, y, z) -> {
        // If is holding scaffolding
        if (item == StateTypes.SCAFFOLDING) {
            return new SimpleCollisionBox(0, 0, 0, 1, 1, 1, true);
        }

        // STABLE_SHAPE for the scaffolding
        ComplexCollisionBox box = new ComplexCollisionBox(
                new HexCollisionBox(0.0D, 14.0D, 0.0D, 16.0D, 16.0D, 16.0D),
                new HexCollisionBox(0.0D, 0.0D, 0.0D, 2.0D, 16.0D, 2.0D),
                new HexCollisionBox(14.0D, 0.0D, 0.0D, 16.0D, 16.0D, 2.0D),
                new HexCollisionBox(0.0D, 0.0D, 14.0D, 2.0D, 16.0D, 16.0D),
                new HexCollisionBox(14.0D, 0.0D, 14.0D, 16.0D, 16.0D, 16.0D));

        if (data.getHalf() == Half.LOWER) { // Add the unstable shape to the collision boxes
            box.add(new HexCollisionBox(0.0D, 0.0D, 0.0D, 2.0D, 2.0D, 16.0D));
            box.add(new HexCollisionBox(14.0D, 0.0D, 0.0D, 16.0D, 2.0D, 16.0D));
            box.add(new HexCollisionBox(0.0D, 0.0D, 14.0D, 16.0D, 2.0D, 16.0D));
            box.add(new HexCollisionBox(0.0D, 0.0D, 0.0D, 16.0D, 2.0D, 2.0D));
        }

        return box;
    }, StateTypes.SCAFFOLDING),

    DRIPLEAF((player, item, version, data, x, y, z) -> {
        if (version.isOlderThanOrEquals(ClientVersion.V_1_16_4))
            return new SimpleCollisionBox(0, 0, 0, 1, 1, 1, true);

        ComplexCollisionBox box = new ComplexCollisionBox();

        if (data.getFacing() == BlockFace.NORTH) { // Stem
            box.add(new HexCollisionBox(5.0D, 0.0D, 9.0D, 11.0D, 15.0D, 15.0D));
        } else if (data.getFacing() == BlockFace.SOUTH) {
            box.add(new HexCollisionBox(5.0D, 0.0D, 1.0D, 11.0D, 15.0D, 7.0D));
        } else if (data.getFacing() == BlockFace.EAST) {
            box.add(new HexCollisionBox(1.0D, 0.0D, 5.0D, 7.0D, 15.0D, 11.0D));
        } else {
            box.add(new HexCollisionBox(9.0D, 0.0D, 5.0D, 15.0D, 15.0D, 11.0D));
        }

        if (data.getTilt() == Tilt.NONE || data.getTilt() == Tilt.UNSTABLE) {
            box.add(new HexCollisionBox(0.0, 11.0, 0.0, 16.0, 15.0, 16.0));
        } else if (data.getTilt() == Tilt.PARTIAL) {
            box.add(new HexCollisionBox(0.0, 11.0, 0.0, 16.0, 13.0, 16.0));
        }

        return box;

    }, StateTypes.BIG_DRIPLEAF),

    MANGROVE_PROPAGULE(((player, item, version, data, x, y, z) -> {
        if (data.isHanging()) {
            return new HexOffsetCollisionBox(data.getType(), 7.0, 0.0, 7.0, 9.0, 16.0, 9.0);
        } else {
            return new HexOffsetCollisionBox(data.getType(),7.0, getPropaguleMinHeight(data.getAge()), 7.0, 9.0, 16.0, 9.0);
        }
    }), StateTypes.MANGROVE_PROPAGULE);


    private static final Map<StateType, HitboxData> lookup = new HashMap<>();

    static {
        for (HitboxData data : HitboxData.values()) {
            for (StateType type : data.materials) {
                lookup.put(type, data);
            }
        }
    }

    private final StateType[] materials;
    private CollisionBox box;
    private HitBoxFactory dynamic;

    HitboxData(CollisionBox box, StateType... materials) {
        this.box = box;
        Set<StateType> mList = new HashSet<>(Arrays.asList(materials));
        mList.remove(null); // Sets can contain one null
        this.materials = mList.toArray(new StateType[0]);
    }

    HitboxData(HitBoxFactory dynamic, StateType... materials) {
        this.dynamic = dynamic;
        Set<StateType> mList = new HashSet<>(Arrays.asList(materials));
        mList.remove(null); // Sets can contain one null
        this.materials = mList.toArray(new StateType[0]);
    }

    public static HitboxData getData(StateType material) {
        return lookup.get(material);
    }

    public static CollisionBox getBlockHitbox(GrimPlayer player, StateType heldItem, ClientVersion version, WrappedBlockState block, int x, int y, int z) {
        HitboxData data = getData(block.getType());

        if (data == null) {
            // Fall back to collision boxes
            return CollisionData.getRawData(block.getType()).getMovementCollisionBox(player, version, block, x, y, z);
        }

        // Simple collision box to override
        if (data.box != null)
            return data.box.copy().offset(x, y, z);

        // Allow this class to override collision boxes when they aren't the same as regular boxes
        HitBoxFactory hitBoxFactory = data.dynamic;
        CollisionBox collisionBox = hitBoxFactory.fetch(player, heldItem, version, block, x, y, z);
        collisionBox.offset(x, y, z);
        return collisionBox;
    }

    private static int getPropaguleMinHeight(int age) {
        switch (age) {
            case 0:
            case 1:
            case 2:
                return 13 - age * 3;
            case 3:
            case 4:
                return (4 - age) * 3;
        }
        throw new RuntimeException("Impossible State");
    }
}
