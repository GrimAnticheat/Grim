package ac.grim.grimac.utils.collisions.blocks;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.blockdata.WrappedBlockDataValue;
import ac.grim.grimac.utils.collisions.CollisionBox;
import ac.grim.grimac.utils.collisions.types.CollisionFactory;
import ac.grim.grimac.utils.collisions.types.HexCollisionBox;
import ac.grim.grimac.utils.collisions.types.SimpleCollisionBox;
import io.github.retrooper.packetevents.utils.player.ClientVersion;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Chest;

// In 1.12, chests don't have data that say what type of chest they are, other than direction
public class DynamicChest implements CollisionFactory {
    public CollisionBox fetch(ClientVersion version, byte data, int x, int y, int z) {
        return new SimpleCollisionBox(0, 0, 0, 1, 1, 1);
    }

    public CollisionBox fetch(ClientVersion version, BlockData block, int x, int y, int z) {
        Chest chest = (Chest) block;

        if (chest.getType() == Chest.Type.SINGLE) {
            return new HexCollisionBox(1.0D, 0.0D, 1.0D, 15.0D, 14.0D, 15.0D);
        }

        if (chest.getFacing() == BlockFace.SOUTH && chest.getType() == Chest.Type.RIGHT || chest.getFacing() == BlockFace.NORTH && chest.getType() == Chest.Type.LEFT) {
            return new HexCollisionBox(1.0D, 0.0D, 1.0D, 16.0D, 14.0D, 15.0D); // Connected to the east face
        } else if (chest.getFacing() == BlockFace.SOUTH && chest.getType() == Chest.Type.LEFT || chest.getFacing() == BlockFace.NORTH && chest.getType() == Chest.Type.RIGHT) {
            return new HexCollisionBox(0.0D, 0.0D, 1.0D, 15.0D, 14.0D, 15.0D); // Connected to the west face
        } else if (chest.getFacing() == BlockFace.WEST && chest.getType() == Chest.Type.RIGHT || chest.getFacing() == BlockFace.EAST && chest.getType() == Chest.Type.LEFT) {
            return new HexCollisionBox(1.0D, 0.0D, 1.0D, 15.0D, 14.0D, 16.0D); // Connected to the south face
        } else { // This is correct
            return new HexCollisionBox(1.0D, 0.0D, 0.0D, 15.0D, 14.0D, 15.0D); // Connected to the north face
        }
    }

    @Override
    public CollisionBox fetch(GrimPlayer player, ClientVersion version, WrappedBlockDataValue block, int x, int y, int z) {
        return null;
    }
}
