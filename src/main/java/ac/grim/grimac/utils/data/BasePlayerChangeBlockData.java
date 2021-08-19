package ac.grim.grimac.utils.data;

import lombok.Data;

import java.util.concurrent.atomic.AtomicInteger;

@Data
public abstract class BasePlayerChangeBlockData {
    private static final AtomicInteger id = new AtomicInteger(0);
    public final int transaction;
    public final int blockX;
    public final int blockY;
    public final int blockZ;
    public final int uniqueID;

    public BasePlayerChangeBlockData(int transaction, int blockX, int blockY, int blockZ) {
        this.transaction = transaction;
        this.blockX = blockX;
        this.blockY = blockY;
        this.blockZ = blockZ;
        this.uniqueID = id.getAndIncrement();
    }

    public abstract int getCombinedID();

    @Override
    public int hashCode() {
        return uniqueID;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BasePlayerChangeBlockData)) return false;
        BasePlayerChangeBlockData that = (BasePlayerChangeBlockData) o;
        return transaction == that.transaction && blockX == that.blockX && blockY == that.blockY && blockZ == that.blockZ && uniqueID == that.uniqueID;
    }
}
