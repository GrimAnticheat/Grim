package ac.grim.grimac.utils.blockdata.types;

public class WrappedRails extends WrappedBlockDataValue {
    boolean isAscending = false;

    public boolean isAscending() {
        return isAscending;
    }

    public void setAscending(boolean isAscending) {
        this.isAscending = isAscending;
    }
}
