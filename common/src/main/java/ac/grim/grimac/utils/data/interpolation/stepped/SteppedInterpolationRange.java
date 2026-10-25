package ac.grim.grimac.utils.data.interpolation.stepped;

public final class SteppedInterpolationRange {

    public double minimumProgress = 1;
    public double maximumProgress = 1;
    public double minimumSpeed = 1;
    public double maximumSpeed = 1;

    public double sampledMinimumProgress;
    public double sampledMaximumProgress;
    public double roundingError;

    public void tick(double confirmedDuration, double availableDuration, double retiredDuration, int interpolationInterval, double optionalDuration) {
        // The lower bound only includes movement confirmed by a transaction
        sampledMinimumProgress = Math.min(minimumProgress, confirmedDuration);
        sampledMaximumProgress = Math.min(maximumProgress, availableDuration);

        // The client uses floats, so elapsed and remaining ticks don't always add up exactly
        // Allow for that rounding until interpolation finishes, even after a transaction
        double clockRoundingError = 16 * Math.ulp((float) (availableDuration - retiredDuration + 1));
        roundingError += clockRoundingError;

        double minimumRemainingDuration = Math.max(0, confirmedDuration - maximumProgress + 1 - optionalDuration - roundingError);
        double maximumRemainingDuration = Math.max(0, availableDuration - minimumProgress + 1 + roundingError);
        double slowestSpeed = nextSpeed(minimumSpeed, minimumRemainingDuration, interpolationInterval);
        double fastestSpeed = nextSpeed(maximumSpeed, maximumRemainingDuration, interpolationInterval);

        // The further along we are, the fewer ticks we have left
        // Work out the remaining ticks separately for each bound
        // Using the smallest remaining time with the lowest progress can make the lower bound get stuck
        double nextMinimumProgress = minimumProgress + nextSpeed(minimumSpeed, Math.max(0, confirmedDuration - minimumProgress + 1 - optionalDuration - roundingError), interpolationInterval);
        double nextMaximumProgress = maximumProgress + nextSpeed(maximumSpeed, Math.max(0, availableDuration - maximumProgress + 1 + roundingError), interpolationInterval);

        // The client resets its speed to 1 when interpolation finishes
        // Allow that for the lower bound as soon as it could have finished, but don't reset
        // the upper bound until we're sure it has finished
        boolean mayFinish = maximumProgress + fastestSpeed >= confirmedDuration + 1 - optionalDuration - roundingError;
        boolean mustFinish = minimumProgress + slowestSpeed >= availableDuration + 1 + roundingError;
        double speedRoundingError = 8 * Math.ulp((float) fastestSpeed);

        minimumSpeed = mayFinish ? 1 : Math.max(1, slowestSpeed - speedRoundingError);
        maximumSpeed = mustFinish ? 1 : fastestSpeed + speedRoundingError;

        // The client advances the timer after moving, so it can end up one tick past the last step
        // Widen the bounds for float rounding when moving from one step to the next
        minimumProgress = Math.max(sampledMinimumProgress, Math.min(confirmedDuration + 1 - roundingError, nextMinimumProgress) - clockRoundingError);
        maximumProgress = Math.min(availableDuration + 1 + roundingError, nextMaximumProgress) + clockRoundingError;
    }

    public void skipTicks(double availableDuration, double retiredDuration, int interpolationInterval) {
        // We don't know how many ticks passed when the player isn't ticking reliably
        // A split transaction alone doesn't cause this, we still count those ticks normally
        roundingError += 16 * Math.ulp((float) (availableDuration - retiredDuration + 1)) * (availableDuration - retiredDuration + 1);
        maximumSpeed = Math.max(maximumSpeed, Math.max(1, (availableDuration - minimumProgress + 1 + roundingError) / interpolationInterval));
        minimumSpeed = 1;
        maximumProgress = availableDuration + 1 + roundingError;
        sampledMaximumProgress = availableDuration;
    }

    public void reset() {
        minimumProgress = 1;
        maximumProgress = 1;
        minimumSpeed = 1;
        maximumSpeed = 1;
        sampledMinimumProgress = 0;
        sampledMaximumProgress = 0;
        roundingError = 0;
    }

    private static double nextSpeed(double previousSpeed, double remainingDuration, int interpolationInterval) {
        return previousSpeed + (Math.max(remainingDuration / interpolationInterval, 1) - previousSpeed) / interpolationInterval;
    }

}
