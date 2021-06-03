package ac.grim.grimac.utils.data;

public class ChangeBlockData {
    public int transaction;
    public int blockX;
    public int blockY;
    public int blockZ;
    public int combinedID;

    public ChangeBlockData(int transaction, int blockX, int blockY, int blockZ, int combinedID) {
        this.transaction = transaction;
        this.blockX = blockX;
        this.blockY = blockY;
        this.blockZ = blockZ;
        this.combinedID = combinedID;
    }
}
