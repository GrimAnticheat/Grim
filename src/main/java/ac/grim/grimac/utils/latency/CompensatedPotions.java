package ac.grim.grimac.utils.latency;

import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.protocol.potion.PotionType;
import com.github.retrooper.packetevents.protocol.potion.PotionTypes;

import java.util.concurrent.ConcurrentHashMap;

public class CompensatedPotions {
    private final GrimPlayer player;
    private final ConcurrentHashMap<Integer, ConcurrentHashMap<PotionType, Integer>> potionsMap = new ConcurrentHashMap<>();

    public CompensatedPotions(GrimPlayer player) {
        this.player = player;
    }

    public Integer getJumpAmplifier() {
        return getPotionLevel(PotionTypes.JUMP_BOOST);
    }

    public Integer getLevitationAmplifier() {
        return getPotionLevel(PotionTypes.LEVITATION);
    }

    public Integer getSlowFallingAmplifier() {
        return getPotionLevel(PotionTypes.SLOW_FALLING);
    }

    public Integer getDolphinsGraceAmplifier() {
        return getPotionLevel(PotionTypes.DOLPHINS_GRACE);
    }

    public void addPotionEffect(PotionType type, int level, int entityID) {
        player.latencyUtils.addRealTimeTask(player.lastTransactionSent.get() + 1, () -> {
            ConcurrentHashMap<PotionType, Integer> potions = potionsMap.get(entityID);

            if (potions == null) {
                potions = new ConcurrentHashMap<>();
                potionsMap.put(entityID, potions);
            }

            player.pointThreeEstimator.updatePlayerPotions(type, level);
            potions.put(type, level);
        });
    }

    public void removePotionEffect(PotionType type, int entityID) {
        player.latencyUtils.addRealTimeTask(player.lastTransactionSent.get() + 1, () -> {
            ConcurrentHashMap<PotionType, Integer> potions = potionsMap.get(entityID);

            player.pointThreeEstimator.updatePlayerPotions(type, null);

            if (potions != null) {
                potions.remove(type);
            }
        });
    }

    public Integer getPotionLevel(PotionType type) {
        ConcurrentHashMap<PotionType, Integer> effects;
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
