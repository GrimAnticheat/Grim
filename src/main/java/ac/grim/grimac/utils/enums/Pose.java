package ac.grim.grimac.utils.enums;

import ac.grim.grimac.player.GrimPlayer;

public enum Pose {
    STANDING(0.6f, 1.8f),
    FALL_FLYING(0.6f, 0.6f),
    SLEEPING(0.2f, 0.2f),
    SWIMMING(0.6f, 0.6f),
    SPIN_ATTACK(0.6f, 0.6f),
    CROUCHING(0.6f, 1.5f),
    DYING(0.2f, 0.2f),

    // Non-player poses
    NINE_CROUCHING(0.6f, 1.65f), // 1.9-1.13 clients have a slightly different crouching hitbox
    LONG_JUMPING(0.6f, 1.8f); // DUMMY (players can't have this pose)

    public float width;
    public float height;

    Pose(float width, float height) {
        this.width = width;
        this.height = height;
    }

    public static Pose getFromIndex(GrimPlayer player, int index) {
        switch (index) {
            case 0:
                return STANDING;
            case 1:
                return FALL_FLYING;
            case 2:
                return SLEEPING;
            case 3:
                return SWIMMING;
            case 4:
                return SPIN_ATTACK;
            case 5:
                return player.getSneakingPose();
            case 6:
                return DYING;
        }
        return STANDING;
    }
}
