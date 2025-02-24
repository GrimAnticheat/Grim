package ac.grim.grimac.utils.collisions.blocks.connecting;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.collisions.CollisionData;
import ac.grim.grimac.utils.collisions.datatypes.*;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.world.BlockFace;
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import com.github.retrooper.packetevents.protocol.world.states.defaulttags.BlockTags;
import com.github.retrooper.packetevents.protocol.world.states.enums.*;
import com.github.retrooper.packetevents.protocol.world.states.type.StateType;

public class DynamicHitboxWall extends DynamicConnecting implements HitBoxFactory {
    @Override
    public CollisionBox fetch(GrimPlayer player, StateType heldItem, ClientVersion version, WrappedBlockState state, boolean isTargetBlock, int x, int y, int z) {
        int[] connections = getConnections(player, version, state, x, y, z);
        int north = connections[0], south = connections[1], west = connections[2], east = connections[3], up = connections[4];

        if (version.isNewerThanOrEquals(ClientVersion.V_1_13)) {
            return getModernHitBox(north, south, west, east, up);
        } else {
            return getLegacyHitBox(north, south, west, east);
        }
    }

    private int[] getConnections(GrimPlayer player, ClientVersion version, WrappedBlockState state, int x, int y, int z) {
        int north = 0, south = 0, west = 0, east = 0, up = 0;

        if (isModernServer()) {
            boolean sixteen = PacketEvents.getAPI().getServerManager().getVersion().isNewerThan(ServerVersion.V_1_16);
            north = getConnectionValue(state.getNorth(), sixteen);
            east = getConnectionValue(state.getEast(), sixteen);
            south = getConnectionValue(state.getSouth(), sixteen);
            west = getConnectionValue(state.getWest(), sixteen);
            up = state.isUp() ? 1 : 0;
        } else {
            north = connectsTo(player, version, x, y, z, BlockFace.NORTH) ? 1 : 0;
            south = connectsTo(player, version, x, y, z, BlockFace.SOUTH) ? 1 : 0;
            west = connectsTo(player, version, x, y, z, BlockFace.WEST) ? 1 : 0;
            east = connectsTo(player, version, x, y, z, BlockFace.EAST) ? 1 : 0;
            up = 1;
        }

        return new int[]{north, south, west, east, up};
    }

    private boolean isModernServer() {
        return PacketEvents.getAPI().getServerManager().getVersion().isNewerThan(ServerVersion.V_1_12_2);
    }

    private int getConnectionValue(Enum<?> direction, boolean sixteen) {
        if (direction == North.NONE || direction == East.NONE || direction == South.NONE || direction == West.NONE) {
            return 0;
        }
        return (direction == North.LOW || direction == East.LOW || direction == South.LOW || direction == West.LOW || sixteen) ? 1 : 2;
    }

    private CollisionBox getModernHitBox(int north, int south, int west, int east, int up) {
        ComplexCollisionBox box = new ComplexCollisionBox(5);
        if (up == 1) {
            box.add(new HexCollisionBox(4, 0, 4, 12, 16, 12));
        }

        addDirectionalBox(box, north, 5, 0, 0.0D, 11, 14, 11);
        addDirectionalBox(box, south, 5, 0, 5, 11, 14, 16);
        addDirectionalBox(box, west, 0, 0, 5, 11, 14, 11);
        addDirectionalBox(box, east, 5, 0, 5, 16, 14, 11);

        return box;
    }

    private void addDirectionalBox(ComplexCollisionBox box, int direction, double x1, double y1, double z1, double x2, double y2, double z2) {
        if (direction == 1) {
            box.add(new HexCollisionBox(x1, y1, z1, x2, y2, z2));
        } else if (direction == 2) {
            box.add(new HexCollisionBox(x1, y1, z1, x2, 16, z2));
        }
    }

    private CollisionBox getLegacyHitBox(int north, int south, int west, int east) {
        float minX = 0.25F, maxX = 0.75F, minZ = 0.25F, maxZ = 0.75F;
        float maxY = 1.0F;

        if (north == 1) minZ = 0.0F;
        if (south == 1) maxZ = 1.0F;
        if (west == 1) minX = 0.0F;
        if (east == 1) maxX = 1.0F;

        if (north == 1 && south == 1 && west == 0 && east == 0) {
            maxY = 0.8125F;
            minX = 0.3125F;
            maxX = 0.6875F;
        } else if (west == 1 && east == 1 && north == 0 && south == 0) {
            maxY = 0.8125F;
            minZ = 0.3125F;
            maxZ = 0.6875F;
        }

        return new SimpleCollisionBox(minX, 0.0F, minZ, maxX, maxY, maxZ);
    }

    @Override
    public boolean checkCanConnect(GrimPlayer player, WrappedBlockState state, StateType one, StateType two, BlockFace direction) {
        return BlockTags.WALLS.contains(one) ||
                CollisionData.getData(one).getMovementCollisionBox(player, player.getClientVersion(), state, 0, 0, 0).isSideFullBlock(direction);
    }
}
