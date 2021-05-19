package ac.grim.grimac.utils.nmsImplementations;

import ac.grim.grimac.utils.blockdata.*;
import ac.grim.grimac.utils.blockstate.BaseBlockState;
import ac.grim.grimac.utils.collisions.CollisionBox;
import ac.grim.grimac.utils.collisions.Materials;
import ac.grim.grimac.utils.collisions.blocks.*;
import ac.grim.grimac.utils.collisions.types.*;
import ac.grim.grimac.utils.data.ProtocolVersion;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.FaceAttachable;
import org.bukkit.block.data.type.*;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import static ac.grim.grimac.utils.collisions.Materials.matchLegacy;

public enum CollisionData {
    VINE((version, block, x, y, z) -> {
        ComplexCollisionBox boxes = new ComplexCollisionBox();

        for (BlockFace face : ((WrappedMultipleFacing) block).getDirections()) {
            if (face == BlockFace.SOUTH) {
                boxes.add(new SimpleCollisionBox(0., 0., 0.9375, 1., 1., 1.));
            }

            if (face == BlockFace.WEST) {
                boxes.add(new SimpleCollisionBox(0., 0., 0., 0.0625, 1., 1.));
            }

            if (face == BlockFace.NORTH) {
                boxes.add(new SimpleCollisionBox(0., 0., 0., 1., 1., 0.0625));
            }

            if (face == BlockFace.EAST) {
                boxes.add(new SimpleCollisionBox(0.9375, 0., 0., 1., 1., 1.));
            }
        }

        return boxes;

    }, XMaterial.VINE.parseMaterial()),


    LIQUID(new SimpleCollisionBox(0, 0, 0, 1f, 0.9f, 1f),
            XMaterial.WATER.parseMaterial(), XMaterial.LAVA.parseMaterial()),

    BREWINGSTAND((version, block, x, y, z) -> {
        int base = 0;

        if (version.isOrAbove(ProtocolVersion.V1_13))
            base = 1;

        return new ComplexCollisionBox(
                new HexCollisionBox(base, 0, base, 16 - base, 2, 16 - base),
                new SimpleCollisionBox(0.4375, 0.0, 0.4375, 0.5625, 0.875, 0.5625));

    }, XMaterial.BREWING_STAND.parseMaterial()),

    BAMBOO((version, block, x, y, z) -> {
        // Offset taken from NMS
        long i = (x * 3129871L) ^ (long) z * 116129781L ^ (long) 0;
        i = i * i * 42317861L + i * 11L;
        i = i >> 16;

        return new HexCollisionBox(6.5D, 0.0D, 6.5D, 9.5D, 16.0D, 9.5D).offset((((i & 15L) / 15.0F) - 0.5D) * 0.5D, 0, (((i >> 8 & 15L) / 15.0F) - 0.5D) * 0.5D);
    }, XMaterial.BAMBOO.parseMaterial()),


    BAMBOO_SAPLING((version, block, x, y, z) -> {
        long i = (x * 3129871L) ^ (long) z * 116129781L ^ (long) 0;
        i = i * i * 42317861L + i * 11L;
        i = i >> 16;

        return new HexCollisionBox(4.0D, 0.0D, 4.0D, 12.0D, 12.0D, 12.0D).offset((((i & 15L) / 15.0F) - 0.5D) * 0.5D, 0, (((i >> 8 & 15L) / 15.0F) - 0.5D) * 0.5D);
    }, XMaterial.BAMBOO_SAPLING.parseMaterial()),

    COMPOSTER((version, block, x, y, z) -> {
        double height = 0.125;

        return new ComplexCollisionBox(
                new SimpleCollisionBox(0, 0, 0, 1, height, 1),
                new SimpleCollisionBox(0, height, 0, 0.125, 1, 1),
                new SimpleCollisionBox(1 - 0.125, height, 0, 1, 1, 1),
                new SimpleCollisionBox(0, height, 0, 1, 1, 0.125),
                new SimpleCollisionBox(0, height, 1 - 0.125, 1, 1, 1));
    }, XMaterial.COMPOSTER.parseMaterial()),

