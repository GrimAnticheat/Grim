package ac.grim.grimac.utils.anticheat.update;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.collisions.HitboxData;
import ac.grim.grimac.utils.collisions.datatypes.CollisionBox;
import ac.grim.grimac.utils.collisions.datatypes.SimpleCollisionBox;
import com.github.retrooper.packetevents.protocol.player.DiggingAction;
import com.github.retrooper.packetevents.protocol.world.BlockFace;
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import com.github.retrooper.packetevents.util.Vector3i;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

public final class BlockBreak {
    private final GrimPlayer player;
    public final Vector3i position;
    public final BlockFace face;
    public final int faceId;
    public final DiggingAction action;
    public final WrappedBlockState block;
    @Getter
    private boolean cancelled;

    public BlockBreak(GrimPlayer player, Vector3i position, BlockFace face, int faceId, DiggingAction action, WrappedBlockState block) {
        this.player = player;
        this.position = position;
        this.face = face;
        this.faceId = faceId;
        this.action = action;
        this.block = block;
    }

    public void cancel() {
        this.cancelled = true;
    }

    public SimpleCollisionBox getCombinedBox() {
        CollisionBox placedOn = HitboxData.getBlockHitbox(player, player.getInventory().getHeldItem().getType().getPlacedType(), player.getClientVersion(), block, true, position.x, position.y, position.z);

        List<SimpleCollisionBox> boxes = new ArrayList<>();
        placedOn.downCast(boxes);

        SimpleCollisionBox combined = new SimpleCollisionBox(position.x, position.y, position.z);
        for (SimpleCollisionBox box : boxes) {
            double minX = Math.max(box.minX, combined.minX);
            double minY = Math.max(box.minY, combined.minY);
            double minZ = Math.max(box.minZ, combined.minZ);
            double maxX = Math.min(box.maxX, combined.maxX);
            double maxY = Math.min(box.maxY, combined.maxY);
            double maxZ = Math.min(box.maxZ, combined.maxZ);
            combined = new SimpleCollisionBox(minX, minY, minZ, maxX, maxY, maxZ);
        }

        return combined;
    }
}
