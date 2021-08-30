package ac.grim.grimac.utils.anticheat.update;

import ac.grim.grimac.utils.data.PredictionData;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Getter
@Setter
public class PredictionComplete {
    private double offset;
    private PredictionData data;
}
