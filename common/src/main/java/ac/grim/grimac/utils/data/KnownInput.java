package ac.grim.grimac.utils.data;

import org.jetbrains.annotations.Contract;

public record KnownInput(boolean forward, boolean backward, boolean left, boolean right,
                         boolean jump, boolean shift, boolean sprint) {
    @Contract(pure = true)
    public boolean moving() {
        return forward || backward || left || right || jump;
    }
}
