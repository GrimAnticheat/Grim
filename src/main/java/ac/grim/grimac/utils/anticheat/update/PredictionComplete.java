package ac.grim.grimac.utils.anticheat.update;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PredictionComplete {
    private double offset;
    private PositionUpdate data;
    private int identifier;

    public PredictionComplete(double offset, PositionUpdate update) {
        this.offset = offset;
        this.data = update;
    }
}
