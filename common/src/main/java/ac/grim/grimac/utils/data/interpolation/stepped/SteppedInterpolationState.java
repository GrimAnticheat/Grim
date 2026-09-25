package ac.grim.grimac.utils.data.interpolation.stepped;

import ac.grim.grimac.utils.collisions.datatypes.SimpleCollisionBox;
import com.github.retrooper.packetevents.protocol.vector.positionpath.PositionPath;
import com.github.retrooper.packetevents.protocol.vector.positionpath.SteppedPositionPath;
import com.github.retrooper.packetevents.util.Vector3d;

import java.util.ArrayList;
import java.util.List;

public final class SteppedInterpolationState {

    public record Step(Vector3d position, float yaw, float pitch, int durationTicks, int transaction, int optionalDurationTicks, SimpleCollisionBox positionBounds) {
        public Step {
            if (positionBounds == null) {
                positionBounds = SteppedInterpolationUtils.pointBounds(position);
            }
        }

        public Step(Vector3d position, float yaw, float pitch, int durationTicks, int transaction) {
            this(position, yaw, pitch, durationTicks, transaction, 0, null);
        }

        public int maximumDurationTicks() {
            return durationTicks + optionalDurationTicks;
        }

        public Step withPositionBounds(SimpleCollisionBox updatedBounds) {
            return new Step(position, yaw, pitch, durationTicks, transaction, optionalDurationTicks, updatedBounds);
        }
    }

    public double currentX;
    public double currentY;
    public double currentZ;
    public float yaw;
    public float pitch;

    public double segmentStartX;
    public double segmentStartY;
    public double segmentStartZ;
    public float segmentStartYaw;
    public float segmentStartPitch;

    public float lastSampleYaw;
    public float lastSamplePitch;

    public Vector3d targetPosition = Vector3d.zero();
    public float targetYaw;
    public float targetPitch;

    public final List<Step> steps = new ArrayList<>();
    public int currentStepIndex;
    public double availableDuration;

    public float nextSampleTicks;
    public float remainingTicks;
    public float interpolationSpeed = 1.0F;

    private SteppedInterpolationState() {
    }

    public static SteppedInterpolationState initial(SimpleCollisionBox position, float yaw, float pitch) {
        SteppedInterpolationState state = new SteppedInterpolationState();
        state.currentX = position.minX;
        state.currentY = position.minY;
        state.currentZ = position.minZ;
        state.yaw = yaw;
        state.pitch = pitch;
        return state;
    }

    public void appendMovement(PositionPath path, Vector3d position, float yaw, float pitch, int interpolationInterval, int transaction) {
        // All steps use the previous target rotation, so don't update it until we've added them all
        if (path instanceof SteppedPositionPath stepped && !position.equals(targetPosition)) {
            int totalTicks = 0;
            for (SteppedPositionPath.Step step : stepped.getSteps()) {
                totalTicks += step.tickOffset();
            }

            int elapsedTicks = 0;
            for (SteppedPositionPath.Step step : stepped.getSteps()) {
                elapsedTicks += step.tickOffset();
                float progress = (float) elapsedTicks / totalTicks;
                float stepYaw = yaw == targetYaw ? yaw : SteppedInterpolationUtils.lerpYaw(progress, targetYaw, yaw);
                float stepPitch = pitch == targetPitch ? pitch : SteppedInterpolationUtils.lerp(progress, targetPitch, pitch);
                appendStep(new Step(step.position(), stepYaw, stepPitch, step.tickOffset(), transaction));
            }
        } else {
            appendStep(new Step(position, yaw, pitch, interpolationInterval, transaction));
        }

        setTarget(position, yaw, pitch);
        rememberRotation();
    }

    public void appendOptionalStep(Vector3d position, float yaw, float pitch, int optionalDuration, SimpleCollisionBox bounds, int transaction) {
        appendStep(new Step(position, yaw, pitch, 0, transaction, optionalDuration, bounds));
        setTarget(position, yaw, pitch);
    }

    private void appendStep(Step step) {
        if (!hasSteps()) {
            segmentStartX = currentX;
            segmentStartY = currentY;
            segmentStartZ = currentZ;
            segmentStartYaw = yaw;
            segmentStartPitch = pitch;
            nextSampleTicks = 1.0F;
        }

        discardConsumedSteps();
        steps.add(step);

        // Only the bounds include time for optional steps, since the client may ignore them
        remainingTicks += step.durationTicks();
        availableDuration += step.maximumDurationTicks();
    }

    private void setTarget(Vector3d position, float yaw, float pitch) {
        targetPosition = position;
        targetYaw = yaw;
        targetPitch = pitch;
    }

