package ac.grim.grimac.utils.data;

import com.google.common.base.Objects;
import org.apache.commons.lang.NotImplementedException;

import java.util.concurrent.atomic.AtomicInteger;

public class PlayerOpenBlockData extends BasePlayerChangeBlockData {

    private static final AtomicInteger id = new AtomicInteger(0);
    private final int uniqueID;

    public PlayerOpenBlockData(int transaction, int blockX, int blockY, int blockZ) {
        super(transaction, blockX, blockY, blockZ);
        uniqueID = id.getAndIncrement();
    }

    @Override
    public int getCombinedID() {
        throw new NotImplementedException();
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(super.hashCode(), uniqueID);
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
