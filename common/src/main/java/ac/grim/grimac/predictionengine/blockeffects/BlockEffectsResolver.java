package ac.grim.grimac.predictionengine.blockeffects;

import ac.grim.grimac.player.GrimPlayer;

import java.util.List;

public interface BlockEffectsResolver {

    void applyEffectsFromBlocks(GrimPlayer player, List<GrimPlayer.Movement> movements);

}
