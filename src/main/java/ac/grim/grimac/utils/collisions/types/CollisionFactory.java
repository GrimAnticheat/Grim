package ac.grim.grimac.utils.collisions.types;

import ac.grim.grimac.utils.blockdata.WrappedBlockDataValue;
import ac.grim.grimac.utils.collisions.CollisionBox;
import ac.grim.grimac.utils.data.ProtocolVersion;

public interface CollisionFactory {
    CollisionBox fetch(ProtocolVersion version, WrappedBlockDataValue block, int x, int y, int z);
}