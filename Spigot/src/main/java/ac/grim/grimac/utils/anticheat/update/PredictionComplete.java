package ac.grim.grimac.utils.anticheat.update;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PredictionComplete {
    private double offset;
    private PositionUpdate data;
    private boolean checked;
    private int identifier;

    public PredictionComplete(double offset, PositionUpdate update, boolean checked) {
        this.offset = offset;
        this.data = update;
        this.checked = checked;
    }
}
