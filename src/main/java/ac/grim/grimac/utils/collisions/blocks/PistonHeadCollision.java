package ac.grim.grimac.utils.collisions.blocks;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.blockdata.types.WrappedBlockDataValue;
import ac.grim.grimac.utils.blockdata.types.WrappedPiston;
import ac.grim.grimac.utils.collisions.datatypes.CollisionBox;
import ac.grim.grimac.utils.collisions.datatypes.CollisionFactory;
import ac.grim.grimac.utils.collisions.datatypes.ComplexCollisionBox;
import ac.grim.grimac.utils.collisions.datatypes.HexCollisionBox;
import io.github.retrooper.packetevents.utils.player.ClientVersion;

public class PistonHeadCollision implements CollisionFactory {
    // 1.12- servers are not capable of sending persistent (non-block event) piston move
    // 1.13+ clients are capable of seeing 1.13+ short pistons - we can look at block data to check
    // 1.7 and 1.8 clients always have short pistons
    // 1.9 - 1.12 clients always have long pistons
    @Override
    public CollisionBox fetch(GrimPlayer player, ClientVersion version, WrappedBlockDataValue block, int x, int y, int z) {
        WrappedPiston piston = (WrappedPiston) block;
        // 1.13+ clients differentiate short and long, and the short vs long data is stored
        // This works correctly in 1.12-, as in the piston returns as always long
        double longAmount = piston.isShort() ? 0 : 4;

        // And 1.9, 1.10 clients always have "long" piston collision boxes - even if the piston is "short"
        // 1.11 and 1.12 clients differentiate short and long piston collision boxes - but I can never get long heads in multiplayer
        // They show up in the debug world, but my client crashes every time I join the debug world in multiplayer in these two version
        // So just group together 1.9-1.12 into all having long pistons
        if (version.isNewerThanOrEquals(ClientVersion.v_1_9) && version.isOlderThanOrEquals(ClientVersion.v_1_12_2))
            longAmount = 4;


        // 1.8 and 1.7 clients always have "short" piston collision boxes
        // Apply last to overwrite other long amount setters
        if (version.isOlderThan(ClientVersion.v_1_9))
            longAmount = 0;


        switch (piston.getDirection()) {
            case DOWN:
            default:
                return new ComplexCollisionBox(new HexCollisionBox(0, 0, 0, 16, 4, 16),
                        new HexCollisionBox(6, 4, 6, 10, 16 + longAmount, 10));
            case UP:
                return new ComplexCollisionBox(new HexCollisionBox(0, 12, 0, 16, 16, 16),
                        new HexCollisionBox(6, 0 - longAmount, 6, 10, 12, 10));
            case NORTH:
                return new ComplexCollisionBox(new HexCollisionBox(0, 0, 0, 16, 16, 4),
                        new HexCollisionBox(4, 6, 4, 12, 10, 16 + longAmount));
            case SOUTH:
                // SOUTH piston is glitched in 1.7 and 1.8, fixed in 1.9
                // Don't bother with short piston boxes as 1.7/1.8 clients don't have them
                if (version.isOlderThanOrEquals(ClientVersion.v_1_8))
                    return new ComplexCollisionBox(new HexCollisionBox(0, 0, 12, 16, 16, 16),
                            new HexCollisionBox(4, 6, 0, 12, 10, 12));

                return new ComplexCollisionBox(new HexCollisionBox(0, 0, 12, 16, 16, 16),
                        new HexCollisionBox(6, 6, 0 - longAmount, 10, 10, 12));
            case WEST:
                // WEST piston is glitched in 1.7 and 1.8, fixed in 1.9
                // Don't bother with short piston boxes as 1.7/1.8 clients don't have them
                if (version.isOlderThanOrEquals(ClientVersion.v_1_8))
                    return new ComplexCollisionBox(new HexCollisionBox(0, 0, 0, 4, 16, 16),
                            new HexCollisionBox(6, 4, 4, 10, 12, 16));

                return new ComplexCollisionBox(new HexCollisionBox(0, 0, 0, 4, 16, 16),
                        new HexCollisionBox(4, 6, 6, 16 + longAmount, 10, 10));
            case EAST:
                return new ComplexCollisionBox(new HexCollisionBox(12, 0, 0, 16, 16, 16),
                        new HexCollisionBox(0 - longAmount, 6, 4, 12, 10, 12));
        }
    }
}
