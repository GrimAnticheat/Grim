package ac.grim.grimac.utils.events;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.player.GrimPlayer;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class FlagEvent extends Event implements Cancellable {
    private static final HandlerList handlers = new HandlerList();
    private final Check check;
    private boolean cancelled;

    public FlagEvent(Check check) {
        super(true); // Async!
        this.check = check;
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
        return check.getPlayer();
    }

    public String getCheckName() {
        return check.getCheckName();
    }

    public double getViolations() {
        return check.getViolations();
    }

    @NotNull
    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    public boolean isAlert() {
        return check.shouldAlert();
    }

    public boolean isSetback() {
        return check.getViolations() > check.getSetbackVL();
    }
}
