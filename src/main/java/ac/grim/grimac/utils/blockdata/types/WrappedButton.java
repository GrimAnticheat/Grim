package ac.grim.grimac.utils.blockdata.types;

public class WrappedButton extends WrappedDirectional {
    boolean isPowered = false;

    public boolean isPowered() {
        return isPowered;
    }

    public void setPowered(boolean isPowered) {
        this.isPowered = isPowered;
    }
}