    public void advance(int interpolationInterval) {
        if (!hasSteps()) {
            remainingTicks = 0.0F;
            interpolationSpeed = 1.0F;
            return;
        }

        adjustRotationFromDeltas();

        // Move the entity before advancing the timer, just like the client
        // Doing this the other way around moves it too far on the first tick
        samplePositionAndRotation();
        advanceClock(interpolationInterval);
        rememberRotation();
    }

    private void samplePositionAndRotation() {
        while (currentStepIndex < steps.size()) {
            Step step = steps.get(currentStepIndex);

            if (nextSampleTicks < step.durationTicks()) {
                break;
            }

            nextSampleTicks -= step.durationTicks();
            segmentStartX = step.position().x;
            segmentStartY = step.position().y;
            segmentStartZ = step.position().z;
            segmentStartYaw = step.yaw();
            segmentStartPitch = step.pitch();
            currentStepIndex++;
        }

        if (currentStepIndex == steps.size()) {
            currentX = targetPosition.x;
            currentY = targetPosition.y;
            currentZ = targetPosition.z;
            yaw = targetYaw;
            pitch = targetPitch;

            steps.clear();
            currentStepIndex = 0;
        } else {
            Step step = steps.get(currentStepIndex);
            float progress = nextSampleTicks / step.durationTicks();

            currentX = segmentStartX + progress * (step.position().x - segmentStartX);
            currentY = segmentStartY + progress * (step.position().y - segmentStartY);
            currentZ = segmentStartZ + progress * (step.position().z - segmentStartZ);
            yaw = SteppedInterpolationUtils.lerpYaw(progress, segmentStartYaw, step.yaw());
            pitch = SteppedInterpolationUtils.lerp(progress, segmentStartPitch, step.pitch());
        }

        yaw %= 360.0F;
        pitch = Math.max(-90.0F, Math.min(90.0F, pitch % 360.0F));
    }

    private void advanceClock(int interpolationInterval) {
        float targetSpeed = Math.max(remainingTicks / interpolationInterval, 1.0F);
        interpolationSpeed = SteppedInterpolationUtils.lerp(1.0F / interpolationInterval, interpolationSpeed, targetSpeed);

        float tickAdvance;
        if (interpolationSpeed < remainingTicks) {
            tickAdvance = interpolationSpeed;
        } else {
            tickAdvance = remainingTicks;
            interpolationSpeed = 1.0F;
        }

        nextSampleTicks += tickAdvance;
        remainingTicks -= tickAdvance;
    }

    public void snap(Vector3d position, float yaw, float pitch) {
        this.currentX = position.x;
        this.currentY = position.y;
        this.currentZ = position.z;
        this.yaw = yaw % 360.0F;
        this.pitch = Math.max(-90.0F, Math.min(90.0F, pitch % 360.0F));
    }

    private void rememberRotation() {
        lastSampleYaw = yaw;
        lastSamplePitch = pitch;
    }

    private void adjustRotationFromDeltas() {
        float yawDelta = yaw - lastSampleYaw;
        float pitchDelta = pitch - lastSamplePitch;
        if (yawDelta == 0 && pitchDelta == 0) {
            return;
        }

        // If the rotation changed while interpolating, adjust the queued rotations too
        // Position changes are handled by reset() or the teleport bounds in SteppedEntityInterpolation
        // Keep the additions of zero to handle negative zero the same way as vanilla
        targetPosition = targetPosition.add(0, 0, 0);
        targetYaw += yawDelta;
        targetPitch += pitchDelta;
        segmentStartX += 0.0;
        segmentStartY += 0.0;
        segmentStartZ += 0.0;
        segmentStartYaw += yawDelta;
        segmentStartPitch += pitchDelta;

        for (int stepIndex = currentStepIndex; stepIndex < steps.size(); stepIndex++) {
            Step step = steps.get(stepIndex);
            steps.set(stepIndex, new Step(step.position().add(0, 0, 0), step.yaw() + yawDelta, step.pitch() + pitchDelta, step.durationTicks(), step.transaction()));
        }
    }

    public Vector3d resolveTargetPosition(PositionPath path) {
        if (path != null) {
            return path.getEndPosition();
        }

        return hasSteps() ? targetPosition : new Vector3d(currentX, currentY, currentZ);
    }

    public float resolveTargetYaw(Float packetYaw) {
        if (packetYaw != null) {
            return packetYaw;
        }

        return hasSteps() ? targetYaw : yaw;
    }

    public float resolveTargetPitch(Float packetPitch) {
        if (packetPitch != null) {
            return packetPitch;
        }

        return hasSteps() ? targetPitch : pitch;
    }

    private void discardConsumedSteps() {
        if (currentStepIndex != 0) {
            steps.subList(0, currentStepIndex).clear();
            currentStepIndex = 0;
        }
    }

    public boolean hasSteps() {
        return currentStepIndex < steps.size();
    }

}
