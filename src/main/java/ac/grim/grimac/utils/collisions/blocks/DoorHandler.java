package ac.grim.grimac.utils.collisions.blocks;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.collisions.datatypes.CollisionBox;
import ac.grim.grimac.utils.collisions.datatypes.CollisionFactory;
import ac.grim.grimac.utils.collisions.datatypes.HexCollisionBox;
import ac.grim.grimac.utils.collisions.datatypes.NoCollisionBox;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.world.BlockFace;
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import com.github.retrooper.packetevents.protocol.world.states.enums.Half;
import com.github.retrooper.packetevents.protocol.world.states.enums.Hinge;

public class DoorHandler implements CollisionFactory {
    protected static final CollisionBox SOUTH_AABB = new HexCollisionBox(0.0D, 0.0D, 0.0D, 16.0D, 16.0D, 3.0D);
    protected static final CollisionBox NORTH_AABB = new HexCollisionBox(0.0D, 0.0D, 13.0D, 16.0D, 16.0D, 16.0D);
    protected static final CollisionBox WEST_AABB = new HexCollisionBox(13.0D, 0.0D, 0.0D, 16.0D, 16.0D, 16.0D);
    protected static final CollisionBox EAST_AABB = new HexCollisionBox(0.0D, 0.0D, 0.0D, 3.0D, 16.0D, 16.0D);

    @Override
    public CollisionBox fetch(GrimPlayer player, ClientVersion version, WrappedBlockState block, int x, int y, int z) {
        switch (fetchDirection(player, version, block, x, y, z)) {
            case NORTH:
                return NORTH_AABB.copy();
            case SOUTH:
                return SOUTH_AABB.copy();
            case EAST:
                return EAST_AABB.copy();
            case WEST:
                return WEST_AABB.copy();
        }

        return NoCollisionBox.INSTANCE;
    }

    public BlockFace fetchDirection(GrimPlayer player, ClientVersion version, WrappedBlockState door, int x, int y, int z) {
        BlockFace facingDirection;
        boolean isClosed;
        boolean isRightHinge;

        // 1.12 stores block data for the top door in the bottom block data
        // ViaVersion can't send 1.12 clients the 1.13 complete data
        // For 1.13, ViaVersion should just use the 1.12 block data
        // I hate legacy versions... this is so messy
        //TODO: This needs to be updated to support corrupted door collision
        if (PacketEvents.getAPI().getServerManager().getVersion().isOlderThanOrEquals(ServerVersion.V_1_12_2)
                || version.isOlderThanOrEquals(ClientVersion.V_1_12_2)) {
            if (door.getHalf() == Half.LOWER) {
                WrappedBlockState above = player.compensatedWorld.getWrappedBlockStateAt(x, y + 1, z);

                facingDirection = door.getFacing();
                isClosed = !door.isOpen();

                // Doors have to be the same material in 1.12 for their block data to be connected together
                // For example, if you somehow manage to get a jungle top with an oak bottom, the data isn't shared
                if (above.getType() == door.getType()) {
                    isRightHinge = above.getHinge() == Hinge.RIGHT;
                } else {
                    // Default missing value
                    isRightHinge = false;
                }
            } else {
                WrappedBlockState below = player.compensatedWorld.getWrappedBlockStateAt(x, y - 1, z);

                if (below.getType() == door.getType() && below.getHalf() == Half.LOWER) {
                    isClosed = !below.isOpen();
                    facingDirection = below.getFacing();
                    isRightHinge = door.getHinge() == Hinge.RIGHT;
                } else {
                    facingDirection = BlockFace.EAST;
                    isClosed = true;
                    isRightHinge = false;
                }
            }
        } else {
            facingDirection = door.getFacing();
            isClosed = !door.isOpen();
            isRightHinge = door.getHinge() == Hinge.RIGHT;
        }

        switch (facingDirection) {
            case EAST:
            default:
                return isClosed ? BlockFace.EAST : (isRightHinge ? BlockFace.NORTH : BlockFace.SOUTH);
            case SOUTH:
                return isClosed ? BlockFace.SOUTH : (isRightHinge ? BlockFace.EAST : BlockFace.WEST);
            case WEST:
                return isClosed ? BlockFace.WEST : (isRightHinge ? BlockFace.SOUTH : BlockFace.NORTH);
            case NORTH:
                return isClosed ? BlockFace.NORTH : (isRightHinge ? BlockFace.WEST : BlockFace.EAST);
        }
    }
}
