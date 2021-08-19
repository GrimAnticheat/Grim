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

    @Override
    public int hashCode() {
        return uniqueID;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ChangeBlockData)) return false;
        if (!super.equals(o)) return false;
        ChangeBlockData that = (ChangeBlockData) o;
        return combinedID == that.combinedID;
    }
}
