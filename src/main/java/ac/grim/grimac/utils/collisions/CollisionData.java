package ac.grim.grimac.utils.collisions;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.predictionengine.movementtick.MovementTickerStrider;
import ac.grim.grimac.utils.blockdata.WrappedBlockData;
import ac.grim.grimac.utils.blockdata.types.*;
import ac.grim.grimac.utils.blockstate.BaseBlockState;
import ac.grim.grimac.utils.collisions.blocks.*;
import ac.grim.grimac.utils.collisions.blocks.connecting.DynamicFence;
import ac.grim.grimac.utils.collisions.blocks.connecting.DynamicPane;
import ac.grim.grimac.utils.collisions.blocks.connecting.DynamicWall;
import ac.grim.grimac.utils.collisions.datatypes.*;
import ac.grim.grimac.utils.data.packetentity.PacketEntityStrider;
import ac.grim.grimac.utils.math.GrimMath;
import ac.grim.grimac.utils.nmsutil.Materials;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import org.bukkit.Axis;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.FaceAttachable;
import org.bukkit.block.data.Levelled;
import org.bukkit.block.data.type.*;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

// Warning for major game updates!
// Do not use an enum for stuff like Axis and other data types not in 1.7
// Meaning only stuff like getDirection() should have enums
//
// An enum will break support for all previous versions which is very bad
// An if statement for new data types is perfectly safe and should be used instead
public enum CollisionData {
    VINE((player, version, block, x, y, z) -> {
        ComplexCollisionBox boxes = new ComplexCollisionBox();

        Set<BlockFace> directions = ((WrappedMultipleFacing) block).getDirections();

        if (directions.contains(BlockFace.UP))
            boxes.add(new HexCollisionBox(0.0D, 15.0D, 0.0D, 16.0D, 16.0D, 16.0D));

        if (directions.contains(BlockFace.WEST))
            boxes.add(new HexCollisionBox(0.0D, 0.0D, 0.0D, 1.0D, 16.0D, 16.0D));

        if (directions.contains(BlockFace.EAST))
            boxes.add(new HexCollisionBox(15.0D, 0.0D, 0.0D, 16.0D, 16.0D, 16.0D));

        if (directions.contains(BlockFace.NORTH))
            boxes.add(new HexCollisionBox(0.0D, 0.0D, 0.0D, 16.0D, 16.0D, 1.0D));

        if (directions.contains(BlockFace.SOUTH))
            boxes.add(new HexCollisionBox(0.0D, 0.0D, 15.0D, 16.0D, 16.0D, 16.0D));

        return boxes;

    }, ItemTypes.VINE),

    LAVA((player, version, block, x, y, z) -> {
        if (MovementTickerStrider.isAbove(player) && player.playerVehicle instanceof PacketEntityStrider) {
            Levelled lava = (Levelled) ((WrappedFlatBlock) block).getBlockData();
            if (lava.getLevel() == 0) {
                return new HexCollisionBox(0.0D, 0.0D, 0.0D, 16.0D, 8.0D, 16.0D);
            }
        }

        return NoCollisionBox.INSTANCE;
    }, ItemTypes.LAVA),

    WATER(NoCollisionBox.INSTANCE, ItemTypes.WATER, ItemTypes.STATIONARY_WATER,
            ItemTypes.STATIONARY_WATER),

    BREWINGSTAND((player, version, block, x, y, z) -> {
        int base = 0;

        if (version.isNewerThanOrEquals(ClientVersion.V_1_13))
            base = 1;

        return new ComplexCollisionBox(
                new HexCollisionBox(base, 0, base, 16 - base, 2, 16 - base),
                new SimpleCollisionBox(0.4375, 0.0, 0.4375, 0.5625, 0.875, 0.5625, false));

    }, ItemTypes.BREWING_STAND),

    BAMBOO((player, version, block, x, y, z) -> {
        // ViaVersion replacement block - sugarcane
        if (version.isOlderThan(ClientVersion.V_1_13_2))
            return NoCollisionBox.INSTANCE;

        // Offset taken from NMS
        long i = (x * 3129871L) ^ (long) z * 116129781L ^ (long) 0;
        i = i * i * 42317861L + i * 11L;
        i = i >> 16;

        return new HexCollisionBox(6.5D, 0.0D, 6.5D, 9.5D, 16.0D, 9.5D).offset((((i & 15L) / 15.0F) - 0.5D) * 0.5D, 0, (((i >> 8 & 15L) / 15.0F) - 0.5D) * 0.5D);
    }, ItemTypes.BAMBOO),


    BAMBOO_SAPLING((player, version, block, x, y, z) -> {
        long i = (x * 3129871L) ^ (long) z * 116129781L ^ (long) 0;
        i = i * i * 42317861L + i * 11L;
        i = i >> 16;

        return new HexCollisionBox(4.0D, 0.0D, 4.0D, 12.0D, 12.0D, 12.0D).offset((((i & 15L) / 15.0F) - 0.5D) * 0.5D, 0, (((i >> 8 & 15L) / 15.0F) - 0.5D) * 0.5D);
    }, ItemTypes.BAMBOO_SAPLING),

    COMPOSTER((player, version, block, x, y, z) -> {
        double height = 0.125;

        if (version.isOlderThanOrEquals(ClientVersion.V_1_13_2))
            height = 0.25;

        if (version.isOlderThanOrEquals(ClientVersion.V_1_12_2))
            height = 0.3125;

        return new ComplexCollisionBox(
                new SimpleCollisionBox(0, 0, 0, 1, height, 1, false),
                new SimpleCollisionBox(0, height, 0, 0.125, 1, 1, false),
                new SimpleCollisionBox(1 - 0.125, height, 0, 1, 1, 1, false),
                new SimpleCollisionBox(0, height, 0, 1, 1, 0.125, false),
                new SimpleCollisionBox(0, height, 1 - 0.125, 1, 1, 1, false));
    }, ItemTypes.COMPOSTER),

    RAIL(new SimpleCollisionBox(0, 0, 0, 1, 0.125, 0, false),
            ItemTypes.RAIL, ItemTypes.ACTIVATOR_RAIL,
            ItemTypes.DETECTOR_RAIL, ItemTypes.POWERED_RAIL),

