package ac.grim.legacyac.network;

import org.bukkit.entity.Player;

public final class InternalPacketEvent {
    public enum Type {
        CLIENT_MOVEMENT,
        CLIENT_TRANSACTION_ACK,
        CLIENT_USE_ENTITY,
        CLIENT_KEEP_ALIVE,
        SERVER_POSITION,
        SERVER_ENTITY_VELOCITY,
        CLIENT_HELD_ITEM_CHANGE,
        CLIENT_ENTITY_ACTION,
        CLIENT_ABILITIES,
        CLIENT_BLOCK_DIG
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
    // BadPackets fields
    private final Integer slot;
    private final Integer actionId;
    private final Integer jumpBoost;
    private final Boolean claimsFlying;
    private final Integer digAction;
    private final Boolean sprintAction;
    private final Boolean sneakAction;
    private final Boolean hasPosition;
    private final Float pitch;
    private final Float yaw;

    private InternalPacketEvent(Type type, Player player, long createdAtNanos, String movementPacketName,
            Short transactionActionId, Integer entityId, boolean attackAction, Long keepAliveId,
            Double x, Double y, Double z,
            Integer velocityX, Integer velocityY, Integer velocityZ,
            Integer slot, Integer actionId, Integer jumpBoost,
            Boolean claimsFlying, Integer digAction,
            Boolean sprintAction, Boolean sneakAction,
            Boolean hasPosition, Float pitch, Float yaw) {
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
        this.slot = slot;
        this.actionId = actionId;
        this.jumpBoost = jumpBoost;
        this.claimsFlying = claimsFlying;
        this.digAction = digAction;
        this.sprintAction = sprintAction;
        this.sneakAction = sneakAction;
        this.hasPosition = hasPosition;
        this.pitch = pitch;
        this.yaw = yaw;
    }

    // ── Existing factories ──────────────────────────────────────────────

    public static InternalPacketEvent clientMovement(Player player, String packetName, long nowNanos) {
        return new InternalPacketEvent(Type.CLIENT_MOVEMENT, player, nowNanos, packetName, null, null, false, null,
                null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);
    }

    /** Extended movement event with position/look flags and rotation values. */
    public static InternalPacketEvent clientMovementEx(Player player, String packetName, long nowNanos,
            boolean hasPosition, float yaw, float pitch) {
        return new InternalPacketEvent(Type.CLIENT_MOVEMENT, player, nowNanos, packetName, null, null, false, null,
                null, null, null, null, null, null, null, null, null, null, null, null, null,
                Boolean.valueOf(hasPosition), Float.valueOf(pitch), Float.valueOf(yaw));
    }

    public static InternalPacketEvent clientTransactionAck(Player player, short actionId, long nowNanos) {
        return new InternalPacketEvent(Type.CLIENT_TRANSACTION_ACK, player, nowNanos, null, Short.valueOf(actionId),
                null, false, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);
    }

    public static InternalPacketEvent clientUseEntity(Player player, int entityId, boolean attackAction, long nowNanos) {
        return new InternalPacketEvent(Type.CLIENT_USE_ENTITY, player, nowNanos, null, null, Integer.valueOf(entityId),
                attackAction, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);
    }

    public static InternalPacketEvent clientKeepAlive(Player player, Long keepAliveId, long nowNanos) {
        return new InternalPacketEvent(Type.CLIENT_KEEP_ALIVE, player, nowNanos, null, null, null, false, keepAliveId,
                null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);
    }

    public static InternalPacketEvent serverPosition(Player player, double x, double y, double z, long nowNanos) {
        return new InternalPacketEvent(Type.SERVER_POSITION, player, nowNanos, null, null, null, false, null,
                Double.valueOf(x), Double.valueOf(y), Double.valueOf(z), null, null, null, null, null, null, null, null, null, null, null, null, null);
    }

    public static InternalPacketEvent serverEntityVelocity(Player player, int entityId, int velocityX, int velocityY, int velocityZ, long nowNanos) {
        return new InternalPacketEvent(Type.SERVER_ENTITY_VELOCITY, player, nowNanos, null, null, Integer.valueOf(entityId), false, null,
                null, null, null, Integer.valueOf(velocityX), Integer.valueOf(velocityY), Integer.valueOf(velocityZ),
                null, null, null, null, null, null, null, null, null, null);
    }

    // ── New BadPackets factories ────────────────────────────────────────

    public static InternalPacketEvent clientHeldItemChange(Player player, int slot, long nowNanos) {
        return new InternalPacketEvent(Type.CLIENT_HELD_ITEM_CHANGE, player, nowNanos, null, null, null, false, null,
                null, null, null, null, null, null, Integer.valueOf(slot), null, null, null, null, null, null, null, null, null);
    }

    public static InternalPacketEvent clientEntityAction(Player player, int entityId, int actionId, int jumpBoost,
            boolean isSprint, boolean isSneak, long nowNanos) {
        return new InternalPacketEvent(Type.CLIENT_ENTITY_ACTION, player, nowNanos, null, null, Integer.valueOf(entityId),
                false, null, null, null, null, null, null, null, null, Integer.valueOf(actionId), Integer.valueOf(jumpBoost),
                null, null, Boolean.valueOf(isSprint), Boolean.valueOf(isSneak), null, null, null);
    }

    public static InternalPacketEvent clientAbilities(Player player, boolean claimsFlying, long nowNanos) {
        return new InternalPacketEvent(Type.CLIENT_ABILITIES, player, nowNanos, null, null, null, false, null,
                null, null, null, null, null, null, null, null, null, Boolean.valueOf(claimsFlying), null, null, null, null, null, null);
    }

    public static InternalPacketEvent clientBlockDig(Player player, int digAction, long nowNanos) {
        return new InternalPacketEvent(Type.CLIENT_BLOCK_DIG, player, nowNanos, null, null, null, false, null,
                null, null, null, null, null, null, null, null, null, null, Integer.valueOf(digAction), null, null, null, null, null);
    }

    // ── Getters ─────────────────────────────────────────────────────────

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
    public Integer getSlot() { return slot; }
    public Integer getActionId() { return actionId; }
    public Integer getJumpBoost() { return jumpBoost; }
    public Boolean getClaimsFlying() { return claimsFlying; }
    public Integer getDigAction() { return digAction; }
    public Boolean getSprintAction() { return sprintAction; }
    public Boolean getSneakAction() { return sneakAction; }
    public Boolean getHasPosition() { return hasPosition; }
    public Float getPitch() { return pitch; }
    public Float getYaw() { return yaw; }
}
