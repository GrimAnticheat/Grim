package ac.grim.grimac.utils.math;

import ac.grim.grimac.player.GrimPlayer;

public class TrigHandler {
    GrimPlayer player;
    private double buffer = 0;
    private boolean isVanillaMath = true;

    public TrigHandler(GrimPlayer player) {
        this.player = player;
    }

    public void setOffset(double offset) {
        // Offset too high, this is an outlier, ignore
        // We are checking in the range of 1e-3 to 5e-5, around what using the wrong trig system results in
        // Also ignore if the player didn't move
        if (offset > 1e-3 || offset == 0) {
            // Minor movements can sometimes end up between 1e-4 to 1e-5 due to < 0.03 lost precision
            buffer -= 0.25;
            return;
        }

        buffer += offset < 5e-5 ? -1 : 1;

        if (buffer > 10) {
            buffer = 0;
            isVanillaMath = !isVanillaMath;
        }

        // Try and identify the math system within 0.5 seconds (At best) of joining
        // Switch systems in 2 seconds (At best) if the player changes their math system
        buffer = GrimMathHelper.clamp(buffer, -30, 10);
    }

    public float sin(float f) {
        return isVanillaMath ? VanillaMath.sin(f) : OptifineShitMath.sin(f);
    }

    public float cos(float f) {
        return isVanillaMath ? VanillaMath.cos(f) : OptifineShitMath.cos(f);
    }
}
