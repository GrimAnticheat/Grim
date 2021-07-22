package ac.grim.grimac.utils.latency;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.data.PotionEffectData;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class CompensatedPotions {
    private final GrimPlayer player;
    private final ConcurrentHashMap<Integer, ConcurrentHashMap<String, Integer>> potionsMap = new ConcurrentHashMap<>();
    private final ConcurrentLinkedQueue<PotionEffectData> queuedPotions = new ConcurrentLinkedQueue<>();

    public CompensatedPotions(GrimPlayer player) {
        this.player = player;
    }

    public void addPotionEffect(String type, int level, int entityID) {
        queuedPotions.add(new PotionEffectData(player.lastTransactionSent.get() + 1, type, level, entityID));
    }

    public void removePotionEffect(String type, int entityID) {
        queuedPotions.add(new PotionEffectData(player.lastTransactionSent.get() + 1, type, 0, entityID));
    }

    public int getPotionLevel(String type) {
        ConcurrentHashMap<String, Integer> effects;
        if (player.packetStateData.vehicle == null) {
            effects = potionsMap.get(player.entityID);
        } else {
            effects = potionsMap.get(player.packetStateData.vehicle);
        }

        if (effects == null)
            return 0;

        Integer level = effects.get(type);
        return level == null ? 0 : level;
    }

    public void removeEntity(int entityID) {
        potionsMap.remove(entityID);
    }

    public void handleTransactionPacket(int lastTransactionReceived) {
        while (true) {
            PotionEffectData data = queuedPotions.peek();

            if (data == null) break;

            // The packet has 100% arrived
            if (data.transaction > lastTransactionReceived) break;
            queuedPotions.poll();

            ConcurrentHashMap<String, Integer> potions = potionsMap.get(data.entityID);

            if (data.level == 0) {
                if (potions != null) {
                    potions.remove(data.type);
                }
            } else {
                if (potions == null) {
                    potions = new ConcurrentHashMap<>();
                    potionsMap.put(data.entityID, potions);
                }

                potions.put(data.type, data.level);
            }
        }
    }
}
