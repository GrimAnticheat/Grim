package ac.grim.grimac.checks.debug;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.player.GrimPlayer;

public abstract class AbstractDebugHandler extends Check {
    public AbstractDebugHandler(GrimPlayer player) {
        super(player);
    }

    public abstract void toggleListener(GrimPlayer player);

    public abstract boolean toggleConsoleOutput();
}