    RAIL(new SimpleCollisionBox(0, 0, 0, 1, 0.125, 0),
            XMaterial.RAIL.parseMaterial(), XMaterial.ACTIVATOR_RAIL.parseMaterial(),
            XMaterial.DETECTOR_RAIL.parseMaterial(), XMaterial.POWERED_RAIL.parseMaterial()),

    ANVIL((version, data, x, y, z) -> {
        // Anvil collision box was changed in 1.13 to be more accurate
        // https://www.mcpk.wiki/wiki/Version_Differences
        // The base is 0.75×0.75, and its floor is 0.25b high.
        // The top is 1×0.625, and its ceiling is 0.375b low.
        if (version.isOrAbove(ProtocolVersion.V1_13)) {
            ComplexCollisionBox complexAnvil = new ComplexCollisionBox();
            // Base of the anvil
            complexAnvil.add(new HexCollisionBox(2, 0, 2, 14, 4, 14));

            if (((WrappedDirectional) data).getDirection() == BlockFace.NORTH) {
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
            if (((WrappedDirectional) data).getDirection() == BlockFace.NORTH) {
                return new SimpleCollisionBox(0.125F, 0.0F, 0.0F, 0.875F, 1.0F, 1.0F);
            } else {
                return new SimpleCollisionBox(0.0F, 0.0F, 0.125F, 1.0F, 1.0F, 0.875F);
            }
        }
    }, XMaterial.ANVIL.parseMaterial(), XMaterial.CHIPPED_ANVIL.parseMaterial(), XMaterial.DAMAGED_ANVIL.parseMaterial()),


    WALL(new DynamicWall(), Arrays.stream(XMaterial.values()).filter(mat -> mat.name().contains("WALL")
            && !mat.name().contains("SIGN") && !mat.name().contains("HEAD") && !mat.name().contains("BANNER")
            && !mat.name().contains("FAN") && !mat.name().contains("SKULL") && !mat.name().contains("TORCH"))
            .map(XMaterial::parseMaterial)
            .toArray(Material[]::new)),


    SLAB((version, data, x, y, z) -> {
        if (((WrappedSlab) data).isDouble()) {
            return new SimpleCollisionBox(0, 0, 0, 1, 1, 1);
        } else if (((WrappedSlab) data).isBottom()) {
            return new SimpleCollisionBox(0, 0, 0, 1, 0.5, 1);
        }

        return new SimpleCollisionBox(0, 0.5, 0, 1, 1, 1);
        // 1.13 can handle double slabs as it's in the block data
        // 1.12 has double slabs as a separate block, no block data to differentiate it
    }, Arrays.stream(XMaterial.values()).filter(mat -> mat.name().contains("_SLAB"))
            .map(XMaterial::parseMaterial).filter(Objects::nonNull).filter(m -> !m.name().contains("DOUBLE")).toArray(Material[]::new)),

    WALL_SKULL((version, data, x, y, z) -> {
        switch (((WrappedDirectional) data).getDirection()) {
            case DOWN:
            default: // On the floor
                return new SimpleCollisionBox(0.25F, 0.0F, 0.25F, 0.75F, 0.5F, 0.75F);
            case NORTH:
                return new SimpleCollisionBox(0.25F, 0.25F, 0.5F, 0.75F, 0.75F, 1.0F);
            case SOUTH:
                return new SimpleCollisionBox(0.25F, 0.25F, 0.0F, 0.75F, 0.75F, 0.5F);
            case WEST:
                return new SimpleCollisionBox(0.5F, 0.25F, 0.25F, 1.0F, 0.75F, 0.75F);
            case EAST:
                return new SimpleCollisionBox(0.0F, 0.25F, 0.25F, 0.5F, 0.75F, 0.75F);
        }
    }, XMaterial.SKELETON_WALL_SKULL.parseMaterial(), XMaterial.WITHER_SKELETON_WALL_SKULL.parseMaterial(),
            XMaterial.CREEPER_WALL_HEAD.parseMaterial(), XMaterial.DRAGON_WALL_HEAD.parseMaterial(), // Yes, the dragon head has the same collision box as regular heads
            XMaterial.PLAYER_WALL_HEAD.parseMaterial(), XMaterial.ZOMBIE_WALL_HEAD.parseMaterial()),


    SKULL(new SimpleCollisionBox(0.25, 0, 0.25, 0.75, 0.5, 0.75), XMaterial.SKELETON_SKULL.parseMaterial(), XMaterial.WITHER_SKELETON_SKULL.parseMaterial(),
            XMaterial.CREEPER_HEAD.parseMaterial(), XMaterial.DRAGON_HEAD.parseMaterial(), // Yes, the dragon head has the same collision box as regular heads
            XMaterial.PLAYER_HEAD.parseMaterial(), XMaterial.ZOMBIE_HEAD.parseMaterial()),


    DOOR(new DoorHandler(), Arrays.stream(XMaterial.values()).filter(mat -> mat.name().contains("_DOOR"))
            .map(XMaterial::parseMaterial).toArray(Material[]::new)),

    HOPPER((version, data, x, y, z) -> {
        double height = 0.125 * 5;

        if (version.isOrAbove(ProtocolVersion.V1_13))
            height = 0.6875;

        return new ComplexCollisionBox(
                new SimpleCollisionBox(0, 0, 0, 1, height, 1),
                new SimpleCollisionBox(0, height, 0, 0.125, 1, 1),
                new SimpleCollisionBox(1 - 0.125, height, 0, 1, 1, 1),
                new SimpleCollisionBox(0, height, 0, 1, 1, 0.125),
                new SimpleCollisionBox(0, height, 1 - 0.125, 1, 1, 1));
    }, XMaterial.HOPPER.parseMaterial()),

    CAKE((version, data, x, y, z) -> {
        double eatenPosition = (1 + ((WrappedCake) data).getSlicesEaten() * 2) / 16D;
        return new SimpleCollisionBox(eatenPosition, 0, 0.0625, 1 - 0.0625, 0.5, 1 - 0.0625);
    }, XMaterial.CAKE.parseMaterial()),


    COCOA_BEANS((version, data, x, y, z) -> {
        WrappedCocoaBeans beans = (WrappedCocoaBeans) data;
        int age = beans.getAge();

        // From 1.9 - 1.10, the large cocoa block is the same as the medium one
        // https://bugs.mojang.com/browse/MC-94274
        if (version.isOrAbove(ProtocolVersion.V1_9_1) && version.isBelow(ProtocolVersion.V1_11))
            age = Math.min(age, 1);

        switch (beans.getDirection()) {
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

    }, XMaterial.COCOA.parseMaterial()),


    STONE_CUTTER(new HexCollisionBox(0.0D, 0.0D, 0.0D, 16.0D, 9.0D, 16.0D),
            XMaterial.STONECUTTER.parseMaterial()),

    BELL((version, data, x, y, z) -> {
        Bell bell = (Bell) ((WrappedFlatBlock) data).getBlockData();
        BlockFace direction = bell.getFacing();

        if (bell.getAttachment() == Bell.Attachment.FLOOR) {
            return direction != BlockFace.NORTH && direction != BlockFace.SOUTH ?
                    new HexCollisionBox(4.0D, 0.0D, 0.0D, 12.0D, 16.0D, 16.0D) :
                    new HexCollisionBox(0.0D, 0.0D, 4.0D, 16.0D, 16.0D, 12.0D);

        }

        ComplexCollisionBox complex = new ComplexCollisionBox(
                new HexCollisionBox(5.0D, 6.0D, 5.0D, 11.0D, 13.0D, 11.0D),
                new HexCollisionBox(4.0D, 4.0D, 4.0D, 12.0D, 6.0D, 12.0D));

        if (bell.getAttachment() == Bell.Attachment.CEILING) {
            complex.add(new HexCollisionBox(7.0D, 13.0D, 7.0D, 9.0D, 16.0D, 9.0D));
        } else if (bell.getAttachment() == Bell.Attachment.DOUBLE_WALL) {
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

    }, XMaterial.BELL.parseMaterial()),

    LADDER((version, data, x, y, z) -> {
        switch (((WrappedDirectional) data).getDirection()) {
            case NORTH:
                return new HexCollisionBox(0.0D, 0.0D, 13.0D, 16.0D, 16.0D, 16.0D);
            case SOUTH:
                return new HexCollisionBox(0.0D, 0.0D, 0.0D, 16.0D, 16.0D, 3.0D);
            case WEST:
                return new HexCollisionBox(13.0D, 0.0D, 0.0D, 16.0D, 16.0D, 16.0D);
            default:
            case EAST:
                return new HexCollisionBox(0.0D, 0.0D, 0.0D, 3.0D, 16.0D, 16.0D);
        }
    }, XMaterial.LADDER.parseMaterial()),

    CAMPFIRE(new HexCollisionBox(0.0D, 0.0D, 0.0D, 16.0D, 7.0D, 16.0D), XMaterial.CAMPFIRE.parseMaterial(), XMaterial.SOUL_CAMPFIRE.parseMaterial()),

    LANTERN((version, data, x, y, z) -> {
        WrappedFlatBlock lantern = (WrappedFlatBlock) data;

        if (((Lantern) lantern.getBlockData()).isHanging()) {
            return new ComplexCollisionBox(new HexCollisionBox(5.0D, 1.0D, 5.0D, 11.0D, 8.0D, 11.0D),
                    new HexCollisionBox(6.0D, 8.0D, 6.0D, 10.0D, 10.0D, 10.0D));
        }

        return new ComplexCollisionBox(new HexCollisionBox(5.0D, 0.0D, 5.0D, 11.0D, 7.0D, 11.0D),
                new HexCollisionBox(6.0D, 7.0D, 6.0D, 10.0D, 9.0D, 10.0D));

    }, XMaterial.LANTERN.parseMaterial(), XMaterial.SOUL_LANTERN.parseMaterial()),


    LECTERN(new ComplexCollisionBox(
            new HexCollisionBox(0.0D, 0.0D, 0.0D, 16.0D, 2.0D, 16.0D), // base
            new HexCollisionBox(4.0D, 2.0D, 4.0D, 12.0D, 14.0D, 12.0D)) // post
            , XMaterial.LECTERN.parseMaterial()),


    HONEY_BLOCK(new HexCollisionBox(1.0D, 0.0D, 1.0D, 15.0D, 15.0D, 15.0D)
            , XMaterial.HONEY_BLOCK.parseMaterial()),


    DRAGON_EGG_BLOCK(new HexCollisionBox(1.0D, 0.0D, 1.0D, 15.0D, 16.0D, 15.0D), XMaterial.DRAGON_EGG.parseMaterial()),

    GRINDSTONE((version, data, x, y, z) -> {
        Grindstone grindstone = (Grindstone) ((WrappedFlatBlock) data).getBlockData();

        if (grindstone.getAttachedFace() == FaceAttachable.AttachedFace.FLOOR) {
            if (grindstone.getFacing() == BlockFace.NORTH || grindstone.getFacing() == BlockFace.SOUTH) {
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
            if (grindstone.getFacing() == BlockFace.NORTH || grindstone.getFacing() == BlockFace.SOUTH) {
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

    }, XMaterial.GRINDSTONE.parseMaterial()),

    CHAIN_BLOCK((version, data, x, y, z) -> {
        Chain chain = (Chain) ((WrappedFlatBlock) data).getBlockData();

        switch (chain.getAxis()) {
            case X:
                return new HexCollisionBox(0.0D, 6.5D, 6.5D, 16.0D, 9.5D, 9.5D);
            case Y:
                return new HexCollisionBox(6.5D, 0.0D, 6.5D, 9.5D, 16.0D, 9.5D);
            default:
            case Z:
                return new HexCollisionBox(6.5D, 6.5D, 0.0D, 9.5D, 9.5D, 16.0D);
        }

    }, XMaterial.CHAIN.parseMaterial()),

    SWEET_BERRY((version, data, x, y, z) -> {
        Ageable berry = (Ageable) ((WrappedFlatBlock) data).getBlockData();

        if (berry.getAge() == 0) {
            return new HexCollisionBox(3.0D, 0.0D, 3.0D, 13.0D, 8.0D, 13.0D);
        }

        return new HexCollisionBox(1.0D, 0.0D, 1.0D, 15.0D, 16.0D, 15.0D);
    }, XMaterial.SWEET_BERRY_BUSH.parseMaterial()),

    CHORUS_FLOWER(new DynamicChorusFlower(), XMaterial.CHORUS_FLOWER.parseMaterial()),

    FENCE_GATE((version, data, x, y, z) -> {
        WrappedFenceGate gate = (WrappedFenceGate) data;

        if (gate.isOpen())
            return NoCollisionBox.INSTANCE;

        switch (gate.getDirection()) {
            case NORTH:
            case SOUTH:
                return new SimpleCollisionBox(0.0F, 0.0F, 0.375F, 1.0F, 1.5F, 0.625F);
            case WEST:
            case EAST:
                return new SimpleCollisionBox(0.375F, 0.0F, 0.0F, 0.625F, 1.5F, 1.0F);
        }

        // This code is unreachable but the compiler does not know this
        return NoCollisionBox.INSTANCE;

    }, Arrays.stream(XMaterial.values()).filter(mat -> mat.name().contains("FENCE") && mat.name().contains("GATE"))
            .map(XMaterial::parseMaterial)
            .toArray(Material[]::new)),


    FENCE(new DynamicFence(), Arrays.stream(XMaterial.values()).filter(mat -> mat.name().contains("FENCE") && !mat.name().contains("GATE"))
            .map(XMaterial::parseMaterial)
            .toArray(Material[]::new)),


    PANE(new DynamicPane(), Arrays.stream(XMaterial.values()).filter(mat -> mat.name().contains("GLASS_PANE") || mat.name().equals("IRON_BARS"))
            .map(XMaterial::parseMaterial).toArray(Material[]::new)),


    SNOW((version, data, x, y, z) -> {
        WrappedSnow snow = (WrappedSnow) data;

        if (snow.getLayers() == 0)
            return NoCollisionBox.INSTANCE;

        return new SimpleCollisionBox(0, 0, 0, 1, snow.getLayers() * 0.125, 1);
    }, XMaterial.SNOW.parseMaterial()),


    STAIR(new DynamicStair(),
            Arrays.stream(XMaterial.values()).filter(mat -> mat.name().contains("STAIRS"))
                    .map(XMaterial::parseMaterial).toArray(Material[]::new)),


    CHEST(new DynamicChest(), XMaterial.CHEST.parseMaterial(), XMaterial.TRAPPED_CHEST.parseMaterial()),


    ENDER_CHEST(new SimpleCollisionBox(0.0625F, 0.0F, 0.0625F,
            0.9375F, 0.875F, 0.9375F),
            XMaterial.ENDER_CHEST.parseMaterial()),


    ENCHANTING_TABLE(new SimpleCollisionBox(0, 0, 0, 1, 1 - 0.25, 1),
            XMaterial.ENCHANTING_TABLE.parseMaterial()),


    FRAME((version, data, x, y, z) -> {
        WrappedFrame frame = (WrappedFrame) data;
        ComplexCollisionBox complexCollisionBox = new ComplexCollisionBox(new HexCollisionBox(0.0D, 0.0D, 0.0D, 16.0D, 13.0D, 16.0D));

        // 1.12 clients do not differentiate between the eye being in and not for collisions
        if (version.isOrAbove(ProtocolVersion.V1_13) && frame.hasEye()) {
            complexCollisionBox.add(new HexCollisionBox(4.0D, 13.0D, 4.0D, 12.0D, 16.0D, 12.0D));
        }

        return complexCollisionBox;

    }, XMaterial.END_PORTAL_FRAME.parseMaterial()),

    CARPET(new SimpleCollisionBox(0.0F, 0.0F, 0.0F, 1.0F, 0.0625F, 1.0F),
            Arrays.stream(XMaterial.values()).filter(mat -> mat.name().contains("CARPET"))
                    .map(XMaterial::parseMaterial).toArray(Material[]::new)),

    DAYLIGHT(new SimpleCollisionBox(0.0F, 0.0F, 0.0F, 1.0F, 0.375, 1.0F),
            XMaterial.DAYLIGHT_DETECTOR.parseMaterial()),

    FARMLAND((version, data, x, y, z) -> {
        // This will be wrong if a player uses 1.10.0 or 1.10.1, not sure if I can fix this as protocol version is same
        if (version.isOrAbove(ProtocolVersion.V1_10))
            return new HexCollisionBox(0.0D, 0.0D, 0.0D, 16.0D, 15.0D, 16.0D);

        return new SimpleCollisionBox(0, 0, 0, 1, 1, 1);

    }, XMaterial.FARMLAND.parseMaterial()),

    LILYPAD((version, data, x, y, z) -> {
        if (version.isBelow(ProtocolVersion.V1_9))
            return new SimpleCollisionBox(0.0f, 0.0F, 0.0f, 1.0f, 0.015625F, 1.0f);
        return new HexCollisionBox(1.0D, 0.0D, 1.0D, 15.0D, 1.5D, 15.0D);
    }, XMaterial.LILY_PAD.parseMaterial()),

    BED(new SimpleCollisionBox(0.0F, 0.0F, 0.0F, 1.0F, 0.5625, 1.0F),
            Arrays.stream(XMaterial.values()).filter(mat -> mat.name().contains("BED") && !mat.name().contains("ROCK"))
                    .map(XMaterial::parseMaterial).toArray(Material[]::new)),

    TRAPDOOR(new TrapDoorHandler(), Arrays.stream(Material.values())
            .filter(mat -> mat.name().contains("TRAP_DOOR") || mat.name().contains("TRAPDOOR")).toArray(Material[]::new)),


    DIODES(new SimpleCollisionBox(0.0F, 0.0F, 0.0F, 1.0F, 0.125F, 1.0F),
            matchLegacy("LEGACY_DIODE_BLOCK_OFF"), matchLegacy("LEGACY_DIODE_BLOCK_ON"),
            matchLegacy("LEGACY_REDSTONE_COMPARATOR_ON"), matchLegacy("LEGACY_REDSTONE_COMPARATOR_OFF"),
            XMaterial.REPEATER.parseMaterial(), XMaterial.COMPARATOR.parseMaterial()),

    STRUCTURE_VOID(new SimpleCollisionBox(0.375, 0.375, 0.375,
            0.625, 0.625, 0.625),
            XMaterial.STRUCTURE_VOID.parseMaterial()),

    END_ROD((version, data, x, y, z) -> {
        WrappedDirectional directional = (WrappedDirectional) data;

        switch (directional.getDirection()) {
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

    }, XMaterial.END_ROD.parseMaterial()),

    CAULDRON((version, data, x, y, z) -> {
        double height = 0.25;

        if (version.isOrAbove(ProtocolVersion.V1_13))
            height = 0.3125;

        return new ComplexCollisionBox(
                new SimpleCollisionBox(0, 0, 0, 1, height, 1),
                new SimpleCollisionBox(0, height, 0, 0.125, 1, 1),
                new SimpleCollisionBox(1 - 0.125, height, 0, 1, 1, 1),
                new SimpleCollisionBox(0, height, 0, 1, 1, 0.125),
                new SimpleCollisionBox(0, height, 1 - 0.125, 1, 1, 1));
    }, XMaterial.CAULDRON.parseMaterial()),

    CACTUS(new SimpleCollisionBox(0.0625, 0, 0.0625,
            1 - 0.0625, 1 - 0.0625, 1 - 0.0625), XMaterial.CACTUS.parseMaterial()),


    PISTON_BASE(new PistonBaseCollision(), m(XMaterial.PISTON), m(XMaterial.STICKY_PISTON)),

    PISTON_ARM(new PistonHeadCollision(), m(XMaterial.PISTON_HEAD)),

    SOULSAND(new SimpleCollisionBox(0, 0, 0, 1, 0.875, 1),
            XMaterial.SOUL_SAND.parseMaterial()),

    PICKLE((version, data, x, y, z) -> {
        SeaPickle pickle = (SeaPickle) ((WrappedFlatBlock) data).getBlockData();

        switch (pickle.getPickles()) {
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

    }, XMaterial.SEA_PICKLE.parseMaterial()),

    TURTLEEGG((version, data, x, y, z) -> {
        TurtleEgg egg = (TurtleEgg) ((WrappedFlatBlock) data).getBlockData();

        if (egg.getEggs() == 1) {
            return new HexCollisionBox(3.0D, 0.0D, 3.0D, 12.0D, 7.0D, 12.0D);
        }

        return new HexCollisionBox(1.0D, 0.0D, 1.0D, 15.0D, 7.0D, 15.0D);
    }, XMaterial.TURTLE_EGG.parseMaterial()),

    CONDUIT((version, data, x, y, z) -> {
        return new HexCollisionBox(5.0D, 5.0D, 5.0D, 11.0D, 11.0D, 11.0D);
    }, XMaterial.CONDUIT.parseMaterial()),

    POT(new HexCollisionBox(5.0D, 0.0D, 5.0D, 11.0D, 6.0D, 11.0D),
            Arrays.stream(Material.values()).filter(mat -> mat.name().contains("POTTED") || mat.name().contains("FLOWER_POT")).toArray(Material[]::new)),


    WALL_SIGN((version, data, x, y, z) -> {
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


    // The nether signes map to sign post and other regular sign
    SIGN(new SimpleCollisionBox(0.25, 0.0, 0.25, 0.75, 1.0, 0.75),
            Arrays.stream(XMaterial.values()).filter(mat -> mat.name().contains("SIGN") && !mat.name().contains("WALL"))
                    .map(XMaterial::parseMaterial).toArray(Material[]::new)),


    BUTTON((version, data, x, y, z) -> {
        WrappedButton button = (WrappedButton) data;
        double f2 = (float) (button.isPowered() ? 1 : 2) / 16.0;

        switch (button.getDirection()) {
            case WEST:
                return new SimpleCollisionBox(0.0, 0.375, 0.3125, f2, 0.625, 0.6875);
            case EAST:
                return new SimpleCollisionBox(1.0 - f2, 0.375, 0.3125, 1.0, 0.625, 0.6875);
            case NORTH:
                return new SimpleCollisionBox(0.3125, 0.375, 0.0, 0.6875, 0.625, f2);
            case SOUTH:
                return new SimpleCollisionBox(0.3125, 0.375, 1.0 - f2, 0.6875, 0.625, 1.0);
            case DOWN:
                return new SimpleCollisionBox(0.3125, 0.0, 0.375, 0.6875, 0.0 + f2, 0.625);
            case UP:
                return new SimpleCollisionBox(0.3125, 1.0 - f2, 0.375, 0.6875, 1.0, 0.625);
        }

        return NoCollisionBox.INSTANCE;

    }, Arrays.stream(Material.values()).filter(mat -> mat.name().contains("BUTTON")).toArray(Material[]::new)),

    LEVER((version, data, x, y, z) -> {
        double f = 0.1875;

        switch (((WrappedDirectional) data).getDirection()) {
            case WEST:
                return new SimpleCollisionBox(1.0 - f * 2.0, 0.2, 0.5 - f, 1.0, 0.8, 0.5 + f);
            case EAST:
                return new SimpleCollisionBox(0.0, 0.2, 0.5 - f, f * 2.0, 0.8, 0.5 + f);
            case NORTH:
                return new SimpleCollisionBox(0.5 - f, 0.2, 1.0 - f * 2.0, 0.5 + f, 0.8, 1.0);
            case SOUTH:
                return new SimpleCollisionBox(0.5 - f, 0.2, 0.0, 0.5 + f, 0.8, f * 2.0);
            case DOWN:
                return new SimpleCollisionBox(0.25, 0.4, 0.25, 0.75, 1.0, 0.75);
            case UP:
                return new SimpleCollisionBox(0.25, 0.0, 0.25, 0.75, 0.6, 0.75);
        }

        return NoCollisionBox.INSTANCE;

    }, XMaterial.LEVER.parseMaterial()),

    TORCH(new HexCollisionBox(6.0D, 0.0D, 6.0D, 10.0D, 10.0D, 10.0D),
            XMaterial.TORCH.parseMaterial(), XMaterial.REDSTONE_TORCH.parseMaterial()),

    WALL_TORCH((version, data, x, y, z) -> {
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

    }, XMaterial.WALL_TORCH.parseMaterial(), XMaterial.REDSTONE_WALL_TORCH.parseMaterial()),

    RAILS((version, data, x, y, z) -> {
        WrappedRails rail = (WrappedRails) data;

        if (rail.isAscending()) {
            return new HexCollisionBox(0.0D, 0.0D, 0.0D, 16.0D, 8.0D, 16.0D);
        }

        return new HexCollisionBox(0.0D, 0.0D, 0.0D, 16.0D, 2.0D, 16.0D);

    }, Arrays.stream(Material.values()).filter(mat -> mat.name().contains("RAIL")).toArray(Material[]::new)),


    // TODO: Some of these blocks have a collision box, fix them for the interact check
    NONE(NoCollisionBox.INSTANCE,
            XMaterial.REDSTONE_WIRE.parseMaterial(), XMaterial.POWERED_RAIL.parseMaterial(),
            XMaterial.RAIL.parseMaterial(), XMaterial.ACTIVATOR_RAIL.parseMaterial(), XMaterial.DETECTOR_RAIL.parseMaterial(), XMaterial.AIR.parseMaterial(), XMaterial.TALL_GRASS.parseMaterial(),
            XMaterial.TRIPWIRE.parseMaterial(), XMaterial.TRIPWIRE_HOOK.parseMaterial()),

    NONE2(NoCollisionBox.INSTANCE,
            Arrays.stream(XMaterial.values()).filter(mat -> mat.name().contains("_PLATE"))
                    .map(XMaterial::parseMaterial).toArray(Material[]::new)),

    DEFAULT(new SimpleCollisionBox(0, 0, 0, 1, 1, 1),
            XMaterial.STONE.parseMaterial());

    // TODO: Some version increased the amount of bounding boxes of this block by an insane amount
    private static final CollisionData[] lookup = new CollisionData[Material.values().length];

    static {
        for (CollisionData data : values()) {
            for (Material mat : data.materials) lookup[mat.ordinal()] = data;
        }
    }

    private final Material[] materials;
    private CollisionBox box;
    private CollisionFactory dynamic;

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

    public static CollisionData getData(Material material) {
        // Material matched = MiscUtils.match(material.toString());
        CollisionData data = lookup[material.ordinal()];
        // _DEFAULT for second thing
        return data != null ? data : DEFAULT;
    }

    private static Material m(XMaterial xmat) {
        return xmat.parseMaterial();
    }

    public CollisionBox getMovementCollisionBox(BaseBlockState block, int x, int y, int z, ProtocolVersion version) {
        WrappedBlockDataValue blockData = WrappedBlockData.getMaterialData(block.getMaterial());
        blockData.getData(block);

        if (!Materials.checkFlag(block.getMaterial(), Materials.SOLID))
            return NoCollisionBox.INSTANCE;

        if (this.box != null)
            return this.box.copy().offset(x, y, z);
        return new DynamicCollisionBox(dynamic, blockData, version).offset(x, y, z);
    }
}
