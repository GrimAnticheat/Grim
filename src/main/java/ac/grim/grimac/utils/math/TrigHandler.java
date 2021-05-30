package ac.grim.grimac.utils.math;

import ac.grim.grimac.player.GrimPlayer;

public class TrigHandler {
    GrimPlayer player;

    // Vanilla offset is probably 1e-7 plus or minus a few magnitudes
    // Optifine FastMath is consistently around 1e-4
    //
    // Therefore, if the offset is higher than 5e-5 switch the math system to try and solve it
    // Start player with vanilla math system as optifine fastmath is in the wrong here
    double vanillaOffset = 0;
    double optifineOffset = 5e-5;

    public TrigHandler(GrimPlayer player) {
        this.player = player;
    }


}
