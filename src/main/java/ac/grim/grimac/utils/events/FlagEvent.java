package ac.grim.grimac.utils.events;

import ac.grim.grimac.player.GrimPlayer;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class FlagEvent extends Event implements Cancellable {
    private static final HandlerList handlers = new HandlerList();
    private final GrimPlayer player;
    private final String checkName;
    private final double violations;
    private boolean cancelled;

    public FlagEvent(GrimPlayer player, String checkName, double violations) {
        super(true); // Async!
        this.player = player;
        this.checkName = checkName;
        this.violations = violations;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        cancelled = cancel;
    }

    public GrimPlayer getPlayer() {
        return player;
    }

    public String getCheckName() {
        return checkName;
    }

    public double getViolations() {
        return violations;
    }

    @NotNull
    @Override
    public HandlerList getHandlers() {
        return handlers;
    }
}
