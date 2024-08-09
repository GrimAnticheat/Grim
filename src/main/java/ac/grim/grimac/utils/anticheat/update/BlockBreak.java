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


public class BlockBreak {
    public final Vector3i position;
    public final BlockFace face;
    public final DiggingAction action;
    @Getter
    private boolean cancelled;

    private final GrimPlayer player;
    private final WrappedBlockState block;

    public BlockBreak(Vector3i position, BlockFace face, DiggingAction action, GrimPlayer player, WrappedBlockState block) {
        this.position = position;
        this.face = face;
        this.action = action;
        this.player = player;
        this.block = block;
    }

    public void cancel() {
        cancelled = true;
    }

    public SimpleCollisionBox getCombinedBox() {
        // Alright, instead of skidding AACAdditionsPro, let's just use bounding boxes
        CollisionBox placedOn = HitboxData.getBlockHitbox(player, player.getInventory().getHeldItem().getType().getPlacedType(), player.getClientVersion(), block, position.x, position.y, position.z);

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
