package ac.grim.grimac.utils.collisions.datatypes;

import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import org.bukkit.Material;

public interface HitBoxFactory {
    CollisionBox fetch(GrimPlayer player, Material heldItem, ClientVersion version, WrappedBlockState block, int x, int y, int z);
}
