package ac.grim.grimac.utils.events;

import ac.grim.grimac.checks.Check;

public class OffsetAlertEvent extends FlagEvent {
    private final double offset;
    private final String checkName;
    private final boolean vehicle;
    private final double violations;
    private final boolean isAlert;
    private final boolean isSetback;
    private boolean cancelled;

    public OffsetAlertEvent(Check check, String checkName, double offset, double violations, boolean vehicle, boolean isAlert, boolean isSetback) {
        super(check);
        this.checkName = checkName;
        this.offset = offset;
        this.vehicle = vehicle;
        this.violations = violations;
        this.isAlert = isAlert;
        this.isSetback = isSetback;
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

    @Override
    public double getViolations() {
        return violations;
    }

    public boolean isVehicle() {
        return vehicle;
    }

    @Override
    public String getCheckName() {
        return checkName;
    }

    @Override
    public boolean isAlert() {
        return isAlert;
    }

    @Override
    public boolean isSetback() {
        return isSetback;
    }
}
