package ac.grim.grimac.utils.data;

public class PlayerOpenBlockData {
    public int transaction;
    public int blockX;
    public int blockY;
    public int blockZ;

    public PlayerOpenBlockData(int transaction, int blockX, int blockY, int blockZ) {
        this.transaction = transaction;
        this.blockX = blockX;
        this.blockY = blockY;
        this.blockZ = blockZ;
    }
}
