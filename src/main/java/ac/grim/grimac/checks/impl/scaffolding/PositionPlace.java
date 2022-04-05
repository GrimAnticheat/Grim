package ac.grim.grimac.checks.impl.scaffolding;

import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.BlockPlaceCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.update.BlockPlace;
import org.bukkit.Bukkit;

@CheckData(name = "PositionPlace")
public class PositionPlace extends BlockPlaceCheck {
    public PositionPlace(GrimPlayer player) {
        super(player);
    }

    @Override
    public void onBlockPlace(final BlockPlace place) {
        if (true) return; // Check currently broken

        double xDiff = player.x - place.getPlacedAgainstBlockLocation().getX();
        double yDiff = player.y - place.getPlacedAgainstBlockLocation().getY();
        double zDiff = player.z - place.getPlacedAgainstBlockLocation().getZ();

        boolean flag = false;

        // TODO: Loop through hitbox to find the best collision
        switch (place.getDirection()) {
            case NORTH:
                flag = zDiff + player.getMovementThreshold() <= 0;
                break;
            case SOUTH:
                flag = zDiff + player.getMovementThreshold() <= 1;
                break;
            case EAST:
                flag = xDiff + player.getMovementThreshold() <= 0;
                break;
            case WEST:
                flag = xDiff + player.getMovementThreshold() <= 1;
                break;
            case UP:
                // The player's maximum eye height is 1.62 blocks, so lower than clicked pos, impossible
                // If the player is below the block by 1.62 blocks, they also couldn't have clicked it
                flag = yDiff - player.getMovementThreshold() > 1.62 || yDiff - player.getMovementThreshold() < -1.62;
                break;
            case DOWN:
                flag = yDiff + player.getMovementThreshold() <= 1;
                break;
        }

        Bukkit.broadcastMessage(xDiff + " " + yDiff + " " + zDiff + " " + place.getDirection());

        if (flag) {
            flagAndAlert();
        }
    }
}
