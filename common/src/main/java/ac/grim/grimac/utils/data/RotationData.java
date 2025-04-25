package ac.grim.grimac.utils.data;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import org.jetbrains.annotations.Contract;

@Getter
@RequiredArgsConstructor
@EqualsAndHashCode
@ToString
public final class RotationData {
    private final float yaw;
    private final float pitch;
    private final int transaction;
    private boolean isAccepted;

    @Contract(mutates = "this")
    public void accept() {
        this.isAccepted = true;
    }
}
