package ac.grim.grimac.utils.blockdata.types;

public class WrappedSlab extends WrappedBlockDataValue {
    boolean isBottom = true;
    boolean isDouble = false;

    // This can only happen in 1.13+ when single and double slabs were combined
    public boolean isDouble() {
        return isDouble;
    }

    public void setDouble(boolean isDouble) {
        this.isDouble = isDouble;
    }

    public boolean isBottom() {
        return isBottom;
    }

    public void setBottom(boolean isBottom) {
        this.isBottom = isBottom;
    }
}
