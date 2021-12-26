package ac.grim.grimac.utils.collisions.blocks.connecting;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.collisions.CollisionData;
import ac.grim.grimac.utils.collisions.datatypes.*;
import ac.grim.grimac.utils.nmsutil.Materials;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.world.BlockFace;
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import org.bukkit.Material;
import org.bukkit.block.data.type.Wall;

public class DynamicWall extends DynamicConnecting implements CollisionFactory {
    // https://bugs.mojang.com/browse/MC-9565
    // https://bugs.mojang.com/browse/MC-94016
    private static final CollisionBox[] COLLISION_BOXES = makeShapes(4.0F, 3.0F, 24.0F, 0.0F, 24.0F, false);
    public static final CollisionBox[] BOXES = makeShapes(4.0F, 3.0F, 16.0F, 0.0F, 16.0F, false);

    public CollisionBox fetchRegularBox(GrimPlayer player, WrappedBlockState state, ClientVersion version, int x, int y, int z) {
        int north, south, west, east, up;
        north = south = west = east = up = 0;

        if (version.isNewerThan(ClientVersion.V_1_12_2)) {
            if (ItemTypes.supports(16)) {
                Wall wall = (Wall) data;

                if (wall.getHeight(BlockFace.NORTH) != Wall.Height.NONE)
                    north += wall.getHeight(BlockFace.NORTH) == Wall.Height.LOW ? 1 : 2;

                if (wall.getHeight(BlockFace.EAST) != Wall.Height.NONE)
                    east += wall.getHeight(BlockFace.EAST) == Wall.Height.LOW ? 1 : 2;

                if (wall.getHeight(BlockFace.SOUTH) != Wall.Height.NONE)
                    south += wall.getHeight(BlockFace.SOUTH) == Wall.Height.LOW ? 1 : 2;

                if (wall.getHeight(BlockFace.WEST) != Wall.Height.NONE)
                    west += wall.getHeight(BlockFace.WEST) == Wall.Height.LOW ? 1 : 2;

                if (wall.isUp())
                    up = 1;
            } else {
                north = facing.getFaces().contains(BlockFace.NORTH) ? 1 : 0;
                east = facing.getFaces().contains(BlockFace.EAST) ? 1 : 0;
                south = facing.getFaces().contains(BlockFace.SOUTH) ? 1 : 0;
                west = facing.getFaces().contains(BlockFace.WEST) ? 1 : 0;
                up = facing.getFaces().contains(BlockFace.UP) ? 1 : 0;
            }
        } else {
            north = connectsTo(player, version, x, y, z, BlockFace.NORTH) ? 1 : 0;
            south = connectsTo(player, version, x, y, z, BlockFace.SOUTH) ? 1 : 0;
            west = connectsTo(player, version, x, y, z, BlockFace.WEST) ? 1 : 0;
            east = connectsTo(player, version, x, y, z, BlockFace.EAST) ? 1 : 0;
            up = 1;
        }

        // On 1.13+ clients the bounding box is much more complicated
        if (player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_13)) {
            ComplexCollisionBox box = new ComplexCollisionBox();

            // Proper and faster way would be to compute all this beforehand
            if (up == 1) {
                box.add(new HexCollisionBox(4, 0, 4, 12, 16, 12));
                return box;
            }

            if (north == 1) {
                box.add(new HexCollisionBox(5, 0, 0.0D, 11, 14, 11));
            } else if (north == 2) {
                box.add(new HexCollisionBox(5, 0, 0, 11, 16, 11));
            }
            if (south == 1) {
                box.add(new HexCollisionBox(5, 0, 5, 11, 14, 16));
            } else if (south == 2) {
                box.add(new HexCollisionBox(5, 0, 5, 11, 16, 16));
            }
            if (west == 1) {
                box.add(new HexCollisionBox(0, 0, 5, 11, 14, 11));
            } else if (west == 2) {
                box.add(new HexCollisionBox(0, 0, 5, 11, 16, 11));
            }
            if (east == 1) {
                box.add(new HexCollisionBox(5, 0, 5, 16, 14, 11));
            } else if (east == 2) {
                box.add(new HexCollisionBox(5, 0, 5, 16, 16, 11));
            }
        }

        // Magic 1.8 code for walls that I copied over, 1.12 below uses this mess
        float f = 0.25F;
        float f1 = 0.75F;
        float f2 = 0.25F;
        float f3 = 0.75F;

        if (north == 1) {
            f2 = 0.0F;
        }

        if (south == 1) {
            f3 = 1.0F;
        }

        if (west == 1) {
            f = 0.0F;
        }

        if (east == 1) {
            f1 = 1.0F;
        }

        if (north == 1 && south == 1 && west != 0 && east != 0) {
            f = 0.3125F;
            f1 = 0.6875F;
        } else if (north != 1 && south != 1 && west == 0 && east == 0) {
            f2 = 0.3125F;
            f3 = 0.6875F;
        }

        return new SimpleCollisionBox(f, 0.0F, f2, f1, 1, f3);
    }

    @Override
    public CollisionBox fetch(GrimPlayer player, ClientVersion version, WrappedBlockDataValue block, int x, int y, int z) {
        boolean north;
        boolean south;
        boolean west;
        boolean east;
        boolean up;

        if (ItemTypes.isNewVersion() && version.isNewerThan(ClientVersion.V_1_12_2)) {
            WrappedMultipleFacing pane = (WrappedMultipleFacing) block;

            east = pane.getDirections().contains(BlockFace.EAST);
            north = pane.getDirections().contains(BlockFace.NORTH);
            south = pane.getDirections().contains(BlockFace.SOUTH);
            west = pane.getDirections().contains(BlockFace.WEST);
            up = pane.getDirections().contains(BlockFace.UP);
        } else {
            north = connectsTo(player, version, x, y, z, BlockFace.NORTH);
            south = connectsTo(player, version, x, y, z, BlockFace.SOUTH);
            west = connectsTo(player, version, x, y, z, BlockFace.WEST);
            east = connectsTo(player, version, x, y, z, BlockFace.EAST);
            up = true;
        }

        // On 1.13+ clients the bounding box is much more complicated
        if (player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_13)) {
            // Proper and faster way would be to compute all this beforehand
            if (up) {
                ComplexCollisionBox box = new ComplexCollisionBox(COLLISION_BOXES[getAABBIndex(north, east, south, west)].copy());
                box.add(new HexCollisionBox(4, 0, 4, 12, 24, 12));
                return box;
            }

            return COLLISION_BOXES[getAABBIndex(north, east, south, west)].copy();
        }

        // Magic 1.8 code for walls that I copied over, 1.12 below uses this mess
        float f = 0.25F;
        float f1 = 0.75F;
        float f2 = 0.25F;
        float f3 = 0.75F;

        if (north) {
            f2 = 0.0F;
        }

        if (south) {
            f3 = 1.0F;
        }

        if (west) {
            f = 0.0F;
        }

        if (east) {
            f1 = 1.0F;
        }

        if (north && south && !west && !east) {
            f = 0.3125F;
            f1 = 0.6875F;
        } else if (!north && !south && west && east) {
            f2 = 0.3125F;
            f3 = 0.6875F;
        }

        return new SimpleCollisionBox(f, 0.0F, f2, f1, 1.5, f3);
    }

    @Override
    public boolean checkCanConnect(GrimPlayer player, WrappedBlockState state, Material one, Material two) {
        return Materials.checkFlag(one, Materials.WALL) || CollisionData.getData(one).getMovementCollisionBox(player, player.getClientVersion(), state, 0, 0, 0).isFullBlock();
    }
}
