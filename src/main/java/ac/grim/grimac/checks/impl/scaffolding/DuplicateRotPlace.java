package ac.grim.grimac.checks.impl.scaffolding;

import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.BlockPlaceCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.update.BlockPlace;
import ac.grim.grimac.utils.anticheat.update.RotationUpdate;

@CheckData(name = "DuplicateRotPlace", experimental = true)
public class DuplicateRotPlace extends BlockPlaceCheck {

    public DuplicateRotPlace(final GrimPlayer player) {
        super(player);
    }

    private float deltaX, deltaY;

    private double deltaDotsX;
    private boolean rotated = false;

    @Override
    public void process(final RotationUpdate rotationUpdate) {
        deltaX = rotationUpdate.getDeltaXRotABS();
        deltaY = rotationUpdate.getDeltaYRotABS();
        deltaDotsX = rotationUpdate.getProcessor().deltaDotsX;
        rotated = true;
    }

    private float lastPlacedDeltaX;
    private double lastPlacedDeltaDotsX;

    public void onPostFlyingBlockPlace(final BlockPlace place) {
        if (!rotated) return;

        if (deltaX > 2) {
            final float xDiff = Math.abs(deltaX - lastPlacedDeltaX);
            final double xDiffDots = Math.abs(deltaDotsX - lastPlacedDeltaDotsX);

            if (xDiff < 0.0001) {
                flagAndAlert("x=" + xDiff + " xdots=" + xDiffDots + " y=" + deltaY);
            } else {
                reward();
            }
        } else {
            reward();
        }
        this.lastPlacedDeltaX = deltaX;
        this.lastPlacedDeltaDotsX = deltaDotsX;
        rotated = false;
    }


}
