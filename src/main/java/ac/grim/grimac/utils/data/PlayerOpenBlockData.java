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
}
