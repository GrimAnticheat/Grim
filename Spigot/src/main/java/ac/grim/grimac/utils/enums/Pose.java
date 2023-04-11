package ac.grim.grimac.utils.enums;

public enum Pose {
    STANDING(0.6f, 1.8f, 1.62f),
    FALL_FLYING(0.6f, 0.6f, 0.4f),
    SLEEPING(0.2f, 0.2f, 0.2f),
    SWIMMING(0.6f, 0.6f, 0.4f),
    SPIN_ATTACK(0.6f, 0.6f, 0.4f),
    CROUCHING(0.6f, 1.5f, 1.27f),
    DYING(0.2f, 0.2f, 0.2f),

    // Non-player poses
    NINE_CROUCHING(0.6f, 1.65f, 1.54f), // 1.9-1.13 clients have a slightly different crouching hitbox
    LONG_JUMPING(0.6f, 1.8f, 1.54f); // DUMMY (players can't have this pose)

    public final float width;
    public final float height;
    public final float eyeHeight;

    Pose(float width, float height, float eyeHeight) {
        this.width = width;
        this.height = height;
        this.eyeHeight = eyeHeight;
    }
}
