package ac.grim.grimac.utils.nmsutil;

import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import com.github.retrooper.packetevents.protocol.world.states.enums.VerticalDirection;
import com.github.retrooper.packetevents.protocol.world.states.type.StateTypes;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.PointedDripstone;

public class Dripstone {
    public static PointedDripstone update(GrimPlayer player, PointedDripstone toPlace, int x, int y, int z, boolean secondaryUse) {
        BlockFace primaryDirection = toPlace.getVerticalDirection();
        BlockFace opposite = toPlace.getVerticalDirection().getOppositeFace();

        WrappedBlockState typePlacingOn = player.compensatedWorld.getWrappedBlockStateAt(x, y + primaryDirection.getModY(), z);

        if (isPointedDripstoneWithDirection(typePlacingOn, opposite)) {
            // Use tip if the player is sneaking, or if it already is merged (somehow)
            // secondary use is flipped, for some reason, remember!
            PointedDripstone.Thickness thick = secondaryUse && ((PointedDripstone) typePlacingOn).getThickness() != PointedDripstone.Thickness.TIP_MERGE ?
                    PointedDripstone.Thickness.TIP : PointedDripstone.Thickness.TIP_MERGE;

            toPlace.setThickness(thick);
        } else {
            // Check if the blockstate air does not have the direction of UP already (somehow)
            if (!isPointedDripstoneWithDirection(typePlacingOn, primaryDirection)) {
                toPlace.setThickness(PointedDripstone.Thickness.TIP);
            } else {
                PointedDripstone.Thickness dripThick = ((PointedDripstone) typePlacingOn).getThickness();
                if (dripThick != PointedDripstone.Thickness.TIP && dripThick != PointedDripstone.Thickness.TIP_MERGE) {
                    // Look downwards
                    WrappedBlockState oppositeData = player.compensatedWorld.getWrappedBlockStateAt(x, y + opposite.getModY(), z);
                    PointedDripstone.Thickness toSetThick = !isPointedDripstoneWithDirection(oppositeData, primaryDirection)
                            ? PointedDripstone.Thickness.BASE : PointedDripstone.Thickness.MIDDLE;
                    toPlace.setThickness(toSetThick);
                } else {
                    toPlace.setThickness(PointedDripstone.Thickness.FRUSTUM);
                }
            }
        }
        return toPlace;
    }

    private static boolean isPointedDripstoneWithDirection(WrappedBlockState unknown, BlockFace direction) {
        return unknown.getType() == StateTypes.POINTED_DRIPSTONE && equalsVerticalDirection(unknown.getVerticalDirection(), direction);
    }

    private static boolean equalsVerticalDirection(VerticalDirection direction, BlockFace blockFace) {
        return (direction == VerticalDirection.UP && blockFace == BlockFace.UP)
                || (direction == VerticalDirection.DOWN && blockFace == BlockFace.DOWN);
    }
}
