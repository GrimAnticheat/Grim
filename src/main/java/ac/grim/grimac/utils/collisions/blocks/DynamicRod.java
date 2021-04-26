package ac.grim.grimac.utils.collisions.blocks;

import ac.grim.grimac.utils.collisions.CollisionBox;
import ac.grim.grimac.utils.collisions.types.CollisionFactory;
import ac.grim.grimac.utils.collisions.types.SimpleCollisionBox;
import ac.grim.grimac.utils.data.ProtocolVersion;
import org.bukkit.block.data.BlockData;

@SuppressWarnings("Duplicates")
public class DynamicRod implements CollisionFactory {

    public static final CollisionBox UD = new SimpleCollisionBox(0.4375, 0, 0.4375, 0.5625, 1, 0.625);
    public static final CollisionBox EW = new SimpleCollisionBox(0, 0.4375, 0.4375, 1, 0.5625, 0.625);
    public static final CollisionBox NS = new SimpleCollisionBox(0.4375, 0.4375, 0, 0.5625, 0.625, 1);

    @Override
    public CollisionBox fetch(ProtocolVersion version, byte b, int x, int y, int z) {
        switch (b) {
            case 0:
            case 1:
            default:
                return UD.copy();
            case 2:
            case 3:
                return NS.copy();
            case 4:
            case 5:
                return EW.copy();
        }
    }

    @Override
    public CollisionBox fetch(ProtocolVersion version, BlockData block, int x, int y, int z) {
        // TODO: Get the actual byte
        fetch(version, (byte) 0, x, y, z);
        return null;
    }

}
