package ac.grim.grimac.utils.math;

import org.bukkit.util.Vector;

public class VectorPair {
    public Vector lastTickOutput;
    public Vector playerInput;

    public VectorPair(Vector lastTickOutput, Vector playerInput) {
        this.lastTickOutput = lastTickOutput;
        this.playerInput = playerInput;
    }
}
