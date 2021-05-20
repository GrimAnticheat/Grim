package ac.grim.grimac.utils.blockdata.types;

public class WrappedDoor extends WrappedDirectional {
    boolean isOpen = true;
    boolean isBottom = true;
    boolean isRightHinge = true;

    public boolean getOpen() {
        return isOpen;
    }

    public void setOpen(boolean isOpen) {
        this.isOpen = isOpen;
    }

    public boolean isRightHinge() {
        return isRightHinge;
    }

    public void setRightHinge(boolean isRightHinge) {
        this.isRightHinge = isRightHinge;
    }

    public boolean isBottom() {
        return isBottom;
    }

    public void setBottom(boolean isBottom) {
        this.isBottom = isBottom;
    }
}
