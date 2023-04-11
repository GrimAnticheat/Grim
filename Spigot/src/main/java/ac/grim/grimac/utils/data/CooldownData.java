package ac.grim.grimac.utils.data;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Getter
@Setter
public class CooldownData {
    int ticksRemaining;
    int transaction;

    public void tick() {
        ticksRemaining--;
    }
}
