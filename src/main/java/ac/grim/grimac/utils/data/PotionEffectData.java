package ac.grim.grimac.utils.data;

public class PotionEffectData {
    public int transaction;
    public String type;
    public int level;
    public int entityID;

    public PotionEffectData(int transaction, String type, int level, int entityID) {
        this.transaction = transaction;
        this.type = type;
        this.level = level;
        this.entityID = entityID;
    }
}

