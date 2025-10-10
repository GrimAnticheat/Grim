package ac.grim.grimac.utils.data;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.Contract;

@AllArgsConstructor
@Getter
@Setter
public final class CooldownData {
    private int ticksRemaining;
    private final int transaction;

    @Contract(mutates = "this")
    public void tick() {
        ticksRemaining--;
    }
}
