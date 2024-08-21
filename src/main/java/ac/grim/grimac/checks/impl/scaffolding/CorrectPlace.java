package ac.grim.grimac.checks.impl.scaffolding;

import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.BlockPlaceCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.update.BlockPlace;
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.util.Vector3f;
import com.github.retrooper.packetevents.util.Vector3i;

@CheckData(name = "CorrectPlace", experimental = true)
public class CorrectPlace extends BlockPlaceCheck {

    private Vector3i previousBlockPlaced = null;

    public CorrectPlace(GrimPlayer player) {
        super(player);
    }

    @Override
    public void onBlockPlace(BlockPlace event) {
        if (player.getInventory().inventory.getHeldItem().getType().equals(ItemTypes.AIR)) return;
        if (player.y < event.getPlacedBlockPos().getY()) return;

        Vector3f cursor = event.getCursor();

        // player is looking at the end of block (at left or right), this won't affect the center
        if ((cursor.getX() < 0.2 && cursor.getX() > 0.8) || (cursor.getZ() < 0.2 && cursor.getZ() > 0.8)) return;

        double diffY = player.y - event.getPlacedBlockPos().toVector3d().getY();

        double adjustedPitch = 2.33d * diffY;

        if (player.yRot < 80.97d + adjustedPitch && player.yRot > 74.5 - adjustedPitch) return;

        Vector3d pos = event.getPlacedBlockPos().toVector3d().subtract(0, 0.6, 0);
        if (!player.compensatedWorld.getWrappedBlockStateAt(pos.toVector3i()).getType().isAir()) return;

        if (previousBlockPlaced != null && previousBlockPlaced.getY() - event.getPlacedBlockPos().getY() == 0) {
            if (flagAndAlert() && shouldCancel() && shouldModifyPackets()) {
                event.resync();
            }
        }
        previousBlockPlaced = event.getPlacedBlockPos();
    }
}