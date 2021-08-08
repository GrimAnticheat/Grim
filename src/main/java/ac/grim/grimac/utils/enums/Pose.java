package ac.grim.grimac.utils.enums;

public enum Pose {
    STANDING(0.6f, 1.8f),
    FALL_FLYING(0.6f, 0.6f),
    SLEEPING(0.2f, 0.2f),
    SWIMMING(0.6f, 0.6f),
    SPIN_ATTACK(0.6f, 0.6f),
    NINE_CROUCHING(0.6f, 1.65f), // 1.9-1.13 clients have a slightly different crouching hitbox
    CROUCHING(0.6f, 1.5f),
    DYING(0.2f, 0.2f),
    LONG_JUMPING(0.6f, 1.8f); // DUMMY (players can't have this pose)

    public float width;
    public float height;

    Pose(float width, float height) {
        this.width = width;
        this.height = height;
    }
}
