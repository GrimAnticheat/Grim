package ac.grim.grimac.checks.type;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.update.BlockPlace;

public class BlockPlaceCheck extends Check<BlockPlace> {
    public BlockPlaceCheck(GrimPlayer player) {
        super(player);
    }

    public void onBlockPlace(final BlockPlace place) {
    }
}
