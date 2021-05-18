package ac.grim.grimac.utils.blockdata;

public class WrappedTrapdoor extends WrappedDirectional {
    boolean isOpen = true;

    public boolean isOpen() {
        return isOpen;
    }

    public void setOpen(boolean isOpen) {
        this.isOpen = isOpen;
    }
}
