package ac.grim.legacyac.network;

import ac.grim.legacyac.LegacyAntiCheatPlugin;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

/**
 * Central transaction anchor owner for packet-backed sync points.
 *
 * <p>
 * 1.7.10 cannot reliably reproduce Grim's exact modern packet sandwich on every
 * path, so the default here is a conservative single authoritative anchor.
 * This still gives one owner for teleport/world/velocity windows and prevents
 * each subsystem from emitting its own transaction packets.
 * </p>
 */
public final class TxAnchorService {
    private final LegacyAntiCheatPlugin plugin;

    public TxAnchorService(LegacyAntiCheatPlugin plugin) {
        this.plugin = plugin;
    }

    public TeleportAnchor anchorTeleport(Player player) {
        return new TeleportAnchor(sendAnchorTransaction(player), AnchorMode.CONSERVATIVE_POST_PACKET);
    }

    public WorldAnchor anchorWorldUpdate(Player player, WorldUpdateKind kind) {
        return new WorldAnchor(sendAnchorTransaction(player), AnchorMode.CONSERVATIVE_POST_PACKET, kind);
    }

    public VelocityWindow beginVelocityWindow(Player player, Vector velocity, long sentAtNanos, long txWindowMaxMs) {
        short anchorTransactionId = sendAnchorTransaction(player);
        return new VelocityWindow((short) 0, anchorTransactionId, VelocityWindowMode.SINGLE_AUTHORITATIVE_POST_TX,
                sentAtNanos, txWindowMaxMs,
                velocity == null ? 0.0D : velocity.getX(),
                velocity == null ? 0.0D : velocity.getY(),
                velocity == null ? 0.0D : velocity.getZ());
    }

    private short sendAnchorTransaction(Player player) {
        TransactionSyncManager manager = plugin.transactionSync();
        if (manager == null || player == null || !player.isOnline()) {
            return 0;
        }
        return manager.sendTransactionNow(player);
    }

    public enum AnchorMode {
        CONSERVATIVE_POST_PACKET
    }

    public enum VelocityWindowMode {
        SINGLE_AUTHORITATIVE_POST_TX,
        EXACT_PRE_POST
    }

    public enum WorldUpdateKind {
        BLOCK_CHANGE,
        MULTI_BLOCK_CHANGE,
        MAP_CHUNK,
        MAP_CHUNK_BULK
    }

    public static final class TeleportAnchor {
        private final short transactionId;
        private final AnchorMode mode;

        public TeleportAnchor(short transactionId, AnchorMode mode) {
            this.transactionId = transactionId;
            this.mode = mode;
        }

        public short getTransactionId() {
            return transactionId;
        }

        public AnchorMode getMode() {
            return mode;
        }
    }

    public static final class WorldAnchor {
        private final short transactionId;
        private final AnchorMode mode;
        private final WorldUpdateKind kind;

        public WorldAnchor(short transactionId, AnchorMode mode, WorldUpdateKind kind) {
            this.transactionId = transactionId;
            this.mode = mode;
            this.kind = kind;
        }

        public short getTransactionId() {
            return transactionId;
        }

        public AnchorMode getMode() {
            return mode;
        }

        public WorldUpdateKind getKind() {
            return kind;
        }
    }

    public static final class VelocityWindow {
        private final short preTransactionId;
        private final short postTransactionId;
        private final VelocityWindowMode mode;
        private final long sentAtNanos;
        private final long txWindowMaxMs;
        private final double vx;
        private final double vy;
        private final double vz;

        public VelocityWindow(short preTransactionId, short postTransactionId, VelocityWindowMode mode,
                long sentAtNanos, long txWindowMaxMs, double vx, double vy, double vz) {
            this.preTransactionId = preTransactionId;
            this.postTransactionId = postTransactionId;
            this.mode = mode;
            this.sentAtNanos = sentAtNanos;
            this.txWindowMaxMs = txWindowMaxMs;
            this.vx = vx;
            this.vy = vy;
            this.vz = vz;
        }

        public short getPreTransactionId() {
            return preTransactionId;
        }

        public short getPostTransactionId() {
            return postTransactionId;
        }

        public VelocityWindowMode getMode() {
            return mode;
        }

        public long getSentAtNanos() {
            return sentAtNanos;
        }

        public long getTxWindowMaxMs() {
            return txWindowMaxMs;
        }

        public double getVx() {
            return vx;
        }

        public double getVy() {
            return vy;
        }

        public double getVz() {
            return vz;
        }
    }
}
