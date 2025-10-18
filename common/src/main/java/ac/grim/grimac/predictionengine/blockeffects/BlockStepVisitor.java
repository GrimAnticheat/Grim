package ac.grim.grimac.predictionengine.blockeffects;

import com.github.retrooper.packetevents.util.Vector3i;

@FunctionalInterface
public interface BlockStepVisitor {

    boolean visit(Vector3i blockPos, int index);

}
