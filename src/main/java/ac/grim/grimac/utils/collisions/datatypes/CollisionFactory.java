package ac.grim.grimac.utils.collisions.datatypes;

import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;

public interface CollisionFactory {
    CollisionBox fetch(GrimPlayer player, ClientVersion version, WrappedBlockState block, int x, int y, int z);
}