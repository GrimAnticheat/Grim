package ac.grim.grimac.utils.events;

import ac.grim.grimac.checks.Check;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class CommandExecuteEvent extends FlagEvent {
    private static final HandlerList handlers = new HandlerList();
    private final String command;

    public CommandExecuteEvent(Check check, String command) {
        super(check); // Async!
        this.command = command;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    @NotNull
    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public String getCommand() {
        return command;
    }
}
