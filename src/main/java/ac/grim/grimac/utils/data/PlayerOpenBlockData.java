package ac.grim.grimac.utils.data;

import org.apache.commons.lang.NotImplementedException;

public class PlayerOpenBlockData extends BasePlayerChangeBlockData {

    public PlayerOpenBlockData(int transaction, int blockX, int blockY, int blockZ) {
        super(transaction, blockX, blockY, blockZ);
    }

    @Override
    public int getCombinedID() {
        throw new NotImplementedException();
    }

    @Override
    public int hashCode() {
        return uniqueID;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PlayerOpenBlockData)) return false;
        if (!super.equals(o)) return false;
        PlayerOpenBlockData that = (PlayerOpenBlockData) o;
        return uniqueID == that.uniqueID;
    }
}
