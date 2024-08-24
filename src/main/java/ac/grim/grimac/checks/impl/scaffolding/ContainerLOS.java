package ac.grim.grimac.checks.impl.scaffolding;

import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.BlockPlaceCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.update.BlockPlace;
import ac.grim.grimac.utils.collisions.datatypes.SimpleCollisionBox;
import ac.grim.grimac.utils.data.Pair;
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
      System.out.println("Ray trace hit");
      return;
    }
    place.resync();
  }

  private boolean didRayTraceHit(BlockPlace place) {
//    Location location = new Location(this.player.bukkitPlayer.getWorld(),
//        place.getPlacedAgainstBlockLocation().getX(),
//        place.getPlacedAgainstBlockLocation().getY(),
//        place.getPlacedAgainstBlockLocation().getZ());
    Vector3i interactBlockVec = new Vector3i(place.getPlacedAgainstBlockLocation().getX(),
        place.getPlacedAgainstBlockLocation().getY(), place.getPlacedAgainstBlockLocation().getZ());
    WrappedBlockState interactBlock = player.compensatedWorld.getWrappedBlockStateAt(interactBlockVec);


//    if (!isInteractableBlock(interactBlock.getType())) {
//      return false;
//    }
    double maxDistance = player.compensatedEntities.getSelf()
        .getAttributeValue(Attributes.PLAYER_BLOCK_INTERACTION_RANGE);
//    List<Vector3f> possibleLookDirs = new ArrayList<>(Arrays.asList(
//        new Vector3f(player.lastXRot, player.yRot, 0),
//        new Vector3f(player.xRot, player.yRot, 0)
//    ));
//    for (double eyeHeight : player.getPossibleEyeHeights()) {
//      for (Vector3f lookDir : possibleLookDirs) {
//        // x, y, z are correct for the block placement even after post tick because of code elsewhere
//        Vector3d starting = new Vector3d(player.x, player.y + eyeHeight, player.z);
//        // xRot and yRot are a tick behind
//        Ray trace = new Ray(player, starting.getX(), starting.getY(), starting.getZ(), lookDir.getX(), lookDir.getY());
//        Pair<Vector, BlockFace> intercept = ReachUtils.calculateIntercept(box, trace.getOrigin(), trace.getPointAtDistance(distance));
//
//        if (intercept.getFirst() != null) return true;
//      }
//    }

    Location eyeLocation = player.bukkitPlayer.getEyeLocation();
    Vector eyePosition = new Vector(eyeLocation.getX(), eyeLocation.getY(), eyeLocation.getZ());

    WrappedBlockState targetBlock;
    Vector3i targetBlockVec = getTargetBlock(eyePosition, eyeLocation.getDirection(), maxDistance);

    if (targetBlockVec == null) {
      System.out.println("Impossible for no ray trace block to exist.");
      return false;
//      flagAndAlert("no-ray-trace-block");
    } else {
      targetBlock = player.compensatedWorld.getWrappedBlockStateAt(targetBlockVec);
    }

    if (interactBlock.equals(targetBlock)) {
//      System.out.println("Nothing to see here, nothing wrong");
      return true;
    } else {
      System.out.println(
          "Player interacted with block at: " + interactBlockVec.getX() + " " + interactBlockVec.getY() +
              " " + interactBlockVec.getZ());
      System.out.println(
          "Raytrace check hit block at: " + targetBlockVec.getX() + " " +
              targetBlockVec.getY() + " " + targetBlockVec.getZ());
//      flagAndAlert("raytrace-hit-wrong-block");
    }
    return false;
  }

  private Vector3i getTargetBlock(Vector eyePosition, Vector eyeDirection, double maxDistance) {
    eyeDirection = eyeDirection.normalize();
    WrappedBlockState wrappedBlockState;

    for (int i = 0; i <= maxDistance; i++) {
      Vector rayTrace = eyeDirection.clone().multiply(i);
      Vector blockVector = eyePosition.add(rayTrace);
      wrappedBlockState = player.compensatedWorld.getWrappedBlockStateAt(blockVector);

      if (!wrappedBlockState.getType().isAir()) {
        return new Vector3i(blockVector.getBlockX(), blockVector.getBlockY(), blockVector.getBlockZ());
      }
      eyePosition.subtract(rayTrace);
    }

    return null;
  }

  private boolean isInteractableBlock(Material type) {
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