    ANVIL((player, version, data, x, y, z) -> {
        BlockFace face = ((WrappedDirectional) data).getDirection();

        // Anvil collision box was changed in 1.13 to be more accurate
        // https://www.mcpk.wiki/wiki/Version_Differences
        // The base is 0.75×0.75, and its floor is 0.25b high.
        // The top is 1×0.625, and its ceiling is 0.375b low.
        if (version.isNewerThanOrEquals(ClientVersion.V_1_13)) {
            ComplexCollisionBox complexAnvil = new ComplexCollisionBox();
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
    }, ItemTypes.ANVIL, ItemTypes.CHIPPED_ANVIL, ItemTypes.DAMAGED_ANVIL),

    WALL(new DynamicWall(), Arrays.stream(Material.values()).filter(mat -> mat.name().contains("WALL")
                    && !mat.name().contains("SIGN") && !mat.name().contains("HEAD") && !mat.name().contains("BANNER")
                    && !mat.name().contains("FAN") && !mat.name().contains("SKULL") && !mat.name().contains("TORCH"))
            .toArray(Material[]::new)),

    SLAB((player, version, data, x, y, z) -> {
        if (((WrappedSlab) data).isDouble()) {
            return new SimpleCollisionBox(0, 0, 0, 1, 1, 1, true);
        } else if (((WrappedSlab) data).isBottom()) {
            return new SimpleCollisionBox(0, 0, 0, 1, 0.5, 1, false);
        }

        return new SimpleCollisionBox(0, 0.5, 0, 1, 1, 1, false);
        // 1.13 can handle double slabs as it's in the block data
        // 1.12 has double slabs as a separate block, no block data to differentiate it
    }, Arrays.stream(Material.values()).filter(mat -> (mat.name().contains("_SLAB") || mat.name().contains("STEP"))
            && !mat.name().contains("DOUBLE")).toArray(Material[]::new)),

    // Overwrite previous SKULL enum for legacy, where head and wall skull isn't separate
    WALL_SKULL((player, version, data, x, y, z) -> {
        switch (((WrappedDirectional) data).getDirection()) {
            case DOWN:
            default: // On the floor
                return new SimpleCollisionBox(0.25F, 0.0F, 0.25F, 0.75F, 0.5F, 0.75F, false);
            case NORTH:
                return new SimpleCollisionBox(0.25F, 0.25F, 0.5F, 0.75F, 0.75F, 1.0F, false);
            case SOUTH:
                return new SimpleCollisionBox(0.25F, 0.25F, 0.0F, 0.75F, 0.75F, 0.5F, false);
            case WEST:
                return new SimpleCollisionBox(0.5F, 0.25F, 0.25F, 1.0F, 0.75F, 0.75F, false);
            case EAST:
                return new SimpleCollisionBox(0.0F, 0.25F, 0.25F, 0.5F, 0.75F, 0.75F, false);
        }
    }, Arrays.stream(Material.values()).filter(mat -> (mat.name().contains("HEAD") || mat.name().contains("SKULL")) && !mat.name().contains("PISTON")).toArray(Material[]::new)),

    BANNER(new HexCollisionBox(4.0D, 0.0D, 4.0D, 12.0D, 16.0D, 12.0D),
            Arrays.stream(Material.values()).filter(mat -> mat.name().contains("BANNER")).toArray(Material[]::new)),

    CORAL_FAN((player, version, data, x, y, z) -> {
        return new HexCollisionBox(2.0D, 0.0D, 2.0D, 14.0D, 4.0D, 14.0D);
    }, Arrays.stream(Material.values()).filter(mat -> mat.name().contains("CORAL_FAN")).toArray(Material[]::new)),

    DOOR(new DoorHandler(), Arrays.stream(Material.values()).filter(mat -> mat.name().contains("_DOOR"))
            .toArray(Material[]::new)),

    HOPPER((player, version, data, x, y, z) -> {
        if (version.isNewerThanOrEquals(ClientVersion.V_1_13)) {
            ComplexCollisionBox hopperBox = new ComplexCollisionBox();

            switch (((WrappedDirectional) data).getDirection()) {
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

            return new ComplexCollisionBox(
                    new SimpleCollisionBox(0, 0, 0, 1, height, 1, false),
                    new SimpleCollisionBox(0, height, 0, 0.125, 1, 1, false),
                    new SimpleCollisionBox(1 - 0.125, height, 0, 1, 1, 1, false),
                    new SimpleCollisionBox(0, height, 0, 1, 1, 0.125, false),
                    new SimpleCollisionBox(0, height, 1 - 0.125, 1, 1, 1, false));
        }

    }, ItemTypes.HOPPER),

    CAKE((player, version, data, x, y, z) -> {
        double height = 0.5;
        if (version.isOlderThan(ClientVersion.V_1_8))
            height = 0.4375;
        double eatenPosition = (1 + ((WrappedCake) data).getSlicesEaten() * 2) / 16D;
        return new SimpleCollisionBox(eatenPosition, 0, 0.0625, 1 - 0.0625, height, 1 - 0.0625, false);
    }, ItemTypes.CAKE),

    COCOA_BEANS((player, version, data, x, y, z) -> {
        WrappedCocoaBeans beans = (WrappedCocoaBeans) data;
        return getCocoa(version, beans.getAge(), beans.getDirection());
    }, ItemTypes.COCOA),

    STONE_CUTTER((player, version, data, x, y, z) -> {
        if (version.isOlderThanOrEquals(ClientVersion.V_1_13_2))
            return new SimpleCollisionBox(0, 0, 0, 1, 1, 1, true);

        return new HexCollisionBox(0.0D, 0.0D, 0.0D, 16.0D, 9.0D, 16.0D);
    }, ItemTypes.STONECUTTER),

    SWEET_BERRY((player, version, data, x, y, z) -> {
        Ageable berry = (Ageable) ((WrappedFlatBlock) data).getBlockData();
        if (berry.getAge() == 0) {
            return new HexCollisionBox(3.0D, 0.0D, 3.0D, 13.0D, 8.0D, 13.0D);
        } else if (berry.getAge() < 3) {
            return new HexCollisionBox(1.0D, 0.0D, 1.0D, 15.0D, 16.0D, 15.0D);
        }
        return new SimpleCollisionBox(0, 0, 0, 1, 1, 1, true);
    }, ItemTypes.SWEET_BERRY_BUSH),

    SAPLING(new HexCollisionBox(2.0D, 0.0D, 2.0D, 14.0D, 12.0D, 14.0D),
            ItemTypes.SPRUCE_SAPLING, ItemTypes.ACACIA_SAPLING,
            ItemTypes.BIRCH_SAPLING, ItemTypes.DARK_OAK_SAPLING,
            ItemTypes.OAK_SAPLING, ItemTypes.JUNGLE_SAPLING),

    ROOTS(new HexCollisionBox(2.0D, 0.0D, 2.0D, 14.0D, 13.0D, 14.0D),
            ItemTypes.WARPED_ROOTS, ItemTypes.CRIMSON_ROOTS),

    FLOWER(new HexCollisionBox(5.0D, 0.0D, 5.0D, 11.0D, 10.0D, 11.0D),
            ItemTypes.DANDELION,
            ItemTypes.POPPY, ItemTypes.BLUE_ORCHID,
            ItemTypes.ALLIUM, ItemTypes.AZURE_BLUET,
            ItemTypes.RED_TULIP, ItemTypes.ORANGE_TULIP,
            ItemTypes.WHITE_TULIP, ItemTypes.PINK_TULIP,
            ItemTypes.OXEYE_DAISY, ItemTypes.CORNFLOWER,
            ItemTypes.LILY_OF_THE_VALLEY),

    DEAD_BUSH(new HexCollisionBox(2.0D, 0.0D, 2.0D, 14.0D, 13.0D, 14.0D), ItemTypes.DEAD_BUSH),

    SUGARCANE(new HexCollisionBox(2.0D, 0.0D, 2.0D, 14.0D, 16.0D, 14.0D), ItemTypes.SUGAR_CANE),

    NETHER_SPROUTS(new HexCollisionBox(2.0D, 0.0D, 2.0D, 14.0D, 3.0D, 14.0D), ItemTypes.NETHER_SPROUTS),

    GRASS_FERN(new HexCollisionBox(2.0D, 0.0D, 2.0D, 14.0D, 13.0D, 14.0D),
            ItemTypes.GRASS, ItemTypes.FERN),

    TALL_GRASS(new SimpleCollisionBox(0, 0, 0, 1, 1, 1, true), ItemTypes.TALL_GRASS),

    SEA_GRASS(new HexCollisionBox(2.0D, 0.0D, 2.0D, 14.0D, 12.0D, 14.0D),
            ItemTypes.SEAGRASS),

    BELL((player, version, data, x, y, z) -> {
        if (version.isOlderThanOrEquals(ClientVersion.V_1_13_2))
            return new SimpleCollisionBox(0, 0, 0, 1, 1, 1, true);

        Bell bell = (Bell) ((WrappedFlatBlock) data).getBlockData();
        org.bukkit.block.BlockFace direction = bell.getFacing();

        if (bell.getAttachment() == Bell.Attachment.FLOOR) {
            return direction != org.bukkit.block.BlockFace.NORTH && direction != org.bukkit.block.BlockFace.SOUTH ?
                    new HexCollisionBox(4.0D, 0.0D, 0.0D, 12.0D, 16.0D, 16.0D) :
                    new HexCollisionBox(0.0D, 0.0D, 4.0D, 16.0D, 16.0D, 12.0D);

        }

        ComplexCollisionBox complex = new ComplexCollisionBox(
                new HexCollisionBox(5.0D, 6.0D, 5.0D, 11.0D, 13.0D, 11.0D),
                new HexCollisionBox(4.0D, 4.0D, 4.0D, 12.0D, 6.0D, 12.0D));

        if (bell.getAttachment() == Bell.Attachment.CEILING) {
            complex.add(new HexCollisionBox(7.0D, 13.0D, 7.0D, 9.0D, 16.0D, 9.0D));
        } else if (bell.getAttachment() == Bell.Attachment.DOUBLE_WALL) {
            if (direction != org.bukkit.block.BlockFace.NORTH && direction != org.bukkit.block.BlockFace.SOUTH) {
                complex.add(new HexCollisionBox(0.0D, 13.0D, 7.0D, 16.0D, 15.0D, 9.0D));
            } else {
                complex.add(new HexCollisionBox(7.0D, 13.0D, 0.0D, 9.0D, 15.0D, 16.0D));
            }
        } else if (direction == org.bukkit.block.BlockFace.NORTH) {
            complex.add(new HexCollisionBox(7.0D, 13.0D, 0.0D, 9.0D, 15.0D, 13.0D));
        } else if (direction == org.bukkit.block.BlockFace.SOUTH) {
            complex.add(new HexCollisionBox(7.0D, 13.0D, 3.0D, 9.0D, 15.0D, 16.0D));
        } else {
            if (direction == org.bukkit.block.BlockFace.EAST) {
                complex.add(new HexCollisionBox(3.0D, 13.0D, 7.0D, 16.0D, 15.0D, 9.0D));
            } else {
                complex.add(new HexCollisionBox(0.0D, 13.0D, 7.0D, 13.0D, 15.0D, 9.0D));
            }
        }

        return complex;

    }, ItemTypes.BELL),

    SCAFFOLDING((player, version, data, x, y, z) -> {
        // ViaVersion replacement block - hay block
        if (version.isOlderThanOrEquals(ClientVersion.V_1_13_2))
            return new SimpleCollisionBox(0, 0, 0, 1, 1, 1, true);

        Scaffolding scaffolding = (Scaffolding) ((WrappedFlatBlock) data).getBlockData();

        if (player.lastY > y + 1 - 1.0E-5F && !player.isSneaking) {
            return new ComplexCollisionBox(new HexCollisionBox(0.0D, 14.0D, 0.0D, 16.0D, 16.0D, 16.0D),
                    new HexCollisionBox(0.0D, 0.0D, 0.0D, 2.0D, 16.0D, 2.0D),
                    new HexCollisionBox(14.0D, 0.0D, 0.0D, 16.0D, 16.0D, 2.0D),
                    new HexCollisionBox(0.0D, 0.0D, 14.0D, 2.0D, 16.0D, 16.0),
                    new HexCollisionBox(14.0D, 0.0D, 14.0D, 16.0D, 16.0D, 16.0D));
        }

        return scaffolding.getDistance() != 0 && scaffolding.isBottom() && player.lastY > y - (double) 1.0E-5F ?
                new HexCollisionBox(0.0D, 0.0D, 0.0D, 16.0D, 2.0D, 16.0D) :
                NoCollisionBox.INSTANCE;
    }, ItemTypes.SCAFFOLDING),

    LADDER((player, version, data, x, y, z) -> {
        int width = 3;
        if (version.isOlderThanOrEquals(ClientVersion.V_1_8))
            width = 2;

        switch (((WrappedDirectional) data).getDirection()) {
            case NORTH:
                return new HexCollisionBox(0.0D, 0.0D, 16.0D - width, 16.0D, 16.0D, 16.0D);
            case SOUTH:
                return new HexCollisionBox(0.0D, 0.0D, 0.0D, 16.0D, 16.0D, width);
            case WEST:
                return new HexCollisionBox(16.0D - width, 0.0D, 0.0D, 16.0D, 16.0D, 16.0D);
            default:
            case EAST:
                return new HexCollisionBox(0.0D, 0.0D, 0.0D, width, 16.0D, 16.0D);
        }
    }, ItemTypes.LADDER),

    CAMPFIRE((player, version, data, x, y, z) -> {
        // ViaVersion replacement block - slab if not lit or fire if lit
        if (version.isOlderThanOrEquals(ClientVersion.V_1_13_2)) {
            WrappedFlatBlock campfire = (WrappedFlatBlock) data;

            if (((Campfire) campfire.getBlockData()).isLit()) {
                return NoCollisionBox.INSTANCE;
            }

            return new HexCollisionBox(0, 0, 0, 16, 8, 16);
        }

        return new HexCollisionBox(0.0D, 0.0D, 0.0D, 16.0D, 7.0D, 16.0D);
    }, ItemTypes.CAMPFIRE, ItemTypes.SOUL_CAMPFIRE),

    LANTERN((player, version, data, x, y, z) -> {
        if (version.isOlderThanOrEquals(ClientVersion.V_1_12_2))
            return new SimpleCollisionBox(0, 0, 0, 1, 1, 1, true);

        WrappedFlatBlock lantern = (WrappedFlatBlock) data;

        if (((Lantern) lantern.getBlockData()).isHanging()) {
            return new ComplexCollisionBox(new HexCollisionBox(5.0D, 1.0D, 5.0D, 11.0D, 8.0D, 11.0D),
                    new HexCollisionBox(6.0D, 8.0D, 6.0D, 10.0D, 10.0D, 10.0D));
        }

        return new ComplexCollisionBox(new HexCollisionBox(5.0D, 0.0D, 5.0D, 11.0D, 7.0D, 11.0D),
                new HexCollisionBox(6.0D, 7.0D, 6.0D, 10.0D, 9.0D, 10.0D));

    }, ItemTypes.LANTERN, ItemTypes.SOUL_LANTERN),


    LECTERN((player, version, data, x, y, z) -> {
        if (version.isOlderThanOrEquals(ClientVersion.V_1_13_2))
            return new SimpleCollisionBox(0, 0, 0, 1, 1, 1, true);

        return new ComplexCollisionBox(
                new HexCollisionBox(0.0D, 0.0D, 0.0D, 16.0D, 2.0D, 16.0D), // base
                new HexCollisionBox(4.0D, 2.0D, 4.0D, 12.0D, 14.0D, 12.0D)); // post
    }, ItemTypes.LECTERN),


    HONEY_BLOCK((player, version, data, x, y, z) -> {
        if (version.isOlderThanOrEquals(ClientVersion.V_1_14_4))
            return new SimpleCollisionBox(0, 0, 0, 1, 1, 1, true);

        return new HexCollisionBox(1.0D, 0.0D, 1.0D, 15.0D, 15.0D, 15.0D); // post
    }, ItemTypes.HONEY_BLOCK),

    SPORE_BLOSSOM(new HexCollisionBox(2.0D, 13.0D, 2.0D, 14.0D, 16.0D, 14.0D), ItemTypes.SPORE_BLOSSOM),

    GLOW_LICHEN((player, version, data, x, y, z) -> {
        GlowLichen lichen = (GlowLichen) ((WrappedFlatBlock) data).getBlockData();

        ComplexCollisionBox box = new ComplexCollisionBox();
        for (org.bukkit.block.BlockFace face : lichen.getFaces()) {
            switch (face) {
                case UP:
                    box.add(new HexCollisionBox(0.0D, 15.0D, 0.0D, 16.0D, 16.0D, 16.0D));
                    break;
                case DOWN:
                    box.add(new HexCollisionBox(0.0D, 0.0D, 0.0D, 16.0D, 1.0D, 16.0D));
                    break;
                case WEST:
                    box.add(new HexCollisionBox(0.0D, 0.0D, 0.0D, 1.0D, 16.0D, 16.0D));
                    break;
                case EAST:
                    box.add(new HexCollisionBox(15.0D, 0.0D, 0.0D, 16.0D, 16.0D, 16.0D));
                    break;
                case NORTH:
                    box.add(new HexCollisionBox(0.0D, 0.0D, 0.0D, 16.0D, 16.0D, 1.0D));
                    break;
                case SOUTH:
                    box.add(new HexCollisionBox(0.0D, 0.0D, 15.0D, 16.0D, 16.0D, 16.0D));
                    break;
            }
        }

        return box;
    }, ItemTypes.GLOW_LICHEN),

    DRAGON_EGG_BLOCK(new HexCollisionBox(1.0D, 0.0D, 1.0D, 15.0D, 16.0D, 15.0D), ItemTypes.DRAGON_EGG),

    GRINDSTONE((player, version, data, x, y, z) -> {
        Grindstone grindstone = (Grindstone) ((WrappedFlatBlock) data).getBlockData();

        // ViaVersion replacement block - Anvil
        if (version.isOlderThanOrEquals(ClientVersion.V_1_12_2)) {
            // Just a single solid collision box with 1.12
            if (grindstone.getFacing() == org.bukkit.block.BlockFace.NORTH || grindstone.getFacing() == org.bukkit.block.BlockFace.SOUTH) {
                return new SimpleCollisionBox(0.125F, 0.0F, 0.0F, 0.875F, 1.0F, 1.0F, false);
            } else {
                return new SimpleCollisionBox(0.0F, 0.0F, 0.125F, 1.0F, 1.0F, 0.875F, false);
            }
        }

        if (version.isOlderThanOrEquals(ClientVersion.V_1_13_2)) {
            ComplexCollisionBox complexAnvil = new ComplexCollisionBox();
            // Base of the anvil
            complexAnvil.add(new HexCollisionBox(2, 0, 2, 14, 4, 14));

            if (grindstone.getFacing() == org.bukkit.block.BlockFace.NORTH || grindstone.getFacing() == org.bukkit.block.BlockFace.SOUTH) {
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

        if (grindstone.getAttachedFace() == FaceAttachable.AttachedFace.FLOOR) {
            if (grindstone.getFacing() == org.bukkit.block.BlockFace.NORTH || grindstone.getFacing() == org.bukkit.block.BlockFace.SOUTH) {
                return new ComplexCollisionBox(new HexCollisionBox(2.0D, 0.0D, 6.0D, 4.0D, 7.0D, 10.0D),
                        new HexCollisionBox(12.0D, 0.0D, 6.0D, 14.0D, 7.0D, 10.0D),
                        new HexCollisionBox(2.0D, 7.0D, 5.0D, 4.0D, 13.0D, 11.0D),
                        new HexCollisionBox(12.0D, 7.0D, 5.0D, 14.0D, 13.0D, 11.0D),
                        new HexCollisionBox(4.0D, 4.0D, 2.0D, 12.0D, 16.0D, 14.0D));
            } else {
                return new ComplexCollisionBox(new HexCollisionBox(6.0D, 0.0D, 2.0D, 10.0D, 7.0D, 4.0D),
                        new HexCollisionBox(6.0D, 0.0D, 12.0D, 10.0D, 7.0D, 14.0D),
                        new HexCollisionBox(5.0D, 7.0D, 2.0D, 11.0D, 13.0D, 4.0D),
                        new HexCollisionBox(5.0D, 7.0D, 12.0D, 11.0D, 13.0D, 14.0D),
                        new HexCollisionBox(2.0D, 4.0D, 4.0D, 14.0D, 16.0D, 12.0D));
            }
        } else if (grindstone.getAttachedFace() == FaceAttachable.AttachedFace.WALL) {
            switch (grindstone.getFacing()) {
                case NORTH:
                    return new ComplexCollisionBox(new HexCollisionBox(2.0D, 6.0D, 7.0D, 4.0D, 10.0D, 16.0D),
                            new HexCollisionBox(12.0D, 6.0D, 7.0D, 14.0D, 10.0D, 16.0D),
                            new HexCollisionBox(2.0D, 5.0D, 3.0D, 4.0D, 11.0D, 9.0D),
                            new HexCollisionBox(12.0D, 5.0D, 3.0D, 14.0D, 11.0D, 9.0D),
                            new HexCollisionBox(4.0D, 2.0D, 0.0D, 12.0D, 14.0D, 12.0D));
                case WEST:
                    return new ComplexCollisionBox(new HexCollisionBox(7.0D, 6.0D, 2.0D, 16.0D, 10.0D, 4.0D),
                            new HexCollisionBox(7.0D, 6.0D, 12.0D, 16.0D, 10.0D, 14.0D),
                            new HexCollisionBox(3.0D, 5.0D, 2.0D, 9.0D, 11.0D, 4.0D),
                            new HexCollisionBox(3.0D, 5.0D, 12.0D, 9.0D, 11.0D, 14.0D),
                            new HexCollisionBox(0.0D, 2.0D, 4.0D, 12.0D, 14.0D, 12.0D));
                case SOUTH:
                    return new ComplexCollisionBox(new HexCollisionBox(2.0D, 6.0D, 0.0D, 4.0D, 10.0D, 7.0D),
                            new HexCollisionBox(12.0D, 6.0D, 0.0D, 14.0D, 10.0D, 7.0D),
                            new HexCollisionBox(2.0D, 5.0D, 7.0D, 4.0D, 11.0D, 13.0D),
                            new HexCollisionBox(12.0D, 5.0D, 7.0D, 14.0D, 11.0D, 13.0D),
                            new HexCollisionBox(4.0D, 2.0D, 4.0D, 12.0D, 14.0D, 16.0D));
                case EAST:
                    return new ComplexCollisionBox(new HexCollisionBox(0.0D, 6.0D, 2.0D, 9.0D, 10.0D, 4.0D),
                            new HexCollisionBox(0.0D, 6.0D, 12.0D, 9.0D, 10.0D, 14.0D),
                            new HexCollisionBox(7.0D, 5.0D, 2.0D, 13.0D, 11.0D, 4.0D),
                            new HexCollisionBox(7.0D, 5.0D, 12.0D, 13.0D, 11.0D, 14.0D),
                            new HexCollisionBox(4.0D, 2.0D, 4.0D, 16.0D, 14.0D, 12.0D));
            }
        } else {
            if (grindstone.getFacing() == org.bukkit.block.BlockFace.NORTH || grindstone.getFacing() == org.bukkit.block.BlockFace.SOUTH) {
                return new ComplexCollisionBox(new HexCollisionBox(2.0D, 9.0D, 6.0D, 4.0D, 16.0D, 10.0D),
                        new HexCollisionBox(12.0D, 9.0D, 6.0D, 14.0D, 16.0D, 10.0D),
                        new HexCollisionBox(2.0D, 3.0D, 5.0D, 4.0D, 9.0D, 11.0D),
                        new HexCollisionBox(12.0D, 3.0D, 5.0D, 14.0D, 9.0D, 11.0D),
                        new HexCollisionBox(4.0D, 0.0D, 2.0D, 12.0D, 12.0D, 14.0D));
            } else {
                return new ComplexCollisionBox(new HexCollisionBox(6.0D, 9.0D, 2.0D, 10.0D, 16.0D, 4.0D),
                        new HexCollisionBox(6.0D, 9.0D, 12.0D, 10.0D, 16.0D, 14.0D),
                        new HexCollisionBox(5.0D, 3.0D, 2.0D, 11.0D, 9.0D, 4.0D),
                        new HexCollisionBox(5.0D, 3.0D, 12.0D, 11.0D, 9.0D, 14.0D),
                        new HexCollisionBox(2.0D, 0.0D, 4.0D, 14.0D, 12.0D, 12.0D));
            }
        }

        return NoCollisionBox.INSTANCE;

    }, ItemTypes.GRINDSTONE),

    CHAIN_BLOCK((player, version, data, x, y, z) -> {
        Chain chain = (Chain) ((WrappedFlatBlock) data).getBlockData();

        if (chain.getAxis() == Axis.X) {
            return new HexCollisionBox(0.0D, 6.5D, 6.5D, 16.0D, 9.5D, 9.5D);
        } else if (chain.getAxis() == Axis.Y) {
            return new HexCollisionBox(6.5D, 0.0D, 6.5D, 9.5D, 16.0D, 9.5D);
        }

        return new HexCollisionBox(6.5D, 6.5D, 0.0D, 9.5D, 9.5D, 16.0D);
    }, ItemTypes.CHAIN),

    CHORUS_PLANT(new DynamicChorusPlant(), ItemTypes.CHORUS_PLANT),

    FENCE_GATE((player, version, data, x, y, z) -> {
        WrappedFenceGate gate = (WrappedFenceGate) data;

        if (gate.isOpen())
            return NoCollisionBox.INSTANCE;

        switch (gate.getDirection()) {
            case NORTH:
            case SOUTH:
                return new SimpleCollisionBox(0.0F, 0.0F, 0.375F, 1.0F, 1.5F, 0.625F, false);
            case WEST:
            case EAST:
                return new SimpleCollisionBox(0.375F, 0.0F, 0.0F, 0.625F, 1.5F, 1.0F, false);
        }

        // This code is unreachable but the compiler does not know this
        return NoCollisionBox.INSTANCE;

    }, Arrays.stream(Material.values()).filter(mat -> mat.name().contains("FENCE") && mat.name().contains("GATE"))
            .toArray(Material[]::new)),

    FENCE(new DynamicFence(), Arrays.stream(Material.values()).filter(mat -> mat.name().contains("FENCE") && !mat.name().contains("GATE") && !mat.name().contains("IRON_FENCE"))
            .toArray(Material[]::new)),


    PANE(new DynamicPane(), Arrays.stream(Material.values()).filter(mat -> mat.name().contains("GLASS_PANE") || mat.name().contains("IRON_BARS") || mat.name().contains("IRON_FENCE") || mat.name().equalsIgnoreCase("THIN_GLASS"))
            .toArray(Material[]::new)),

    SNOW((player, version, data, x, y, z) -> {
        WrappedSnow snow = (WrappedSnow) data;

        if (snow.getLayers() == 0 && version.isNewerThanOrEquals(ClientVersion.V_1_13))
            return NoCollisionBox.INSTANCE;

        return new SimpleCollisionBox(0, 0, 0, 1, snow.getLayers() * 0.125, 1);
    }, ItemTypes.SNOW),

    STAIR(new DynamicStair(),
            Arrays.stream(Material.values()).filter(mat -> mat.name().contains("STAIRS"))
                    .toArray(Material[]::new)),

    CHEST(new DynamicChest(), ItemTypes.CHEST, ItemTypes.TRAPPED_CHEST),

    ENDER_CHEST(new SimpleCollisionBox(0.0625F, 0.0F, 0.0625F,
            0.9375F, 0.875F, 0.9375F, false),
            ItemTypes.ENDER_CHEST),

    ENCHANTING_TABLE(new SimpleCollisionBox(0, 0, 0, 1, 1 - 0.25, 1, false),
            ItemTypes.ENCHANTING_TABLE),

    FRAME((player, version, data, x, y, z) -> {
        WrappedFrame frame = (WrappedFrame) data;
        ComplexCollisionBox complexCollisionBox = new ComplexCollisionBox(new HexCollisionBox(0.0D, 0.0D, 0.0D, 16.0D, 13.0D, 16.0D));

        if (frame.hasEye()) {
            if (version.isNewerThanOrEquals(ClientVersion.V_1_13)) {  // 1.13 players have a 0.5x0.5 eye
                complexCollisionBox.add(new HexCollisionBox(4.0D, 13.0D, 4.0D, 12.0D, 16.0D, 12.0D));
            } else { // 1.12 and below players have a 0.375x0.375 eye
                complexCollisionBox.add(new HexCollisionBox(5.0D, 13.0D, 5.0D, 11.0D, 16.0D, 11.0D));
            }
        }

        return complexCollisionBox;

    }, ItemTypes.END_PORTAL_FRAME),

    CARPET((player, version, data, x, y, z) -> {
        if (version.isOlderThanOrEquals(ClientVersion.V_1_7_10))
            return new SimpleCollisionBox(0.0F, 0.0F, 0.0F, 1.0F, 0.0F, 1.0F, false);

        return new SimpleCollisionBox(0.0F, 0.0F, 0.0F, 1.0F, 0.0625F, 1.0F, false);
    }, Arrays.stream(Material.values()).filter(mat -> mat.name().contains("CARPET")).toArray(Material[]::new)),

    DAYLIGHT(new SimpleCollisionBox(0.0F, 0.0F, 0.0F, 1.0F, 0.375, 1.0F, false),
            Arrays.stream(Material.values()).filter(mat -> mat.name().contains("DAYLIGHT")).toArray(Material[]::new)),

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

    }, ItemTypes.FARMLAND),

    GRASS_PATH((player, version, data, x, y, z) -> {
        if (version.isNewerThanOrEquals(ClientVersion.V_1_9))
            return new HexCollisionBox(0.0D, 0.0D, 0.0D, 16.0D, 15.0D, 16.0D);

        return new SimpleCollisionBox(0, 0, 0, 1, 1, 1, true);

    }, ItemTypes.DIRT_PATH),

    LILYPAD((player, version, data, x, y, z) -> {
        // Boats break lilypads client sided on 1.12- clients.
        if (player.playerVehicle != null && player.playerVehicle.type == EntityTypes.BOAT && version.isOlderThanOrEquals(ClientVersion.V_1_12_2))
            return NoCollisionBox.INSTANCE;

        if (version.isOlderThan(ClientVersion.V_1_9))
            return new SimpleCollisionBox(0.0f, 0.0F, 0.0f, 1.0f, 0.015625F, 1.0f, false);
        return new HexCollisionBox(1.0D, 0.0D, 1.0D, 15.0D, 1.5D, 15.0D);
    }, ItemTypes.LILY_PAD),

    BED((player, version, data, x, y, z) -> {
        // It's all the same box on 1.14 clients
        if (version.isOlderThan(ClientVersion.V_1_14))
            return new SimpleCollisionBox(0.0F, 0.0F, 0.0F, 1.0F, 0.5625, 1.0F, false);

        ComplexCollisionBox baseBox = new ComplexCollisionBox(new HexCollisionBox(0.0D, 3.0D, 0.0D, 16.0D, 9.0D, 16.0D));
        WrappedDirectional directional = (WrappedDirectional) data;

        switch (directional.getDirection()) {
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
    }, Arrays.stream(Material.values()).filter(mat -> mat.name().contains("BED") && !mat.name().contains("ROCK")).toArray(Material[]::new)),

    TRAPDOOR(new TrapDoorHandler(), Arrays.stream(Material.values())
            .filter(mat -> mat.name().contains("TRAP_DOOR") || mat.name().contains("TRAPDOOR")).toArray(Material[]::new)),


    DIODES(new SimpleCollisionBox(0.0F, 0.0F, 0.0F, 1.0F, 0.125F, 1.0F, false),
            Materials.matchLegacy("LEGACY_DIODE_BLOCK_OFF"), Materials.matchLegacy("LEGACY_DIODE_BLOCK_ON"),
            Materials.matchLegacy("LEGACY_REDSTONE_COMPARATOR_ON"), Materials.matchLegacy("LEGACY_REDSTONE_COMPARATOR_OFF"),
            ItemTypes.REPEATER, ItemTypes.COMPARATOR),

    STRUCTURE_VOID(new SimpleCollisionBox(0.375, 0.375, 0.375,
            0.625, 0.625, 0.625, false),
            ItemTypes.STRUCTURE_VOID),

    END_ROD((player, version, data, x, y, z) -> {
        WrappedDirectional directional = (WrappedDirectional) data;

        return getEndRod(version, directional.getDirection());

    }, ItemTypes.END_ROD, ItemTypes.LIGHTNING_ROD),

    CAULDRON((player, version, data, x, y, z) -> {
        double height = 0.25;

        if (version.isOlderThan(ClientVersion.V_1_13))
            height = 0.3125;

        return new ComplexCollisionBox(
                new SimpleCollisionBox(0, 0, 0, 1, height, 1, false),
                new SimpleCollisionBox(0, height, 0, 0.125, 1, 1, false),
                new SimpleCollisionBox(1 - 0.125, height, 0, 1, 1, 1, false),
                new SimpleCollisionBox(0, height, 0, 1, 1, 0.125, false),
                new SimpleCollisionBox(0, height, 1 - 0.125, 1, 1, 1, false));
    }, Arrays.stream(Material.values()).filter(mat -> mat.name().contains("CAULDRON")).toArray(Material[]::new)),

    CACTUS(new SimpleCollisionBox(0.0625, 0, 0.0625,
            1 - 0.0625, 1 - 0.0625, 1 - 0.0625, false), ItemTypes.CACTUS),


    PISTON_BASE(new PistonBaseCollision(), ItemTypes.PISTON, ItemTypes.STICKY_PISTON),

    PISTON_HEAD(new PistonHeadCollision(), ItemTypes.PISTON_HEAD),

    SOULSAND(new SimpleCollisionBox(0, 0, 0, 1, 0.875, 1, false),
            ItemTypes.SOUL_SAND),

    PICKLE((player, version, data, x, y, z) -> {
        SeaPickle pickle = (SeaPickle) ((WrappedFlatBlock) data).getBlockData();

        return getPicklesBox(version, pickle.getPickles());
    }, ItemTypes.SEA_PICKLE),

    TURTLEEGG((player, version, data, x, y, z) -> {
        TurtleEgg egg = (TurtleEgg) ((WrappedFlatBlock) data).getBlockData();

        // ViaVersion replacement block (West facing cocoa beans)
        if (version.isOlderThanOrEquals(ClientVersion.V_1_12_2)) {
            return getCocoa(version, egg.getEggs(), BlockFace.WEST);
        }

        if (egg.getEggs() == 1) {
            return new HexCollisionBox(3.0D, 0.0D, 3.0D, 12.0D, 7.0D, 12.0D);
        }

        return new HexCollisionBox(1.0D, 0.0D, 1.0D, 15.0D, 7.0D, 15.0D);
    }, ItemTypes.TURTLE_EGG),

    CONDUIT((player, version, data, x, y, z) -> {
        // ViaVersion replacement block - Beacon
        if (version.isOlderThanOrEquals(ClientVersion.V_1_12_2))
            return new SimpleCollisionBox(0, 0, 0, 1, 1, 1, true);

        return new HexCollisionBox(5.0D, 5.0D, 5.0D, 11.0D, 11.0D, 11.0D);
    }, ItemTypes.CONDUIT),

    POT(new HexCollisionBox(5.0D, 0.0D, 5.0D, 11.0D, 6.0D, 11.0D),
            Arrays.stream(Material.values()).filter(mat -> mat.name().contains("POTTED") || mat.name().contains("FLOWER_POT")).toArray(Material[]::new)),

    WALL_SIGN((player, version, data, x, y, z) -> {
        WrappedDirectional directional = (WrappedDirectional) data;

        switch (directional.getDirection()) {
            case NORTH:
                return new HexCollisionBox(0.0D, 4.5D, 14.0D, 16.0D, 12.5D, 16.0D);
            case SOUTH:
                return new HexCollisionBox(0.0D, 4.5D, 0.0D, 16.0D, 12.5D, 2.0D);
            case WEST:
                return new HexCollisionBox(14.0D, 4.5D, 0.0D, 16.0D, 12.5D, 16.0D);
            case EAST:
                return new HexCollisionBox(0.0D, 4.5D, 0.0D, 2.0D, 12.5D, 16.0D);
            default:
                return NoCollisionBox.INSTANCE;
        }
    }, Arrays.stream(Material.values()).filter(mat -> mat.name().contains("WALL_SIGN"))
            .toArray(Material[]::new)),

    WALL_FAN((player, version, data, x, y, z) -> {
        CoralWallFan fan = (CoralWallFan) ((WrappedFlatBlock) data).getBlockData();

        switch (fan.getFacing()) {
            case NORTH:
                return new HexCollisionBox(0.0D, 4.0D, 5.0D, 16.0D, 12.0D, 16.0D);
            case SOUTH:
                return new HexCollisionBox(0.0D, 4.0D, 0.0D, 16.0D, 12.0D, 11.0D);
            case WEST:
                return new HexCollisionBox(5.0D, 4.0D, 0.0D, 16.0D, 12.0D, 16.0D);
            case EAST:
            default:
                return new HexCollisionBox(0.0D, 4.0D, 0.0D, 11.0D, 12.0D, 16.0D);
        }
    }, Arrays.stream(Material.values()).filter(mat -> mat.name().contains("WALL_FAN")).toArray(Material[]::new)),

    CORAL_PLANT((player, version, data, x, y, z) -> {
        return new HexCollisionBox(2.0D, 0.0D, 2.0D, 14.0D, 15.0D, 14.0D);
    }, Arrays.stream(Material.values()).filter(mat -> mat.name().endsWith("_CORAL")).toArray(Material[]::new)),

    // The nether signes map to sign post and other regular sign
    SIGN(new SimpleCollisionBox(0.25, 0.0, 0.25, 0.75, 1.0, 0.75, false),
            Arrays.stream(Material.values()).filter(mat -> mat.name().contains("SIGN") && !mat.name().contains("WALL"))
                    .toArray(Material[]::new)),

    BEETROOT((player, version, data, x, y, z) -> {
        WrappedAgeable ageable = (WrappedAgeable) data;
        return new HexCollisionBox(0.0D, 0.0D, 0.0D, 1.0D, (ageable.getAge() + 1) * 2, 1.0D);
    }, ItemTypes.BEETROOT),

    WHEAT((player, version, data, x, y, z) -> {
        WrappedAgeable ageable = (WrappedAgeable) data;
        return new HexCollisionBox(0.0D, 0.0D, 0.0D, 1.0D, (ageable.getAge() + 1) * 2, 1.0D);
    }, ItemTypes.WHEAT),

    CARROT_NETHERWART((player, version, data, x, y, z) -> {
        WrappedAgeable ageable = (WrappedAgeable) data;
        return new HexCollisionBox(0.0D, 0.0D, 0.0D, 1.0D, ageable.getAge() + 2, 1.0D);
    }, ItemTypes.CARROT, ItemTypes.NETHER_WART),

    NETHER_WART((player, version, data, x, y, z) -> {
        WrappedAgeable ageable = (WrappedAgeable) data;
        return new HexCollisionBox(0.0D, 0.0D, 0.0D, 1.0D, 5 + (ageable.getAge() * 3), 1.0D);
    }, ItemTypes.NETHER_WART),

    BUTTON((player, version, data, x, y, z) -> {
        WrappedDirectionalPower button = (WrappedDirectionalPower) data;
        double f2 = (float) (button.isPowered() ? 1 : 2) / 16.0;

        switch (button.getDirection()) {
            case WEST:
                return new SimpleCollisionBox(0.0, 0.375, 0.3125, f2, 0.625, 0.6875, false);
            case EAST:
                return new SimpleCollisionBox(1.0 - f2, 0.375, 0.3125, 1.0, 0.625, 0.6875, false);
            case NORTH:
                return new SimpleCollisionBox(0.3125, 0.375, 0.0, 0.6875, 0.625, f2, false);
            case SOUTH:
                return new SimpleCollisionBox(0.3125, 0.375, 1.0 - f2, 0.6875, 0.625, 1.0, false);
            case DOWN:
                return new SimpleCollisionBox(0.3125, 0.0, 0.375, 0.6875, 0.0 + f2, 0.625, false);
            case UP:
                return new SimpleCollisionBox(0.3125, 1.0 - f2, 0.375, 0.6875, 1.0, 0.625, false);
        }

        return NoCollisionBox.INSTANCE;

    }, Arrays.stream(Material.values()).filter(mat -> mat.name().contains("BUTTON")).toArray(Material[]::new)),

    LEVER((player, version, data, x, y, z) -> {
        double f = 0.1875;

        switch (((WrappedDirectional) data).getDirection()) {
            case WEST:
                return new SimpleCollisionBox(1.0 - f * 2.0, 0.2, 0.5 - f, 1.0, 0.8, 0.5 + f, false);
            case EAST:
                return new SimpleCollisionBox(0.0, 0.2, 0.5 - f, f * 2.0, 0.8, 0.5 + f, false);
            case NORTH:
                return new SimpleCollisionBox(0.5 - f, 0.2, 1.0 - f * 2.0, 0.5 + f, 0.8, 1.0, false);
            case SOUTH:
                return new SimpleCollisionBox(0.5 - f, 0.2, 0.0, 0.5 + f, 0.8, f * 2.0, false);
            case DOWN:
                return new SimpleCollisionBox(0.25, 0.4, 0.25, 0.75, 1.0, 0.75, false);
            case UP:
                return new SimpleCollisionBox(0.25, 0.0, 0.25, 0.75, 0.6, 0.75, false);
        }

        return NoCollisionBox.INSTANCE;

    }, ItemTypes.LEVER),

    PRESSURE_PLATE((player, version, data, x, y, z) -> {
        WrappedPower power = ((WrappedPower) data);

        if (power.getPower() == 15) { // Pressed
            return new HexCollisionBox(1.0D, 0.0D, 1.0D, 15.0D, 0.5D, 15.0D);
        }

        return new HexCollisionBox(1.0D, 0.0D, 1.0D, 15.0D, 1.0D, 15.0D);
    }, Arrays.stream(Material.values()).filter(mat -> mat.name().contains("PLATE")).toArray(Material[]::new)),

    TRIPWIRE((player, version, data, x, y, z) -> {
        WrappedTripwire power = ((WrappedTripwire) data);
        if (power.isAttached()) {
            return new HexCollisionBox(0.0D, 1.0D, 0.0D, 16.0D, 2.5D, 16.0D);
        }
        return new HexCollisionBox(0.0D, 0.0D, 0.0D, 16.0D, 8.0D, 16.0D);
    }, ItemTypes.TRIPWIRE),

    ATTACHED_PUMPKIN_STEM((player, version, data, x, y, z) -> {
        if (version.isOlderThan(ClientVersion.V_1_13))
            return new HexCollisionBox(7.0D, 0.0D, 7.0D, 9.0D, 16.0D, 9.0D);

        Directional directional = (Directional) ((WrappedFlatBlock) data).getBlockData();
        switch (directional.getFacing()) {
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
    }, ItemTypes.ATTACHED_MELON_STEM, ItemTypes.ATTACHED_PUMPKIN_STEM),

    PUMPKIN_STEM((player, version, data, x, y, z) -> {
        WrappedAgeable ageable = (WrappedAgeable) data;
        return new HexCollisionBox(7, 0, 7, 9, 2 * (ageable.getAge() + 1), 9);
    }, ItemTypes.PUMPKIN_STEM, ItemTypes.MELON_STEM),

    TRIPWIRE_HOOK((player, version, data, x, y, z) -> {
        WrappedDirectionalPower directional = (WrappedDirectionalPower) data;

        switch (directional.getDirection()) {
            case NORTH:
                return new HexCollisionBox(5.0D, 0.0D, 10.0D, 11.0D, 10.0D, 16.0D);
            case SOUTH:
                return new HexCollisionBox(5.0D, 0.0D, 0.0D, 11.0D, 10.0D, 6.0D);
            case WEST:
                return new HexCollisionBox(10.0D, 0.0D, 5.0D, 16.0D, 10.0D, 11.0D);
            case EAST:
            default:
                return new HexCollisionBox(0.0D, 0.0D, 5.0D, 6.0D, 10.0D, 11.0D);
        }
    }, ItemTypes.TRIPWIRE_HOOK),

    TORCH(new HexCollisionBox(6.0D, 0.0D, 6.0D, 10.0D, 10.0D, 10.0D),
            ItemTypes.TORCH, ItemTypes.REDSTONE_TORCH),

    WALL_TORCH((player, version, data, x, y, z) -> {
        Directional directional = (Directional) data;

        switch (directional.getFacing()) {
            case NORTH:
                return new HexCollisionBox(5.5D, 3.0D, 11.0D, 10.5D, 13.0D, 16.0D);
            case SOUTH:
                return new HexCollisionBox(5.5D, 3.0D, 0.0D, 10.5D, 13.0D, 5.0D);
            case WEST:
                return new HexCollisionBox(11.0D, 3.0D, 5.5D, 16.0D, 13.0D, 10.5D);
            case EAST:
                return new HexCollisionBox(0.0D, 3.0D, 5.5D, 5.0D, 13.0D, 10.5D);
            default: // 1.13 separates wall and normal torches, 1.12 does not
            case UP:
                return new HexCollisionBox(6.0D, 0.0D, 6.0D, 10.0D, 10.0D, 10.0D);
        }

    }, ItemTypes.WALL_TORCH, ItemTypes.REDSTONE_WALL_TORCH),

    RAILS((player, version, data, x, y, z) -> {
        WrappedRails rail = (WrappedRails) data;

        if (rail.isAscending()) {
            return new HexCollisionBox(0.0D, 0.0D, 0.0D, 16.0D, 8.0D, 16.0D);
        }

        return new HexCollisionBox(0.0D, 0.0D, 0.0D, 16.0D, 2.0D, 16.0D);

    }, Arrays.stream(Material.values()).filter(mat -> mat.name().contains("RAIL")).toArray(Material[]::new)),

    // Known as block 36 - has no collision box
    TECHNICAL_MOVING_PISTON(NoCollisionBox.INSTANCE, Arrays.stream(Material.values()).filter(mat -> mat.name().contains("MOVING")).toArray(Material[]::new)),

    // 1.17 blocks
    CANDLE((player, version, data, x, y, z) -> {
        Candle candle = (Candle) ((WrappedFlatBlock) data).getBlockData();

        if (version.isNewerThanOrEquals(ClientVersion.V_1_17)) {
            switch (candle.getCandles()) {
                case 1:
                    return new HexCollisionBox(7.0, 0.0, 7.0, 9.0, 6.0, 9.0);
                case 2:
                    return new HexCollisionBox(5.0, 0.0, 6.0, 11.0, 6.0, 9.0);
                case 3:
                    return new HexCollisionBox(5.0, 0.0, 6.0, 10.0, 6.0, 11.0);
                default:
                case 4:
                    return new HexCollisionBox(5.0, 0.0, 5.0, 11.0, 6.0, 10.0);
            }
        }

        return getPicklesBox(version, candle.getCandles());
    }, Arrays.stream(Material.values()).filter(mat -> mat.name().endsWith("CANDLE")).toArray(Material[]::new)),

    CANDLE_CAKE((player, version, data, x, y, z) -> {
        ComplexCollisionBox cake = new ComplexCollisionBox(new HexCollisionBox(1.0, 0.0, 1.0, 15.0, 8.0, 15.0));
        if (version.isNewerThanOrEquals(ClientVersion.V_1_17))
            cake.add(new HexCollisionBox(7.0, 8.0, 7.0, 9.0, 14.0, 9.0));
        return cake;
    }, Arrays.stream(Material.values()).filter(mat -> mat.name().endsWith("CANDLE_CAKE")).toArray(Material[]::new)),

    SCULK_SENSOR(new HexCollisionBox(0.0, 0.0, 0.0, 16.0, 8.0, 16.0), ItemTypes.SCULK_SENSOR),

    BIG_DRIPLEAF((player, version, data, x, y, z) -> {
        if (version.isOlderThanOrEquals(ClientVersion.V_1_16_4))
            return new SimpleCollisionBox(0, 0, 0, 1, 1, 1, true);

        BigDripleaf dripleaf = (BigDripleaf) ((WrappedFlatBlock) data).getBlockData();

        if (dripleaf.getTilt() == BigDripleaf.Tilt.NONE || dripleaf.getTilt() == BigDripleaf.Tilt.UNSTABLE) {
            return new HexCollisionBox(0.0, 11.0, 0.0, 16.0, 15.0, 16.0);
        } else if (dripleaf.getTilt() == BigDripleaf.Tilt.PARTIAL) {
            return new HexCollisionBox(0.0, 11.0, 0.0, 16.0, 13.0, 16.0);
        }

        return NoCollisionBox.INSTANCE;

    }, ItemTypes.BIG_DRIPLEAF),

    DRIPSTONE((player, version, data, x, y, z) -> {
        PointedDripstone dripstone = (PointedDripstone) ((WrappedFlatBlock) data).getBlockData();

        if (version.isOlderThan(ClientVersion.V_1_17))
            return getEndRod(version, BlockFace.UP);

        HexCollisionBox box;

        if (dripstone.getThickness() == PointedDripstone.Thickness.TIP_MERGE) {
            box = new HexCollisionBox(5.0, 0.0, 5.0, 11.0, 16.0, 11.0);
        } else if (dripstone.getThickness() == PointedDripstone.Thickness.TIP) {
            if (dripstone.getVerticalDirection() == org.bukkit.block.BlockFace.DOWN) {
                box = new HexCollisionBox(5.0, 5.0, 5.0, 11.0, 16.0, 11.0);
            } else {
                box = new HexCollisionBox(5.0, 0.0, 5.0, 11.0, 11.0, 11.0);
            }
        } else if (dripstone.getThickness() == PointedDripstone.Thickness.FRUSTUM) {
            box = new HexCollisionBox(4.0, 0.0, 4.0, 12.0, 16.0, 12.0);
        } else if (dripstone.getThickness() == PointedDripstone.Thickness.MIDDLE) {
            box = new HexCollisionBox(3.0, 0.0, 3.0, 13.0, 16.0, 13.0);
        } else {
            box = new HexCollisionBox(2.0, 0.0, 2.0, 14.0, 16.0, 14.0);
        }

        // Copied from NMS and it works!  That's all you need to know.
        long i = (x * 3129871L) ^ (long) z * 116129781L ^ (long) 0;
        i = i * i * 42317861L + i * 11L;
        i = i >> 16;

        return box.offset(GrimMath.clamp((((i & 15L) / 15.0F) - 0.5D) * 0.5D, -0.125f, 0.125f), 0, GrimMath.clamp((((i >> 8 & 15L) / 15.0F) - 0.5D) * 0.5D, -0.125f, 0.125f));
    }, ItemTypes.POINTED_DRIPSTONE),

    POWDER_SNOW((player, version, data, x, y, z) -> {
        if (version.isOlderThanOrEquals(ClientVersion.V_1_16_4))
            return new SimpleCollisionBox(0, 0, 0, 1, 1, 1, true);

        // If fall distance greater than 2.5, 0.899999 box
        if (player.fallDistance > 2.5) {
            return new SimpleCollisionBox(0.0, 0.0, 0.0, 1.0, 0.8999999761581421, 1.0, false);
        }

        org.bukkit.inventory.ItemStack boots = player.bukkitPlayer.getInventory().getBoots();
        if (player.lastY > y + 1 - 9.999999747378752E-6 && boots != null && boots.getType() == Material.LEATHER_BOOTS && !player.isSneaking)
            return new SimpleCollisionBox(0, 0, 0, 1, 1, 1, true);

        return NoCollisionBox.INSTANCE;

    }, ItemTypes.POWDER_SNOW),

    AZALEA((player, version, data, x, y, z) -> {
        return new ComplexCollisionBox(new HexCollisionBox(0.0, 8.0, 0.0, 16.0, 16.0, 16.0),
                new HexCollisionBox(6.0, 0.0, 6.0, 10.0, 8.0, 10.0));
    }, ItemTypes.AZALEA, ItemTypes.FLOWERING_AZALEA),

    AMETHYST_CLUSTER((player, version, data, x, y, z) -> {
        Directional cluster = (Directional) ((WrappedFlatBlock) data).getBlockData();
        return getAmethystBox(version, cluster.getFacing(), 7, 3);
    }, ItemTypes.AMETHYST_CLUSTER),

    SMALL_AMETHYST_BUD((player, version, data, x, y, z) -> {
        Directional cluster = (Directional) ((WrappedFlatBlock) data).getBlockData();
        return getAmethystBox(version, cluster.getFacing(), 3, 4);
    }, ItemTypes.SMALL_AMETHYST_BUD),

    MEDIUM_AMETHYST_BUD((player, version, data, x, y, z) -> {
        Directional cluster = (Directional) ((WrappedFlatBlock) data).getBlockData();
        return getAmethystBox(version, cluster.getFacing(), 4, 3);
    }, ItemTypes.MEDIUM_AMETHYST_BUD),

    LARGE_AMETHYST_BUD((player, version, data, x, y, z) -> {
        Directional cluster = (Directional) ((WrappedFlatBlock) data).getBlockData();
        return getAmethystBox(version, cluster.getFacing(), 5, 3);
    }, ItemTypes.LARGE_AMETHYST_BUD),

    NONE(NoCollisionBox.INSTANCE, ItemTypes.AIR),

    DEFAULT(new SimpleCollisionBox(0, 0, 0, 1, 1, 1, true),
            ItemTypes.STONE);

    private static final CollisionData[] lookup = new CollisionData[Material.values().length];
    private static final CollisionData[] raw_lookup = new CollisionData[Material.values().length];

    static {
        for (CollisionData data : values()) {
            for (Material mat : data.materials) lookup[mat.ordinal()] = data;
            for (Material mat : data.materials) raw_lookup[mat.ordinal()] = data;
        }

        // If a block is not solid, then it does not have a collision box
        for (Material mat : Material.values()) {
            if (!Materials.checkFlag(mat, Materials.SOLID)) lookup[mat.ordinal()] = NONE;
        }

        for (Material mat : Material.values()) {
            if (lookup[mat.ordinal()] == null) lookup[mat.ordinal()] = DEFAULT;
            if (raw_lookup[mat.ordinal()] == null) raw_lookup[mat.ordinal()] = DEFAULT;
        }
    }

    public final Material[] materials;
    public CollisionBox box;
    public CollisionFactory dynamic;

    CollisionData(CollisionBox box, Material... materials) {
        this.box = box;
        Set<Material> mList = new HashSet<>(Arrays.asList(materials));
        mList.remove(null); // Sets can contain one null
        this.materials = mList.toArray(new Material[0]);
    }

    CollisionData(CollisionFactory dynamic, Material... materials) {
        this.dynamic = dynamic;
        Set<Material> mList = new HashSet<>(Arrays.asList(materials));
        mList.remove(null); // Sets can contain one null
        this.materials = mList.toArray(new Material[0]);
    }

    private static CollisionBox getAmethystBox(ClientVersion version, org.bukkit.block.BlockFace facing, int param_0, int param_1) {
        if (version.isOlderThanOrEquals(ClientVersion.V_1_16_4))
            return NoCollisionBox.INSTANCE;

        switch (facing) {
            default:
            case UP:
                return new HexCollisionBox(param_1, 0.0, param_1, 16 - param_1, param_0, 16 - param_1);
            case DOWN:
                return new HexCollisionBox(param_1, 16 - param_0, param_1, 16 - param_1, 16.0, 16 - param_1);
            case NORTH:
                return new HexCollisionBox(param_1, param_1, 16 - param_0, 16 - param_1, 16 - param_1, 16.0);
            case SOUTH:
                return new HexCollisionBox(param_1, param_1, 0.0, 16 - param_1, 16 - param_1, param_0);
            case EAST:
                return new HexCollisionBox(0.0, param_1, param_1, param_0, 16 - param_1, 16 - param_1);
            case WEST:
                return new HexCollisionBox(16 - param_0, param_1, param_1, 16.0, 16 - param_1, 16 - param_1);
        }
    }

    private static CollisionBox getPicklesBox(ClientVersion version, int pickles) {
        // ViaVersion replacement block (West facing cocoa beans)
        if (version.isOlderThanOrEquals(ClientVersion.V_1_12_2)) {
            return getCocoa(version, pickles, BlockFace.WEST);
        }

        switch (pickles) {
            case 1:
                return new HexCollisionBox(6.0D, 0.0D, 6.0D, 10.0D, 6.0D, 10.0D);
            case 2:
                return new HexCollisionBox(3.0D, 0.0D, 3.0D, 13.0D, 6.0D, 13.0D);
            case 3:
                return new HexCollisionBox(2.0D, 0.0D, 2.0D, 14.0D, 6.0D, 14.0D);
            case 4:
                return new HexCollisionBox(2.0D, 0.0D, 2.0D, 14.0D, 7.0D, 14.0D);
        }
        return NoCollisionBox.INSTANCE;
    }

    private static CollisionBox getCocoa(ClientVersion version, int age, BlockFace direction) {
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

        switch (face) {
            case UP:
            case DOWN:
            default:
                return new HexCollisionBox(6.0D, 0.0D, 6.0D, 10.0D, 16.0D, 10.0);
            case NORTH:
            case SOUTH:
                return new HexCollisionBox(6.0D, 6.0D, 0.0D, 10.0D, 10.0D, 16.0D);
            case EAST:
            case WEST:
                return new HexCollisionBox(0.0D, 6.0D, 6.0D, 16.0D, 10.0D, 10.0D);
        }
    }

    public static CollisionData getData(Material material) {
        return lookup[material.ordinal()];
    }

    public static CollisionData getRawData(Material material) {
        return raw_lookup[material.ordinal()];
    }

    public CollisionBox getMovementCollisionBox(GrimPlayer player, ClientVersion version, BaseBlockState block, int x, int y, int z) {
        if (this.box != null)
            return this.box.copy().offset(x, y, z);

        WrappedBlockDataValue blockData = WrappedBlockData.getMaterialData(block);
        return new DynamicCollisionBox(player, version, dynamic, blockData).offset(x, y, z);
    }

    public CollisionBox getMovementCollisionBox(GrimPlayer player, ClientVersion version, BaseBlockState block) {
        if (this.box != null)
            return this.box.copy();

        WrappedBlockDataValue blockData = WrappedBlockData.getMaterialData(block);
        return new DynamicCollisionBox(player, version, dynamic, blockData);
    }
}