package ac.grim.grimac.utils.nmsImplementations;

import ac.grim.grimac.utils.collisions.CollisionBox;
import ac.grim.grimac.utils.collisions.blocks.*;
import ac.grim.grimac.utils.collisions.types.*;
import ac.grim.grimac.utils.data.ProtocolVersion;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.*;
import org.bukkit.block.data.type.*;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public enum CollisionData {
    _VINE(new CollisionFactory() {
        @Override
        public CollisionBox fetch(ProtocolVersion version, byte data, int x, int y, int z) {
            switch (data) {
                // TODO: Vines attaching to the top of blocks probably doesn't affect much, but is client sided
                // ViaVersion doesn't allow placing stuff at the top on 1.12 servers
                // But... on 1.13+ clients on 1.12 servers... GUESS WHAT??
                // Vines don't attach to the top of blocks at all, even when they should!
                // Doesn't affect much but could break some interaction checks.

                // South
                case (1):
                    return new SimpleCollisionBox(0., 0., 0.9375, 1., 1., 1.);
                // West
                case (2):
                    return new SimpleCollisionBox(0., 0., 0., 0.0625, 1., 1.);
                // North
                case (4):
                    return new SimpleCollisionBox(0., 0., 0., 1., 1., 0.0625);
                // East
                case (8):
                    return new SimpleCollisionBox(0.9375, 0., 0., 1., 1., 1.);
            }
            return new SimpleCollisionBox(0, 0, 0, 1., 1., 1.);
        }

        @Override
        public CollisionBox fetch(ProtocolVersion version, BlockData block, int x, int y, int z) {
            MultipleFacing facing = (MultipleFacing) block;
            ComplexCollisionBox boxes = new ComplexCollisionBox();

            for (BlockFace face : facing.getFaces()) {
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
        }
    }, XMaterial.VINE.parseMaterial()),


    _LIQUID(new SimpleCollisionBox(0, 0, 0, 1f, 0.9f, 1f),
            XMaterial.WATER.parseMaterial(), XMaterial.LAVA.parseMaterial()),

    _BREWINGSTAND(new ComplexCollisionBox(
            new SimpleCollisionBox(0, 0, 0, 1, 0.125, 1),                      //base
            new SimpleCollisionBox(0.4375, 0.0, 0.4375, 0.5625, 0.875, 0.5625) //top
    ), XMaterial.BREWING_STAND.parseMaterial()),

    _RAIL(new SimpleCollisionBox(0, 0, 0, 1, 0.125, 0),
            XMaterial.RAIL.parseMaterial(), XMaterial.ACTIVATOR_RAIL.parseMaterial(),
            XMaterial.DETECTOR_RAIL.parseMaterial(), XMaterial.POWERED_RAIL.parseMaterial()),

    _ANVIL(new CollisionFactory() {
        // TODO: Some version increased the amount of bounding boxes of this block by an insane amount
        @Override
        public CollisionBox fetch(ProtocolVersion version, byte data, int x, int y, int z) {

            // Anvil collision box was changed in 1.13 to be more accurate
            // https://www.mcpk.wiki/wiki/Version_Differences
            // The base is 0.75×0.75, and its floor is 0.25b high.
            // The top is 1×0.625, and its ceiling is 0.375b low.
            if (version.isOrAbove(ProtocolVersion.V1_13)) {
                ComplexCollisionBox complexAnvil = new ComplexCollisionBox();
                // Base of the anvil
                complexAnvil.add(new HexCollisionBox(2, 0, 2, 14, 4, 14));

                switch (data & 0b01) {
                    // North and South top
                    case (0):
                        complexAnvil.add(new HexCollisionBox(4.0D, 4.0D, 3.0D, 12.0D, 5.0D, 13.0D));
                        complexAnvil.add(new HexCollisionBox(6.0D, 5.0D, 4.0D, 10.0D, 10.0D, 12.0D));
                        complexAnvil.add(new HexCollisionBox(3.0D, 10.0D, 0.0D, 13.0D, 16.0D, 16.0D));
                        break;
                    // East and West top
                    case (1):
                        complexAnvil.add(new HexCollisionBox(3.0D, 4.0D, 4.0D, 13.0D, 5.0D, 12.0D));
                        complexAnvil.add(new HexCollisionBox(4.0D, 5.0D, 6.0D, 12.0D, 10.0D, 10.0D));
                        complexAnvil.add(new HexCollisionBox(0.0D, 10.0D, 3.0D, 16.0D, 16.0D, 13.0D));
                }

                return complexAnvil;
            } else {
                // Just a single solid collision box with 1.12
                switch (data & 0b01) {
                    // North and South
                    case (0):
                        return new SimpleCollisionBox(0.125F, 0.0F, 0.0F, 0.875F, 1.0F, 1.0F);
                    // East and West
                    case (1):
                        return new SimpleCollisionBox(0.0F, 0.0F, 0.125F, 1.0F, 1.0F, 0.875F);
                }
            }

            // This should never run.
            return new SimpleCollisionBox(0, 0, 0, 1, 1, 1);
        }

        @Override
        public CollisionBox fetch(ProtocolVersion version, BlockData block, int x, int y, int z) {
            Directional facing = (Directional) block;

            // Making exemption for not using legacy stuff in modern stuff, as we are doing our own byte conversion
            if (facing.getFacing() == BlockFace.EAST || facing.getFacing() == BlockFace.WEST) {
                return fetch(version, (byte) 1, x, y, z);
            } else {
                // Must be North, South, or a bad server jar
                return fetch(version, (byte) 0, x, y, z);
            }
        }
    }, XMaterial.ANVIL.parseMaterial(), XMaterial.CHIPPED_ANVIL.parseMaterial(), XMaterial.DAMAGED_ANVIL.parseMaterial()),


    _WALL(new DynamicWall(), Arrays.stream(XMaterial.values()).filter(mat -> mat.name().contains("WALL")
            && !mat.name().contains("SIGN") && !mat.name().contains("HEAD") && !mat.name().contains("BANNER")
            && !mat.name().contains("FAN") && !mat.name().contains("SKULL") && !mat.name().contains("TORCH"))
            .map(XMaterial::parseMaterial)
            .toArray(Material[]::new)),


    _SLAB(new CollisionFactory() {
        @Override
        public CollisionBox fetch(ProtocolVersion version, byte data, int x, int y, int z) {
            if ((data & 8) == 0) {
                return new SimpleCollisionBox(0, 0, 0, 1, 0.5, 1);
            }

            return new SimpleCollisionBox(0, 0.5, 0, 1, 1, 1);
        }

        @Override
        public CollisionBox fetch(ProtocolVersion version, BlockData block, int x, int y, int z) {
            Slab slab = (Slab) block;

            if (slab.getType() == Slab.Type.BOTTOM) {
                return new SimpleCollisionBox(0, 0, 0, 1, 0.5, 1);
            } else if (slab.getType() == Slab.Type.TOP) {
                return new SimpleCollisionBox(0, 0.5, 0, 1, 1, 1);
            }

            return new SimpleCollisionBox(0, 0, 0, 1, 1, 1);
        }
        // 1.13 can handle double slabs as it's in the block data
        // 1.12 has double slabs as a separate block, no block data to differentiate it
    }, Arrays.stream(XMaterial.values()).filter(mat -> mat.name().contains("_SLAB"))
            .map(XMaterial::parseMaterial).filter(m -> !m.name().contains("DOUBLE")).toArray(Material[]::new)),

    // Note, getting legacy byte seems broken for skulls
    _WALL_SKULL(new CollisionFactory() {
        @Override
        public CollisionBox fetch(ProtocolVersion version, byte data, int x, int y, int z) {
            int rotation = data & 7;
            switch (rotation) {
                // Normal on floor - for any rotation.
                case 1:
                default:
                    return new SimpleCollisionBox(0.25F, 0.0F, 0.25F, 0.75F, 0.5F, 0.75F);
                // Facing north
                case 2:
                    return new SimpleCollisionBox(0.25F, 0.25F, 0.5F, 0.75F, 0.75F, 1.0F);
                // Facing south
                case 3:
                    return new SimpleCollisionBox(0.25F, 0.25F, 0.0F, 0.75F, 0.75F, 0.5F);
                // Facing west
                case 4:
                    return new SimpleCollisionBox(0.5F, 0.25F, 0.25F, 1.0F, 0.75F, 0.75F);
                // Facing east
                case 5:
                    return new SimpleCollisionBox(0.0F, 0.25F, 0.25F, 0.5F, 0.75F, 0.75F);
            }
        }

        // Note that this is for stuff on walls and not regular skull blocks
        @Override
        public CollisionBox fetch(ProtocolVersion version, BlockData block, int x, int y, int z) {
            Directional skullDir = (Directional) block;

            switch (skullDir.getFacing()) {
                // Heads on walls cannot have diagonal rotations
                default:
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
        }
    }, XMaterial.SKELETON_WALL_SKULL.parseMaterial(), XMaterial.WITHER_SKELETON_WALL_SKULL.parseMaterial(),
            XMaterial.CREEPER_WALL_HEAD.parseMaterial(), XMaterial.DRAGON_WALL_HEAD.parseMaterial(), // Yes, the dragon head has the same collision box as regular heads
            XMaterial.PLAYER_WALL_HEAD.parseMaterial(), XMaterial.ZOMBIE_WALL_HEAD.parseMaterial()),


    _SKULL(new SimpleCollisionBox(0.25, 0, 0.25, 0.75, 0.5, 0.75), XMaterial.SKELETON_SKULL.parseMaterial(), XMaterial.WITHER_SKELETON_SKULL.parseMaterial(),
            XMaterial.CREEPER_HEAD.parseMaterial(), XMaterial.DRAGON_HEAD.parseMaterial(), // Yes, the dragon head has the same collision box as regular heads
            XMaterial.PLAYER_HEAD.parseMaterial(), XMaterial.ZOMBIE_HEAD.parseMaterial()),


    _DOOR(new DoorHandler(), Arrays.stream(XMaterial.values()).filter(mat -> mat.name().contains("_DOOR"))
            .map(XMaterial::parseMaterial).toArray(Material[]::new)),

    _HOPPER(new ComplexCollisionBox(
            new SimpleCollisionBox(0, 0, 0, 1, 0.125 * 5, 1),
            new SimpleCollisionBox(0, 0.125 * 5, 0, 0.125, 1, 1),
            new SimpleCollisionBox(1 - 0.125, 0.125 * 5, 0, 1, 1, 1),
            new SimpleCollisionBox(0, 0.125 * 5, 0, 1, 1, 0.125),
            new SimpleCollisionBox(0, 0.125 * 5, 1 - 0.125, 1, 1, 1)
    ), XMaterial.HOPPER.parseMaterial()),

    _CAKE(new CollisionFactory() {
        // Byte is the number of bytes eaten.
        @Override
        public CollisionBox fetch(ProtocolVersion version, byte data, int x, int y, int z) {
            double slicesEaten = (1 + data * 2) / 16D;
            return new SimpleCollisionBox(slicesEaten, 0, 0.0625, 1 - 0.0625, 0.5, 1 - 0.0625);
        }

        // Note that this is for stuff on walls and not regular skull blocks
        @Override
        public CollisionBox fetch(ProtocolVersion version, BlockData block, int x, int y, int z) {
            Cake cake = (Cake) block;
            return fetch(version, (byte) cake.getBites(), x, y, z);
        }
    }, XMaterial.CAKE.parseMaterial()),


    _LADDER(new CollisionFactory() {
        @Override
        public CollisionBox fetch(ProtocolVersion version, byte data, int x, int y, int z) {
            if (data == 2) { // North
                return new HexCollisionBox(0.0D, 0.0D, 13.0D, 16.0D, 16.0D, 16.0D);
            } else if (data == 3) { // South
                return new HexCollisionBox(0.0D, 0.0D, 0.0D, 16.0D, 16.0D, 3.0D);
            } else if (data == 4) { // West
                return new HexCollisionBox(13.0D, 0.0D, 0.0D, 16.0D, 16.0D, 16.0D);
            } else if (data == 5) { // East
                return new HexCollisionBox(0.0D, 0.0D, 0.0D, 3.0D, 16.0D, 16.0D);
            }

            // This code is unreachable but the compiler does not know this
            return NoCollisionBox.INSTANCE;
        }

        // Note that this is for stuff on walls and not regular skull blocks
        @Override
        public CollisionBox fetch(ProtocolVersion version, BlockData block, int x, int y, int z) {
            Directional ladder = (Directional) block;

            switch (ladder.getFacing()) {
                case NORTH:
                    return new HexCollisionBox(0.0D, 0.0D, 13.0D, 16.0D, 16.0D, 16.0D);
                case SOUTH:
                    return new HexCollisionBox(0.0D, 0.0D, 0.0D, 16.0D, 16.0D, 3.0D);
                case WEST:
                    return new HexCollisionBox(13.0D, 0.0D, 0.0D, 16.0D, 16.0D, 16.0D);
                case EAST:
                    return new HexCollisionBox(0.0D, 0.0D, 0.0D, 3.0D, 16.0D, 16.0D);
            }

            // This code is unreachable but the compiler does not know this
            return NoCollisionBox.INSTANCE;
        }
    }, XMaterial.LADDER.parseMaterial()),

    _CAMPFIRE(new HexCollisionBox(0.0D, 0.0D, 0.0D, 16.0D, 7.0D, 16.0D), XMaterial.CAMPFIRE.parseMaterial(), XMaterial.SOUL_CAMPFIRE.parseMaterial()),

    _LANTERN(new CollisionFactory() {
        @Override
        public CollisionBox fetch(ProtocolVersion version, byte data, int x, int y, int z) {
            // Block only exists in 1.14+
            return null;
        }

        @Override
        public CollisionBox fetch(ProtocolVersion version, BlockData block, int x, int y, int z) {
            Lantern lantern = (Lantern) block;

            if (lantern.isHanging()) {
                return new ComplexCollisionBox(new HexCollisionBox(5.0D, 1.0D, 5.0D, 11.0D, 8.0D, 11.0D),
                        new HexCollisionBox(6.0D, 8.0D, 6.0D, 10.0D, 10.0D, 10.0D));
            }

            return new ComplexCollisionBox(new HexCollisionBox(5.0D, 0.0D, 5.0D, 11.0D, 7.0D, 11.0D),
                    new HexCollisionBox(6.0D, 7.0D, 6.0D, 10.0D, 9.0D, 10.0D));
        }
    }, XMaterial.LANTERN.parseMaterial(), XMaterial.SOUL_LANTERN.parseMaterial()),


    _LECTERN(new CollisionFactory() {
        @Override
        public CollisionBox fetch(ProtocolVersion version, byte data, int x, int y, int z) {
            // 1.14+ block
            return null;
        }

        @Override
        public CollisionBox fetch(ProtocolVersion version, BlockData block, int x, int y, int z) {
            // I'm not sure why the top plate isn't applied, wrongly named variable or special modern collision stuff?
            // new HexCollisionBox(0.0D, 15.0D, 0.0D, 16.0D, 15.0D, 16.0D) - Top plate
            return new ComplexCollisionBox(
                    new HexCollisionBox(0.0D, 0.0D, 0.0D, 16.0D, 2.0D, 16.0D), // base
                    new HexCollisionBox(4.0D, 2.0D, 4.0D, 12.0D, 14.0D, 12.0D) // post
            );
        }
    }, XMaterial.LECTERN.parseMaterial()),


    _HONEY_BLOCK(new CollisionFactory() {
        @Override
        public CollisionBox fetch(ProtocolVersion version, byte data, int x, int y, int z) {
            // 1.15+ block
            return null;
        }

        @Override
        public CollisionBox fetch(ProtocolVersion version, BlockData block, int x, int y, int z) {
            return new HexCollisionBox(1.0D, 0.0D, 1.0D, 15.0D, 15.0D, 15.0D);
        }
    }, XMaterial.HONEY_BLOCK.parseMaterial()),


    _DRAGON_EGG_BLOCK(new HexCollisionBox(1.0D, 0.0D, 1.0D, 15.0D, 16.0D, 15.0D), XMaterial.DRAGON_EGG.parseMaterial()),

    _GRINDSTONE(new CollisionFactory() {
        @Override
        public CollisionBox fetch(ProtocolVersion version, byte data, int x, int y, int z) {
            // 1.14+ block
            return null;
        }

        @Override
        public CollisionBox fetch(ProtocolVersion version, BlockData block, int x, int y, int z) {
            Grindstone grindstone = (Grindstone) block;

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
        }
    }, XMaterial.GRINDSTONE.parseMaterial()),

    _CHAIN_BLOCK(new CollisionFactory() {
        @Override
        public CollisionBox fetch(ProtocolVersion version, byte data, int x, int y, int z) {
            return null;
        }

        @Override
        public CollisionBox fetch(ProtocolVersion version, BlockData block, int x, int y, int z) {
            Chain chain = (Chain) block;

            switch (chain.getAxis()) {
                case X:
                    return new HexCollisionBox(0.0D, 6.5D, 6.5D, 16.0D, 9.5D, 9.5D);
                case Y:
                    return new HexCollisionBox(6.5D, 0.0D, 6.5D, 9.5D, 16.0D, 9.5D);
                case Z:
                    return new HexCollisionBox(6.5D, 6.5D, 0.0D, 9.5D, 9.5D, 16.0D);
            }

            return null;
        }
    }, XMaterial.CHAIN.parseMaterial()),

    _SWEET_BERRY(new CollisionFactory() {
        @Override
        public CollisionBox fetch(ProtocolVersion version, byte data, int x, int y, int z) {
            // 1.14 only block
            return null;
        }

        @Override
        public CollisionBox fetch(ProtocolVersion version, BlockData block, int x, int y, int z) {
            Ageable berry = (Ageable) block;

            if (berry.getAge() == 0) {
                return new HexCollisionBox(3.0D, 0.0D, 3.0D, 13.0D, 8.0D, 13.0D);
            }

            return new HexCollisionBox(1.0D, 0.0D, 1.0D, 15.0D, 16.0D, 15.0D);
        }
    }, XMaterial.SWEET_BERRY_BUSH.parseMaterial()),

    _FENCE_GATE(new CollisionFactory() {
        @Override
        public CollisionBox fetch(ProtocolVersion version, byte data, int x, int y, int z) {
            CollisionBox box = NoCollisionBox.INSTANCE;

            // Byte format - 0x1, 0x2 facing direction. 0x4 gate open/closed. 1 if open.
            if ((data & 0x4) == 0) {
                if (data == 0 || data == 2) {
                    // Facing north or south
                    box = new SimpleCollisionBox(0.0F, 0.0F, 0.375F, 1.0F, 1.5F, 0.625F);
                } else {
                    box = new SimpleCollisionBox(0.375F, 0.0F, 0.0F, 0.625F, 1.5F, 1.0F);
                }
            }
            return box;
        }

        @Override
        public CollisionBox fetch(ProtocolVersion version, BlockData block, int x, int y, int z) {
            Gate gate = (Gate) block;

            if (gate.isOpen())
                return NoCollisionBox.INSTANCE;

            switch (gate.getFacing()) {
                case NORTH:
                case SOUTH:
                    return new SimpleCollisionBox(0.0F, 0.0F, 0.375F, 1.0F, 1.5F, 0.625F);
                case WEST:
                case EAST:
                    return new SimpleCollisionBox(0.375F, 0.0F, 0.0F, 0.625F, 1.5F, 1.0F);
            }

            // This code is unreachable but the compiler does not know this
            return NoCollisionBox.INSTANCE;
        }
    }, Arrays.stream(XMaterial.values()).filter(mat -> mat.name().contains("FENCE") && mat.name().contains("GATE"))
            .map(XMaterial::parseMaterial)
            .toArray(Material[]::new)),


    _FENCE(new DynamicFence(), Arrays.stream(XMaterial.values()).filter(mat -> mat.name().contains("FENCE") && !mat.name().contains("GATE"))
            .map(XMaterial::parseMaterial)
            .toArray(Material[]::new)),


    _PANE(new DynamicPane(), Arrays.stream(XMaterial.values()).filter(mat -> mat.name().contains("GLASS_PANE") || mat.name().equals("IRON_BARS"))
            .map(XMaterial::parseMaterial).toArray(Material[]::new)),


    _SNOW(new CollisionFactory() {
        @Override
        public CollisionBox fetch(ProtocolVersion version, byte data, int x, int y, int z) {
            // Byte format = number of layers of snow
            if (data == 0)
                return NoCollisionBox.INSTANCE;

            return new SimpleCollisionBox(0, 0, 0, 1, data * 0.125, 1);
        }

        @Override
        public CollisionBox fetch(ProtocolVersion version, BlockData block, int x, int y, int z) {
            Snow snow = (Snow) block;
            return fetch(version, (byte) (snow.getLayers() - 1), x, y, z);
        }
    }, XMaterial.SNOW.parseMaterial()),


    _STAIR(new DynamicStair(),
            Arrays.stream(XMaterial.values()).filter(mat -> mat.name().contains("STAIRS"))
                    .map(XMaterial::parseMaterial).toArray(Material[]::new)),


    _CHEST(new DynamicChest(), XMaterial.CHEST.parseMaterial(), XMaterial.TRAPPED_CHEST.parseMaterial()),


    _ENDERCHEST(new SimpleCollisionBox(0.0625F, 0.0F, 0.0625F,
            0.9375F, 0.875F, 0.9375F),
            XMaterial.ENDER_CHEST.parseMaterial()),


    _ETABLE(new SimpleCollisionBox(0, 0, 0, 1, 1 - 0.25, 1),
            XMaterial.ENCHANTING_TABLE.parseMaterial()),


    // TODO: This actually depends on client version?
    _FRAME(new SimpleCollisionBox(0, 0, 0, 1, 1 - (0.0625 * 3), 1),
            XMaterial.END_PORTAL_FRAME.parseMaterial()),

    _CARPET(new SimpleCollisionBox(0.0F, 0.0F, 0.0F, 1.0F, 0.0625F, 1.0F),
            Arrays.stream(XMaterial.values()).filter(mat -> mat.name().contains("CARPET"))
                    .map(XMaterial::parseMaterial).toArray(Material[]::new)),

    _Daylight(new SimpleCollisionBox(0.0F, 0.0F, 0.0F, 1.0F, 0.375, 1.0F),
            XMaterial.DAYLIGHT_DETECTOR.parseMaterial()),


    _LILIPAD(new CollisionFactory() {
        @Override
        public CollisionBox fetch(ProtocolVersion version, byte data, int x, int y, int z) {
            if (version.isBelow(ProtocolVersion.V1_9))
                return new SimpleCollisionBox(0.0f, 0.0F, 0.0f, 1.0f, 0.015625F, 1.0f);
            return new SimpleCollisionBox(0.0625, 0.0F, 0.0625, 0.9375, 0.015625F, 0.9375);
        }

        @Override
        public CollisionBox fetch(ProtocolVersion version, BlockData block, int x, int y, int z) {
            return fetch(version, (byte) 0, x, y, z);
        }
    }, XMaterial.LILY_PAD.parseMaterial()),

    _BED(new SimpleCollisionBox(0.0F, 0.0F, 0.0F, 1.0F, 0.5625, 1.0F),
            Arrays.stream(XMaterial.values()).filter(mat -> mat.name().contains("BED") && !mat.name().contains("ROCK"))
                    .map(XMaterial::parseMaterial).toArray(Material[]::new)),

    _TRAPDOOR(new TrapDoorHandler(), Arrays.stream(Material.values())
            .filter(mat -> mat.name().contains("TRAP_DOOR")).toArray(Material[]::new)),


    _STUPID(new SimpleCollisionBox(0.0F, 0.0F, 0.0F, 1.0F, 0.125F, 1.0F),
            matchLegacy("LEGACY_DIODE_BLOCK_OFF"), matchLegacy("LEGACY_DIODE_BLOCK_ON"),
            matchLegacy("LEGACY_REDSTONE_COMPARATOR_ON"), matchLegacy("LEGACY_REDSTONE_COMPARATOR_OFF"),
            XMaterial.REPEATER.parseMaterial(), XMaterial.COMPARATOR.parseMaterial()),

    _STRUCTURE_VOID(new SimpleCollisionBox(0.375, 0.375, 0.375,
            0.625, 0.625, 0.625),
            XMaterial.STRUCTURE_VOID.parseMaterial()),

    _END_ROD(new DynamicRod(), XMaterial.END_ROD.parseMaterial()),

    _CAULDRON(new ComplexCollisionBox(
            new SimpleCollisionBox(0, 0, 0, 1, 0.3125, 1),
            new SimpleCollisionBox(0, 0.3125, 0, 0.125, 1, 1),
            new SimpleCollisionBox(1 - 0.125, 0.3125, 0, 1, 1, 1),
            new SimpleCollisionBox(0, 0.3125, 0, 1, 1, 0.125), new SimpleCollisionBox(0, 0.3125, 1 - 0.125, 1, 1, 1)),
            XMaterial.CAULDRON.parseMaterial()),

    _CACTUS(new SimpleCollisionBox(0.0625, 0, 0.0625,
            1 - 0.0625, 1 - 0.0625, 1 - 0.0625), XMaterial.CACTUS.parseMaterial()),


    _PISTON_BASE(new PistonBaseCollision(), m(XMaterial.PISTON), m(XMaterial.STICKY_PISTON)),

    _PISTON_ARM(new PistonHeadCollision(), m(XMaterial.PISTON_HEAD)),

    _SOULSAND(new SimpleCollisionBox(0, 0, 0, 1, 0.875, 1),
            XMaterial.SOUL_SAND.parseMaterial()),

    _PICKLE(new CollisionFactory() {
        @Override
        public CollisionBox fetch(ProtocolVersion version, byte data, int x, int y, int z) {
            // 1.13+ only block
            return NoCollisionBox.INSTANCE;
        }

        @Override
        public CollisionBox fetch(ProtocolVersion version, BlockData block, int x, int y, int z) {
            int pickles = ((SeaPickle) block).getPickles();

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
    }, XMaterial.SEA_PICKLE.parseMaterial()),


    _POT(new HexCollisionBox(5.0D, 0.0D, 5.0D, 11.0D, 6.0D, 11.0D),
            XMaterial.FLOWER_POT.parseMaterial()),


    _WALL_SIGN(new CollisionFactory() {
        @Override
        public CollisionBox fetch(ProtocolVersion version, byte data, int x, int y, int z) {
            // 1.13+ only block
            if (data == 2) { // North
                return new HexCollisionBox(0.0D, 4.5D, 14.0D, 16.0D, 12.5D, 16.0D);
            } else if (data == 3) { // South
                return new HexCollisionBox(0.0D, 4.5D, 0.0D, 16.0D, 12.5D, 2.0D);
            } else if (data == 4) { // West
                return new HexCollisionBox(14.0D, 4.5D, 0.0D, 16.0D, 12.5D, 16.0D);
            } else if (data == 5) { // East
                return new HexCollisionBox(0.0D, 4.5D, 0.0D, 2.0D, 12.5D, 16.0D);
            }

            // Shouldn't be reachable
            return NoCollisionBox.INSTANCE;
        }

        @Override
        public CollisionBox fetch(ProtocolVersion version, BlockData block, int x, int y, int z) {
            WallSign sign = (WallSign) block;

            switch (sign.getFacing()) {
                case NORTH:
                    return fetch(version, (byte) 2, x, y, z);
                case SOUTH:
                    return fetch(version, (byte) 3, x, y, z);
                case WEST:
                    return fetch(version, (byte) 4, x, y, z);
                case EAST:
                    return fetch(version, (byte) 5, x, y, z);
                default:
                    return NoCollisionBox.INSTANCE;
            }
        }
    }, Arrays.stream(Material.values()).filter(mat -> mat.name().contains("WALL_SIGN"))
            .toArray(Material[]::new)),


    // The nether signes map to sign post and other regular sign
    _SIGN(new SimpleCollisionBox(0.25, 0.0, 0.25, 0.75, 1.0, 0.75),
            Arrays.stream(XMaterial.values()).filter(mat -> mat.name().contains("SIGN") && !mat.name().contains("WALL"))
                    .map(XMaterial::parseMaterial).toArray(Material[]::new)),


    _BUTTON(new CollisionFactory() {
        @Override
        public CollisionBox fetch(ProtocolVersion version, byte data, int x, int y, int z) {
            boolean flag = (data & 8) == 8; //is powered;
            double f2 = (float) (flag ? 1 : 2) / 16.0;

            switch (data & 7) {
                case 1:
                    return new SimpleCollisionBox(0.0, 0.375, 0.3125, f2, 0.625, 0.6875);
                case 2:
                    return new SimpleCollisionBox(1.0 - f2, 0.375, 0.3125, 1.0, 0.625, 0.6875);
                case 3:
                    return new SimpleCollisionBox(0.3125, 0.375, 0.0, 0.6875, 0.625, f2);
                case 4:
                    return new SimpleCollisionBox(0.3125, 0.375, 1.0 - f2, 0.6875, 0.625, 1.0);
                case 5:
                    return new SimpleCollisionBox(0.3125, 0.0, 0.375, 0.6875, 0.0 + f2, 0.625);
                case 0:
                    return new SimpleCollisionBox(0.3125, 1.0 - f2, 0.375, 0.6875, 1.0, 0.625);
            }

            return NoCollisionBox.INSTANCE;
        }

        @Override
        public CollisionBox fetch(ProtocolVersion version, BlockData block, int x, int y, int z) {
            BlockFace direction = ((Directional) block).getFacing();
            Powerable powerable = (Powerable) block;

            double f2 = (float) (powerable.isPowered() ? 1 : 2) / 16.0;

            switch (direction) {
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
        }
    }, Arrays.stream(Material.values()).filter(mat -> mat.name().contains("BUTTON")).toArray(Material[]::new)),

    _LEVER(new CollisionFactory() {
        @Override
        public CollisionBox fetch(ProtocolVersion version, byte data, int x, int y, int z) {
            double f = 0.1875;

            switch (data & 7) {
                case 0: // up
                case 7:
                    return new SimpleCollisionBox(0.25, 0.0, 0.25, 0.75, 0.6, 0.75);
                case 1: // west
                    return new SimpleCollisionBox(1.0 - f * 2.0, 0.2, 0.5 - f, 1.0, 0.8, 0.5 + f);
                case 2: // east
                    return new SimpleCollisionBox(0.0, 0.2, 0.5 - f, f * 2.0, 0.8, 0.5 + f);
                case 3: // north
                    return new SimpleCollisionBox(0.5 - f, 0.2, 1.0 - f * 2.0, 0.5 + f, 0.8, 1.0);
                case 4: // south
                    return new SimpleCollisionBox(0.5 - f, 0.2, 0.0, 0.5 + f, 0.8, f * 2.0);
                case 5: // down
                case 6:
                    return new SimpleCollisionBox(0.25, 0.4, 0.25, 0.75, 1.0, 0.75);
                default:
                    return NoCollisionBox.INSTANCE;
            }
        }

        @Override
        public CollisionBox fetch(ProtocolVersion version, BlockData block, int x, int y, int z) {
            BlockFace direction = ((Directional) block).getFacing();
            double f = 0.1875;

            switch (direction) {
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
        }
    }, XMaterial.LEVER.parseMaterial()),

    // TODO: Some of these blocks have a collision box, fix them for the interact check
    _NONE(NoCollisionBox.INSTANCE, XMaterial.TORCH.parseMaterial(), XMaterial.REDSTONE_TORCH.parseMaterial(),
            XMaterial.REDSTONE_WIRE.parseMaterial(), XMaterial.REDSTONE_WALL_TORCH.parseMaterial(), XMaterial.POWERED_RAIL.parseMaterial(), XMaterial.WALL_TORCH.parseMaterial(),
            XMaterial.RAIL.parseMaterial(), XMaterial.ACTIVATOR_RAIL.parseMaterial(), XMaterial.DETECTOR_RAIL.parseMaterial(), XMaterial.AIR.parseMaterial(), XMaterial.TALL_GRASS.parseMaterial(),
            XMaterial.TRIPWIRE.parseMaterial(), XMaterial.TRIPWIRE_HOOK.parseMaterial()),

    _NONE2(NoCollisionBox.INSTANCE,
            Arrays.stream(XMaterial.values()).filter(mat -> mat.name().contains("_PLATE"))
                    .map(XMaterial::parseMaterial).toArray(Material[]::new)),

    _DEFAULT(new SimpleCollisionBox(0, 0, 0, 1, 1, 1),
            XMaterial.STONE.parseMaterial());

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
        return data != null ? data : _DEFAULT;
    }

    private static Material m(XMaterial xmat) {
        return xmat.parseMaterial();
    }

    public static Material matchLegacy(String material) {
        if (ProtocolVersion.getGameVersion().isOrAbove(ProtocolVersion.V1_13)) {
            return null;
        }
        return Material.getMaterial(material.replace("LEGACY_", ""));
    }

    public CollisionBox getBox(BlockData block, int x, int y, int z, ProtocolVersion version) {
        if (this.box != null)
            return this.box.copy().offset(x, y, z);
        return new DynamicCollisionBox(dynamic, block, version).offset(x, y, z);
    }
}
