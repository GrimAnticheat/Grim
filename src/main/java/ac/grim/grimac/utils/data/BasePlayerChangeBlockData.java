package ac.grim.grimac.utils.data;

import com.google.common.base.Objects;
import lombok.Data;

@Data
public abstract class BasePlayerChangeBlockData {
    public final int transaction;
    public final int blockX;
    public final int blockY;
    public final int blockZ;

    public BasePlayerChangeBlockData(int transaction, int blockX, int blockY, int blockZ) {
        this.transaction = transaction;
        this.blockX = blockX;
        this.blockY = blockY;
        this.blockZ = blockZ;
    }

    public abstract int getCombinedID();

    @Override
    public int hashCode() {
        return Objects.hashCode(transaction, blockX, blockY, blockZ);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BasePlayerChangeBlockData)) return false;
        BasePlayerChangeBlockData that = (BasePlayerChangeBlockData) o;
        return transaction == that.transaction && blockX == that.blockX && blockY == that.blockY && blockZ == that.blockZ;
    }
}
