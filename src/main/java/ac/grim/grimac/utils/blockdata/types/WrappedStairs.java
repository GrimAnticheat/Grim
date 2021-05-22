package ac.grim.grimac.utils.blockdata.types;

public class WrappedStairs extends WrappedDirectional {
    boolean isUpsideDown = false;

    public boolean getUpsideDown() {
        return isUpsideDown;
    }

    public void setUpsideDown(boolean isUpsideDown) {
        this.isUpsideDown = isUpsideDown;
    }
}
