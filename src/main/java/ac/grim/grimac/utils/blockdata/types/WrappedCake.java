package ac.grim.grimac.utils.blockdata.types;

public class WrappedCake extends WrappedBlockDataValue {
    int slices = 0;

    public int getSlicesEaten() {
        return slices;
    }

    public void setSlices(int slices) {
        this.slices = slices;
    }
}
