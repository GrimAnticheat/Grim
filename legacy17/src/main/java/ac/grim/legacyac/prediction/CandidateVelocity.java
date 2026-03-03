package ac.grim.legacyac.prediction;

public final class CandidateVelocity {
    private final String profile;
    private final double motionX;
    private final double motionY;
    private final double motionZ;

    public CandidateVelocity(String profile, double motionX, double motionY, double motionZ) {
        this.profile = profile;
        this.motionX = motionX;
        this.motionY = motionY;
        this.motionZ = motionZ;
    }

    public String getProfile() {
        return profile;
    }

    public double getMotionX() {
        return motionX;
    }

    public double getMotionY() {
        return motionY;
    }

    public double getMotionZ() {
        return motionZ;
    }

    public double getHorizontalMagnitude() {
        return Math.sqrt((motionX * motionX) + (motionZ * motionZ));
    }
}
