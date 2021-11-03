package ac.grim.grimac.utils.latency;

import ac.grim.grimac.player.GrimPlayer;

import java.util.concurrent.ConcurrentHashMap;

public class CompensatedPotions {
    private final GrimPlayer player;
    private final ConcurrentHashMap<Integer, ConcurrentHashMap<String, Integer>> potionsMap = new ConcurrentHashMap<>();

    public CompensatedPotions(GrimPlayer player) {
        this.player = player;
    }

    public Integer getJumpAmplifier() {
        return getPotionLevel("JUMP");
    }

    public Integer getLevitationAmplifier() {
        return getPotionLevel("LEVITATION");
    }

    public Integer getSlowFallingAmplifier() {
        return getPotionLevel("SLOW_FALLING");
    }

    public Integer getDolphinsGraceAmplifier() {
        return getPotionLevel("DOLPHINS_GRACE");
    }

    public void addPotionEffect(String type, int level, int entityID) {
        player.latencyUtils.addRealTimeTask(player.lastTransactionSent.get() + 1, () -> {
            ConcurrentHashMap<String, Integer> potions = potionsMap.get(entityID);

            if (potions == null) {
                potions = new ConcurrentHashMap<>();
                potionsMap.put(entityID, potions);
            }

            player.pointThreeEstimator.updatePlayerPotions(type, level);
            potions.put(type, level);
        });
    }

    public void removePotionEffect(String type, int entityID) {
        player.latencyUtils.addRealTimeTask(player.lastTransactionSent.get() + 1, () -> {
            ConcurrentHashMap<String, Integer> potions = potionsMap.get(entityID);

            player.pointThreeEstimator.updatePlayerPotions(type, null);

            if (potions != null) {
                potions.remove(type);
            }
        });
    }

    public Integer getPotionLevel(String type) {
        ConcurrentHashMap<String, Integer> effects;
        if (player.vehicle == null) {
            effects = potionsMap.get(player.entityID);
        } else {
            effects = potionsMap.get(player.vehicle);
        }

        if (effects == null) {
            return null;
        }

        return effects.get(type);
    }

    public void removeEntity(int entityID) {
        potionsMap.remove(entityID);
    }
}
