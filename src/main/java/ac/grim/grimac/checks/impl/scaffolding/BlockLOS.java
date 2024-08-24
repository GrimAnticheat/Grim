package ac.grim.grimac.checks.impl.scaffolding;

import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.BlockPlaceCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.update.BlockPlace;
import ac.grim.grimac.utils.collisions.datatypes.SimpleCollisionBox;
import ac.grim.grimac.utils.data.HitData;
import ac.grim.grimac.utils.data.Pair;
import ac.grim.grimac.utils.nmsutil.BlockRayTrace;
import ac.grim.grimac.utils.nmsutil.Ray;
import ac.grim.grimac.utils.nmsutil.ReachUtils;
import com.github.retrooper.packetevents.protocol.attribute.Attributes;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.world.BlockFace;
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import com.github.retrooper.packetevents.protocol.world.states.type.StateTypes;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.util.Vector3f;
import com.github.retrooper.packetevents.util.Vector3i;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@CheckData(name = "ContainerLOS")
public class ContainerLOS extends BlockPlaceCheck {

  public ContainerLOS(GrimPlayer player) {
    super(player);
  }

  // Use post flying because it has the correct rotation, and can't false easily.
  @Override
  public void onPostFlyingBlockPlace(BlockPlace place) {
    if (place.getMaterial() == StateTypes.SCAFFOLDING) return;

    if (didRayTraceHit(place)) {
      return;
    }
    System.out.println("Invalid place");
    flagAndAlert("Invalid place, failed los check");

    // How do I actually cancel the interaction cause resync doesn't work
    place.resync();
  }

  private boolean didRayTraceHit(BlockPlace place) {
    Vector3i interactBlockVec = new Vector3i(place.getPlacedAgainstBlockLocation().getX(),
        place.getPlacedAgainstBlockLocation().getY(), place.getPlacedAgainstBlockLocation().getZ());
    WrappedBlockState interactBlock = player.compensatedWorld.getWrappedBlockStateAt(interactBlockVec);

    double maxDistance = player.compensatedEntities.getSelf()
        .getAttributeValue(Attributes.PLAYER_BLOCK_INTERACTION_RANGE);
    List<Vector3f> possibleLookDirs = new ArrayList<>(Arrays.asList(
        new Vector3f(player.lastXRot, player.yRot, 0),
        new Vector3f(player.xRot, player.yRot, 0)
    ));
    for (double eyeHeight : player.getPossibleEyeHeights()) {
      for (Vector3f lookDir : possibleLookDirs) {
        Vector eyePosition = new Vector(player.x, player.y + eyeHeight, player.z);
        // Why do we have our own vector classes and MC vector classes mixed throughout this codebase?
        Vector eyeLookDir = new Ray(player, eyePosition.getX(), eyePosition.getY(), eyePosition.getZ(), lookDir.x, lookDir.y).calculateDirection();

        WrappedBlockState targetBlock;
        Vector3i targetBlockVec = getTargetBlock(eyePosition, eyeLookDir, maxDistance);

        if (targetBlockVec == null) {
          continue;
        }
        targetBlock = player.compensatedWorld.getWrappedBlockStateAt(targetBlockVec);

        if (interactBlock.equals(targetBlock)) {
          return true;
        }
      }
    }

    return false;
  }

  private Vector3i getTargetBlock(Vector eyePosition, Vector eyeDirection, double maxDistance) {
    HitData hitData = BlockRayTrace.getNearestReachHitResult(player, eyePosition, eyeDirection, maxDistance, maxDistance);
    if (hitData == null) return null;
    return hitData.getPosition();
  }

  private boolean checkBlockType(Material type) {
    switch (type) {
      case ANVIL:
      case BARREL:
      case BEACON:
      case BEEHIVE:
      case BEE_NEST:
      case BLAST_FURNACE:
      case BREWING_STAND:
      case CAMPFIRE:
      case CARTOGRAPHY_TABLE:
      case CHEST:
      case CHEST_MINECART:
      case CHIPPED_ANVIL:
      case COMPOSTER:
      case CRAFTING_TABLE:
      case DAMAGED_ANVIL:
      case DISPENSER:
      case DROPPER:
      case ENCHANTING_TABLE:
      case ENDER_CHEST:
      case FURNACE:
      case FURNACE_MINECART:
      case FLETCHING_TABLE:
      case GRINDSTONE:
      case HOPPER:
      case HOPPER_MINECART:
      case ITEM_FRAME:
      case JUKEBOX:
      case LECTERN:
      case LOOM:
      case RESPAWN_ANCHOR:
      case SHULKER_BOX:
      case SMITHING_TABLE:
      case SMOKER:
      case SOUL_CAMPFIRE:
      case STONECUTTER:
      case TRAPPED_CHEST:

        // Do we really need to check for creative mode blocks?
      case COMMAND_BLOCK:
      case STRUCTURE_BLOCK:
      case JIGSAW:
        return true;
      default:
        return true;
    }
  }
}
