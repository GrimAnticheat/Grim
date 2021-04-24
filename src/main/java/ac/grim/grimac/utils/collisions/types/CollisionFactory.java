package ac.grim.grimac.utils.collisions.types;

import ac.grim.grimac.utils.collisions.CollisionBox;
import ac.grim.grimac.utils.data.ProtocolVersion;
import org.bukkit.block.Block;

public interface CollisionFactory {
    CollisionBox fetch(ProtocolVersion version, Block block);
}