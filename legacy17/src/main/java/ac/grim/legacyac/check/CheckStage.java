package ac.grim.legacyac.check;

/**
 * Execution stage for checks, aligned with Grim's stage-based pipeline model.
 *
 * <p>
 * Pipeline order: PRE → PREDICTION → POST → FALLBACK
 * </p>
 *
 * <ul>
 * <li>{@link #PRE} — Packet-level preprocessing (timing, inventory state)</li>
 * <li>{@link #PREDICTION} — Movement prediction phase</li>
 * <li>{@link #POST} — Post-prediction checks (speed, fly, phase, velocity)</li>
 * <li>{@link #FALLBACK} — Fallback checks when prediction is unavailable</li>
 * <li>{@link #COMBAT} — Attack-event-driven checks (reach, killaura)</li>
 * <li>{@link #PASSIVE} — Rate-limit / timing checks (autoclicker, fastplace,
 * etc.)</li>
 * </ul>
 */
public enum CheckStage {
    /** Packet state preprocessing — Timer, InventoryMove */
    PRE,

    /** Movement prediction phase */
    PREDICTION,

    /** Post-prediction movement checks — requires prediction result */
    POST,

    /** Fallback when prediction is unavailable — reduced check set */
    FALLBACK,

    /** Attack-event-driven checks — Reach, KillAura */
    COMBAT,

    /** Rate-limit / passive checks — AutoClicker, FastPlace, FastBreak, FastUse */
    PASSIVE
}
