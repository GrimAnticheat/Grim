package ac.grim.grimac.utils.data;

public class ChangeBlockData extends BasePlayerChangeBlockData {
    public int combinedID;

    public ChangeBlockData(int transaction, int blockX, int blockY, int blockZ, int combinedID) {
        super(transaction, blockX, blockY, blockZ);
        this.combinedID = combinedID;
    }

    @Override
    public int getCombinedID() {
        return combinedID;
    }
}
