package ac.grim.grimac.utils.data;

public class WorldChangeBlockData {
    public int tick;
    public int blockX;
    public int blockY;
    public int blockZ;
    public int blockID;

    public WorldChangeBlockData(int tick, int blockX, int blockY, int blockZ, int blockID) {
        this.tick = tick;
        this.blockX = blockX;
        this.blockY = blockY;
        this.blockZ = blockZ;
        this.blockID = blockID;
    }
}
