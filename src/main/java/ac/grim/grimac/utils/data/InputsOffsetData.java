package ac.grim.grimac.utils.data;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.bukkit.util.Vector;

@AllArgsConstructor
@Data
public class InputsOffsetData {
    Vector inputs;
    double offset;
    boolean sprinting, useItem, sneaking;
}
