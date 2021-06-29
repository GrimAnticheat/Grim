package ac.grim.grimac.utils.data;

public abstract class BasePlayerChangeBlockData {
    public int transaction;
    public int blockX;
    public int blockY;
    public int blockZ;

    public BasePlayerChangeBlockData(int transaction, int blockX, int blockY, int blockZ) {
        this.transaction = transaction;
        this.blockX = blockX;
        this.blockY = blockY;
        this.blockZ = blockZ;
    }

    public abstract int getCombinedID();
}
