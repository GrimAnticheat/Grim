package ac.grim.grimac.utils.blockdata.types;

public class WrappedTripwire extends WrappedBlockDataValue {
    boolean isAttached;

    public boolean isAttached() {
        return isAttached;
    }

    public void setAttached(boolean attached) {
        isAttached = attached;
    }
}
