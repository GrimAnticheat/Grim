package ac.grim.grimac.checks.impl.scaffolding;

import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.BlockPlaceCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.update.BlockPlace;
import ac.grim.grimac.utils.collisions.CollisionData;
import ac.grim.grimac.utils.collisions.datatypes.CollisionBox;
import ac.grim.grimac.utils.collisions.datatypes.ComplexCollisionBox;
import ac.grim.grimac.utils.collisions.datatypes.NoCollisionBox;
import ac.grim.grimac.utils.collisions.datatypes.SimpleCollisionBox;
import ac.grim.grimac.utils.data.BlockHitData;
import ac.grim.grimac.utils.data.Pair;
import ac.grim.grimac.utils.math.Vector3dm;
import ac.grim.grimac.utils.nmsutil.Ray;
import ac.grim.grimac.utils.nmsutil.ReachUtils;
import ac.grim.grimac.utils.nmsutil.WorldRayTrace;
import com.github.retrooper.packetevents.protocol.player.GameMode;
import com.github.retrooper.packetevents.protocol.world.BlockFace;
import com.github.retrooper.packetevents.util.Vector3d;

@CheckData(name = "WallPlace", description = "Placing blocks through walls")
public class WallPlace extends BlockPlaceCheck {

    private final SimpleCollisionBox[] cachedBoxes = new SimpleCollisionBox[ComplexCollisionBox.DEFAULT_MAX_COLLISION_BOX_SIZE];

    public WallPlace(GrimPlayer player) {
        super(player);
    }

    @Override
    public void onBlockPlace(BlockPlace place) {
        if (player.gamemode == GameMode.CREATIVE || player.gamemode == GameMode.SPECTATOR || place.cursor == null) {
            return;
        }

        Vector3d targetPoint = new Vector3d(
                place.position.getX() + place.cursor.getX(),
                place.position.getY() + place.cursor.getY(),
                place.position.getZ() + place.cursor.getZ()
        );

        boolean canSeeTarget = false;

        for (double eyeHeight : player.getPossibleEyeHeights()) {
            Vector3d eyePosV3 = new Vector3d(player.x, player.y + eyeHeight, player.z);
            Vector3dm eyePosM = new Vector3dm(player.x, player.y + eyeHeight, player.z);

            Vector3dm directionVec = new Vector3dm(
                    targetPoint.getX() - eyePosV3.getX(),
                    targetPoint.getY() - eyePosV3.getY(),
                    targetPoint.getZ() - eyePosV3.getZ()
            ).normalize();

            Ray ray = new Ray(eyePosM, directionVec);
            double maxDist = eyePosV3.distance(targetPoint) + 0.001;

            BlockHitData obstruction = WorldRayTrace.traverseBlocks(player, eyePosV3, targetPoint, (blockState, blockPos) -> {
                if (blockPos.equals(place.position)) return null;

                CollisionBox collisionBox = CollisionData.getData(blockState.getType())
                        .getMovementCollisionBox(player, player.getClientVersion(), blockState, blockPos.getX(), blockPos.getY(), blockPos.getZ());

                if (collisionBox instanceof NoCollisionBox) return null;

                int size = collisionBox.downCast(cachedBoxes);

                for (int i = 0; i < size; i++) {
                    SimpleCollisionBox box = cachedBoxes[i];
                    box.expand(-1e-4);

                    Pair<Vector3dm, BlockFace> intercept = ReachUtils.calculateIntercept(box, ray.getOrigin(), ray.getPointAtDistance(maxDist));

                    if (intercept.first() != null) {
                        return new BlockHitData(blockPos, intercept.first(), intercept.second(), blockState);
                    }
                }
                return null;
            });

            if (obstruction == null) {
                canSeeTarget = true;
                break;
            }
        }

        if (!canSeeTarget) {
            double dist = player.platformPlayer.getPosition().distance(targetPoint);
            if (flagAndAlert("dist=" + String.format("%.3f", dist)) && shouldModifyPackets()) {
                place.resync();
            }
        } else {
            reward();
        }
    }
}
