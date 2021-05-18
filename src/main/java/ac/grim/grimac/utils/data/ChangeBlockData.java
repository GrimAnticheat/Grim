package ac.grim.grimac.utils.data;

public class ChangeBlockData {
    public int tick;
    public int blockX;
    public int blockY;
    public int blockZ;
    public int combinedID;

    public ChangeBlockData(int tick, int blockX, int blockY, int blockZ, int combinedID) {
        this.tick = tick;
        this.blockX = blockX;
        this.blockY = blockY;
        this.blockZ = blockZ;
        this.combinedID = combinedID;
    }
}
