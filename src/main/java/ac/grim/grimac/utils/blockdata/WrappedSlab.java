package ac.grim.grimac.utils.blockdata;

public class WrappedSlab extends WrappedBlockDataValue {
    boolean isBottom = true;
    boolean isDouble = false;

    // This can only happen in 1.13+ when single and double slabs were combined
    public boolean isDouble() {
        return isDouble;
    }

    public boolean isBottom() {
        return isBottom;
    }
}
