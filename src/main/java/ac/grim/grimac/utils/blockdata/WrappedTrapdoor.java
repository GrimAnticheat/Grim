package ac.grim.grimac.utils.blockdata;

public class WrappedTrapdoor extends WrappedDirectional {
    boolean isOpen = true;
    boolean isBottom = true;

    public boolean isOpen() {
        return isOpen;
    }

    public boolean isBottom() {
        return isBottom;
    }

    public void setOpen(boolean isOpen) {
        this.isOpen = isOpen;
    }

    public void setBottom(boolean isBottom) {
        this.isBottom = isBottom;
    }
}
