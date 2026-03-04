package ac.grim.legacyac.data.state;

import ac.grim.legacyac.combat.HitboxFrame;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Domain state aggregate for combat-related data.
 * Tracks attack targets, timing, hitbox history, click windows, etc.
 */
public final class CombatState {
    private long lastAttackAt;
    private int lastAttackTargetId;

    // Click rate tracking
    private int clickWindow;
    private long clickWindowStart;

    // Hitbox backtrack history
    private final LinkedList<HitboxFrame> hitboxHistory = new LinkedList<HitboxFrame>();

    // ── Update methods ──────────────────────────────────────────────────

    public void recordAttack(int targetEntityId) {
        this.lastAttackAt = System.currentTimeMillis();
        this.lastAttackTargetId = targetEntityId;
    }

    public void setLastAttackAt(long millis) {
        this.lastAttackAt = millis;
    }

    public void setLastAttackTargetId(int id) {
        this.lastAttackTargetId = id;
    }

    public int incrementClickWindow() {
        long now = System.currentTimeMillis();
        if (clickWindowStart == 0L || now - clickWindowStart > 1000L) {
            clickWindowStart = now;
            clickWindow = 0;
        }
        clickWindow++;
        return clickWindow;
    }

    public void decayClickWindow() {
        if (clickWindowStart != 0L && System.currentTimeMillis() - clickWindowStart > 1500L) {
            clickWindow = 0;
            clickWindowStart = 0L;
        }
    }

    public void recordHitbox(double x, double y, double z, double width, double height,
            boolean teleportMarker, boolean transactionAligned, boolean enforceable) {
        double halfWidth = width * 0.5D;
        long now = System.currentTimeMillis();
        hitboxHistory.addFirst(new HitboxFrame(now, teleportMarker, transactionAligned, enforceable,
                x - halfWidth, y, z - halfWidth, x + halfWidth, y + height, z + halfWidth));
        while (!hitboxHistory.isEmpty() && now - hitboxHistory.getLast().getTimestampMillis() > 400L) {
            hitboxHistory.removeLast();
        }
    }

    public List<HitboxFrame> getHitboxHistorySnapshot(long maxAgeMillis) {
        long now = System.currentTimeMillis();
        List<HitboxFrame> copy = new ArrayList<HitboxFrame>();
        for (HitboxFrame frame : hitboxHistory) {
            if (now - frame.getTimestampMillis() <= maxAgeMillis) {
                copy.add(frame);
            }
        }
        return copy;
    }

    // ── Read interface ──────────────────────────────────────────────────

    public long getLastAttackAt() {
        return lastAttackAt;
    }

    public int getLastAttackTargetId() {
        return lastAttackTargetId;
    }

    public int getClickWindow() {
        return clickWindow;
    }

    public long getClickWindowStart() {
        return clickWindowStart;
    }

    public void setClickWindow(int clickWindow) {
        this.clickWindow = clickWindow;
    }

    public void setClickWindowStart(long clickWindowStart) {
        this.clickWindowStart = clickWindowStart;
    }
}
