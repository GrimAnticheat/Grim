package ac.grim.grimac.utils.data;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TeleportAcceptData {
    private boolean isTeleport;
    private SetBackData setback;
    private TeleportData teleportData;
}
