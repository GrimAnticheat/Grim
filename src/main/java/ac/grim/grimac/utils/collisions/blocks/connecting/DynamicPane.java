package ac.grim.grimac.utils.collisions.blocks.connecting;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.blockdata.types.WrappedBlockDataValue;
import ac.grim.grimac.utils.blockdata.types.WrappedMultipleFacing;
import ac.grim.grimac.utils.collisions.CollisionData;
import ac.grim.grimac.utils.collisions.datatypes.CollisionBox;
import ac.grim.grimac.utils.collisions.datatypes.CollisionFactory;
import ac.grim.grimac.utils.collisions.datatypes.ComplexCollisionBox;
import ac.grim.grimac.utils.collisions.datatypes.SimpleCollisionBox;
import ac.grim.grimac.utils.nmsutil.Materials;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.world.BlockFace;
import org.bukkit.Material;

public class DynamicPane extends DynamicConnecting implements CollisionFactory {

    private static final CollisionBox[] COLLISION_BOXES = makeShapes(1.0F, 1.0F, 16.0F, 0.0F, 16.0F, true);

    @Override
    public CollisionBox fetch(GrimPlayer player, ClientVersion version, WrappedBlockDataValue block, int x, int y, int z) {
        boolean east;
        boolean north;
        boolean south;
        boolean west;

        // 1.13+ servers on 1.13+ clients send the full fence data
        if (ItemTypes.isNewVersion() && version.isNewerThanOrEquals(ClientVersion.V_1_13)) {
            WrappedMultipleFacing pane = (WrappedMultipleFacing) block;

            east = pane.getDirections().contains(BlockFace.EAST);
            north = pane.getDirections().contains(BlockFace.NORTH);
            south = pane.getDirections().contains(BlockFace.SOUTH);
            west = pane.getDirections().contains(BlockFace.WEST);
        } else {
            east = connectsTo(player, version, x, y, z, BlockFace.EAST);
            north = connectsTo(player, version, x, y, z, BlockFace.NORTH);
            south = connectsTo(player, version, x, y, z, BlockFace.SOUTH);
            west = connectsTo(player, version, x, y, z, BlockFace.WEST);
        }

        // On 1.7 and 1.8 clients, and 1.13+ clients on 1.7 and 1.8 servers, the glass pane is + instead of |
        if (!north && !south && !east && !west && (version.isOlderThanOrEquals(ClientVersion.V_1_8) || (PacketEvents.getAPI().getServerManager().getVersion().isOlderThanOrEquals(ServerVersion.V_1_8_8) && version.isNewerThanOrEquals(ClientVersion.V_1_13)))) {
            north = south = east = west = true;
        }

        if (version.isNewerThanOrEquals(ClientVersion.V_1_9)) {
            return COLLISION_BOXES[getAABBIndex(north, east, south, west)].copy();
        } else { // 1.8 and below clients have pane bounding boxes one pixel less
            ComplexCollisionBox boxes = new ComplexCollisionBox();
            if ((!west || !east) && (west || east || north || south)) {
                if (west) {
                    boxes.add(new SimpleCollisionBox(0.0F, 0.0F, 0.4375F, 0.5F, 1.0F, 0.5625F));
                } else if (east) {
                    boxes.add(new SimpleCollisionBox(0.5F, 0.0F, 0.4375F, 1.0F, 1.0F, 0.5625F));
                }
            } else {
                boxes.add(new SimpleCollisionBox(0.0F, 0.0F, 0.4375F, 1.0F, 1.0F, 0.5625F));
            }

            if ((!north || !south) && (west || east || north || south)) {
                if (north) {
                    boxes.add(new SimpleCollisionBox(0.4375F, 0.0F, 0.0F, 0.5625F, 1.0F, 0.5F));
                } else if (south) {
                    boxes.add(new SimpleCollisionBox(0.4375F, 0.0F, 0.5F, 0.5625F, 1.0F, 1.0F));
                }
            } else {
                boxes.add(new SimpleCollisionBox(0.4375F, 0.0F, 0.0F, 0.5625F, 1.0F, 1.0F));
            }

            return boxes;
        }
    }


    @Override
    public boolean checkCanConnect(GrimPlayer player, BaseBlockState state, Material one, Material two) {
        if (Materials.checkFlag(one, Materials.GLASS_PANE))
            return true;
        else
            return CollisionData.getData(one).getMovementCollisionBox(player, player.getClientVersion(), state, 0, 0, 0).isFullBlock();
    }
}
