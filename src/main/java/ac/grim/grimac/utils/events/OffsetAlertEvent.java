package ac.grim.grimac.utils.events;

import ac.grim.grimac.player.GrimPlayer;

public class OffsetAlertEvent extends FlagEvent {
    private final double offset;
    private final boolean vehicle;
    private final double violations;
    private boolean cancelled;

    public OffsetAlertEvent(GrimPlayer player, String checkName, double offset, double violations, boolean vehicle) {
        super(player, checkName, violations);
        this.offset = offset;
        this.vehicle = vehicle;
        this.violations = violations;
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public void setCancelled(boolean cancel) {
        cancelled = cancel;
    }

    public double getOffset() {
        return offset;
    }

    public double getViolations() {
        return violations;
    }

    public boolean isVehicle() {
        return vehicle;
    }
}
