package ac.grim.grimac.utils.collisions.blocks;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.blockdata.types.WrappedBlockDataValue;
import ac.grim.grimac.utils.blockdata.types.WrappedChest;
import ac.grim.grimac.utils.blockstate.BaseBlockState;
import ac.grim.grimac.utils.collisions.CollisionBox;
import ac.grim.grimac.utils.collisions.types.CollisionFactory;
import ac.grim.grimac.utils.collisions.types.HexCollisionBox;
import io.github.retrooper.packetevents.utils.player.ClientVersion;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.Chest;

// In 1.12, chests don't have data that say what type of chest they are, other than direction
// In 1.13, chests store whether they are left or right
// With 1.12 clients on 1.13+ servers, the client checks NORTH and WEST for chests before SOUTH and EAST
// With 1.13+ clients on 1.12 servers, ViaVersion checks NORTH and WEST for chests before SOUTH and EAST
public class DynamicChest implements CollisionFactory {
    public CollisionBox fetch(GrimPlayer player, ClientVersion version, WrappedBlockDataValue block, int x, int y, int z) {
        WrappedChest chest = (WrappedChest) block;


        // 1.13+ clients on 1.13+ servers
        if (chest.isModern() && version.isNewerThanOrEquals(ClientVersion.v_1_13)) {
            if (chest.getType() == Chest.Type.SINGLE) {
                return new HexCollisionBox(1.0D, 0.0D, 1.0D, 15.0D, 14.0D, 15.0D);
            }

            if (chest.getDirection() == BlockFace.SOUTH && chest.getType() == Chest.Type.RIGHT || chest.getDirection() == BlockFace.NORTH && chest.getType() == Chest.Type.LEFT) {
                return new HexCollisionBox(1.0D, 0.0D, 1.0D, 16.0D, 14.0D, 15.0D); // Connected to the east face
            } else if (chest.getDirection() == BlockFace.SOUTH && chest.getType() == Chest.Type.LEFT || chest.getDirection() == BlockFace.NORTH && chest.getType() == Chest.Type.RIGHT) {
                return new HexCollisionBox(0.0D, 0.0D, 1.0D, 15.0D, 14.0D, 15.0D); // Connected to the west face
            } else if (chest.getDirection() == BlockFace.WEST && chest.getType() == Chest.Type.RIGHT || chest.getDirection() == BlockFace.EAST && chest.getType() == Chest.Type.LEFT) {
                return new HexCollisionBox(1.0D, 0.0D, 1.0D, 15.0D, 14.0D, 16.0D); // Connected to the south face
            } else {
                return new HexCollisionBox(1.0D, 0.0D, 0.0D, 15.0D, 14.0D, 15.0D); // Connected to the north face
            }
        }


        // 1.12 clients on 1.12 servers
        // 1.12 clients on 1.12 servers
        // 1.13 clients on 1.12 servers
        if (chest.getDirection() == BlockFace.EAST || chest.getDirection() == BlockFace.WEST) {
            BaseBlockState westState = player.compensatedWorld.getWrappedBlockStateAt(x - 1, y, z);

            if (westState.getMaterial() == chest.getMaterial()) {
                return new HexCollisionBox(0.0D, 0.0D, 1.0D, 15.0D, 14.0D, 15.0D); // Connected to the west face
            }

            BaseBlockState eastState = player.compensatedWorld.getWrappedBlockStateAt(x + 1, y, z);
            if (eastState.getMaterial() == chest.getMaterial()) {
                return new HexCollisionBox(1.0D, 0.0D, 1.0D, 16.0D, 14.0D, 15.0D); // Connected to the east face
            }
        } else {
            BaseBlockState northState = player.compensatedWorld.getWrappedBlockStateAt(x, y, z - 1);
            if (northState.getMaterial() == chest.getMaterial()) {
                return new HexCollisionBox(1.0D, 0.0D, 0.0D, 15.0D, 14.0D, 15.0D); // Connected to the north face
            }

            BaseBlockState southState = player.compensatedWorld.getWrappedBlockStateAt(x, y, z + 1);
            if (southState.getMaterial() == chest.getMaterial()) {
                return new HexCollisionBox(1.0D, 0.0D, 1.0D, 15.0D, 14.0D, 16.0D); // Connected to the south face
            }
        }

        // Single chest
        return new HexCollisionBox(1.0D, 0.0D, 1.0D, 15.0D, 14.0D, 15.0D);
    }


}
