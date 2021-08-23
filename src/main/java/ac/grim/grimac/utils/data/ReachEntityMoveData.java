package ac.grim.grimac.utils.data;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class ReachEntityMoveData {
    int entityID;
    double x, y, z;
    boolean relative;
}
