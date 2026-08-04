package ac.grim.grimac.checks.type;

import ac.grim.grimac.api.AbstractCheck;
import ac.grim.grimac.utils.anticheat.update.BlockPlace;

public interface BlockPlaceListener extends AbstractCheck {
    void onBlockPlace(BlockPlace place);
}
