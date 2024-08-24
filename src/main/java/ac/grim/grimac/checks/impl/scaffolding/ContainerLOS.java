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

    Location location = new Location(this.player.bukkitPlayer.getWorld(),
        place.getPlacedAgainstBlockLocation().getX(),
        place.getPlacedAgainstBlockLocation().getY(),
        place.getPlacedAgainstBlockLocation().getZ());
    Block interactBlock = location.getBlock();


    if (!isInteractableBlock(interactBlock.getType())) {
      return;
    }
    double maxDistance = player.compensatedEntities.getSelf().getAttributeValue(Attributes.PLAYER_BLOCK_INTERACTION_RANGE);


    Block targetBlock = getTargetBlock(player, maxDistance);
    if (targetBlock == null) {
      System.out.println("Impossible for no ray trace block to exist.");
      flagAndAlert("no-ray-trace-block");
      return;
    }

    if (targetBlock.equals(interactBlock)) {
      System.out.println("Nothing to see here, nothing wrong");
    } else {
      System.out.println("Player interacted with block at: " + interactBlock.getX() + " " + interactBlock.getY() + " " + interactBlock.getZ());
      System.out.println("Raytrace check hit " + targetBlock.getType() + " block at: " + targetBlock.getX() + " " + targetBlock.getY() + " " + targetBlock.getZ());
      flagAndAlert("raytrace-hit-wrong-block");
    }
  }

  private Block getTargetBlock(GrimPlayer player, double maxDistance) {
    Location eyeLocation = player.bukkitPlayer.getEyeLocation(); // How do I compensate for latency?
    Vector direction = eyeLocation.getDirection().normalize();

    for (int i = 0; i <= maxDistance; i++) {
      Vector vec = direction.clone().multiply(i);
      Block block = eyeLocation.add(vec).getBlock();
      eyeLocation.subtract(vec);

      if (block.getType() != Material.AIR) {
        return block;
      }
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
