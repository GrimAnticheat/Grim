package ac.grim.grimac.checks.debug;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.player.GrimPlayer;

public abstract class AbstractDebugHandler extends Check {

    protected final GrimPlayer grimPlayer;

    public AbstractDebugHandler(GrimPlayer grimPlayer) {
        super(grimPlayer);
        this.grimPlayer = grimPlayer;
    }

    public GrimPlayer getPlayer() {
        return grimPlayer;
    }

    public abstract void toggleListener(GrimPlayer player);

    public abstract boolean toggleConsoleOutput();
}
