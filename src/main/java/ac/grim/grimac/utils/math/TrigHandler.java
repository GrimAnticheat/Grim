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
        //
        // Ignore if 0 offset
        if (offset == 0 || offset > 1e-3) {
            return;
        }

        buffer += offset < 5e-5 ? -1 : 1;

        if (buffer > 10) {
            buffer = 0;
            isVanillaMath = !isVanillaMath;
        }

        // Try and identify the math system within 0.5 seconds (At best) of joining
        // Switch systems in 1.5 seconds (At best) if the player changes their math system
        buffer = GrimMathHelper.clamp(buffer, -20, 10);
    }

    public float sin(float f) {
        return isVanillaMath ? VanillaMath.sin(f) : OptifineShitMath.sin(f);
    }

    public float cos(float f) {
        return isVanillaMath ? VanillaMath.cos(f) : OptifineShitMath.cos(f);
    }
}
