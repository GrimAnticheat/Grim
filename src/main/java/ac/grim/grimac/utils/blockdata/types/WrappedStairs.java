package ac.grim.grimac.utils.blockdata.types;

public class WrappedStairs extends WrappedDirectional {
    boolean isUpsideDown = false;
    int shapeOrdinal = 0;

    public boolean getUpsideDown() {
        return isUpsideDown;
    }

    public void setUpsideDown(boolean isUpsideDown) {
        this.isUpsideDown = isUpsideDown;
    }

    public int getShapeOrdinal() {
        return shapeOrdinal;
    }

    public void setShapeOrdinal(int ordinal) {
        this.shapeOrdinal = ordinal;
    }
}
