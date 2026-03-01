package ac.grim.legacyac.network;

import org.bukkit.entity.Player;

public final class InternalPacketEvent {
    public enum Type {
        CLIENT_MOVEMENT,
        CLIENT_TRANSACTION_ACK,
        CLIENT_USE_ENTITY,
        CLIENT_KEEP_ALIVE,
        SERVER_POSITION,
        SERVER_ENTITY_VELOCITY
    }

    private final Type type;
    private final Player player;
    private final long createdAtNanos;
    private final String movementPacketName;
    private final Short transactionActionId;
    private final Integer entityId;
    private final boolean attackAction;
    private final Long keepAliveId;
    private final Double x;
    private final Double y;
    private final Double z;
    private final Integer velocityX;
    private final Integer velocityY;
    private final Integer velocityZ;

    private InternalPacketEvent(Type type, Player player, long createdAtNanos, String movementPacketName, Short transactionActionId,
                                Integer entityId, boolean attackAction, Long keepAliveId,
                                Double x, Double y, Double z,
                                Integer velocityX, Integer velocityY, Integer velocityZ) {
        this.type = type;
        this.player = player;
        this.createdAtNanos = createdAtNanos;
        this.movementPacketName = movementPacketName;
        this.transactionActionId = transactionActionId;
        this.entityId = entityId;
        this.attackAction = attackAction;
        this.keepAliveId = keepAliveId;
        this.x = x;
        this.y = y;
        this.z = z;
        this.velocityX = velocityX;
        this.velocityY = velocityY;
        this.velocityZ = velocityZ;
    }

    public static InternalPacketEvent clientMovement(Player player, String packetName, long nowNanos) {
        return new InternalPacketEvent(Type.CLIENT_MOVEMENT, player, nowNanos, packetName, null, null, false, null, null, null, null, null, null, null);
    }

    public static InternalPacketEvent clientTransactionAck(Player player, short actionId, long nowNanos) {
        return new InternalPacketEvent(Type.CLIENT_TRANSACTION_ACK, player, nowNanos, null, Short.valueOf(actionId), null, false, null, null, null, null, null, null, null);
    }

    public static InternalPacketEvent clientUseEntity(Player player, int entityId, boolean attackAction, long nowNanos) {
        return new InternalPacketEvent(Type.CLIENT_USE_ENTITY, player, nowNanos, null, null, Integer.valueOf(entityId), attackAction, null, null, null, null, null, null, null);
    }

    public static InternalPacketEvent clientKeepAlive(Player player, Long keepAliveId, long nowNanos) {
        return new InternalPacketEvent(Type.CLIENT_KEEP_ALIVE, player, nowNanos, null, null, null, false, keepAliveId, null, null, null, null, null, null);
    }

    public static InternalPacketEvent serverPosition(Player player, double x, double y, double z, long nowNanos) {
        return new InternalPacketEvent(Type.SERVER_POSITION, player, nowNanos, null, null, null, false, null, Double.valueOf(x), Double.valueOf(y), Double.valueOf(z), null, null, null);
    }

    public static InternalPacketEvent serverEntityVelocity(Player player, int entityId, int velocityX, int velocityY, int velocityZ, long nowNanos) {
        return new InternalPacketEvent(Type.SERVER_ENTITY_VELOCITY, player, nowNanos, null, null, Integer.valueOf(entityId), false, null,
            null, null, null, Integer.valueOf(velocityX), Integer.valueOf(velocityY), Integer.valueOf(velocityZ));
    }

    public Type getType() { return type; }
    public Player getPlayer() { return player; }
    public long getCreatedAtNanos() { return createdAtNanos; }
    public String getMovementPacketName() { return movementPacketName; }
    public Short getTransactionActionId() { return transactionActionId; }
    public Integer getEntityId() { return entityId; }
    public boolean isAttackAction() { return attackAction; }
    public Long getKeepAliveId() { return keepAliveId; }
    public Double getX() { return x; }
    public Double getY() { return y; }
    public Double getZ() { return z; }
    public Integer getVelocityX() { return velocityX; }
    public Integer getVelocityY() { return velocityY; }
    public Integer getVelocityZ() { return velocityZ; }
}
