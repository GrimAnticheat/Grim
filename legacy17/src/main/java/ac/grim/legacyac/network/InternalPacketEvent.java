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
        CLIENT_BLOCK_DIG,
        CLIENT_BLOCK_PLACE
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
    private final Integer slot;
    private final Integer actionId;
    private final Integer jumpBoost;
    private final Boolean claimsFlying;
    private final Integer digAction;
    private final Boolean sprintAction;
    private final Boolean sneakAction;
    private final Boolean hasPosition;
    private final Boolean onGround;
    private final Float pitch;
    private final Float yaw;
    private final Integer face;
    private final Float cursorX;
    private final Float cursorY;
    private final Float cursorZ;

    private InternalPacketEvent(Builder builder) {
        this.type = builder.type;
        this.player = builder.player;
        this.createdAtNanos = builder.createdAtNanos;
        this.movementPacketName = builder.movementPacketName;
        this.transactionActionId = builder.transactionActionId;
        this.entityId = builder.entityId;
        this.attackAction = builder.attackAction;
        this.keepAliveId = builder.keepAliveId;
        this.x = builder.x;
        this.y = builder.y;
        this.z = builder.z;
        this.velocityX = builder.velocityX;
        this.velocityY = builder.velocityY;
        this.velocityZ = builder.velocityZ;
        this.slot = builder.slot;
        this.actionId = builder.actionId;
        this.jumpBoost = builder.jumpBoost;
        this.claimsFlying = builder.claimsFlying;
        this.digAction = builder.digAction;
        this.sprintAction = builder.sprintAction;
        this.sneakAction = builder.sneakAction;
        this.hasPosition = builder.hasPosition;
        this.onGround = builder.onGround;
        this.pitch = builder.pitch;
        this.yaw = builder.yaw;
        this.face = builder.face;
        this.cursorX = builder.cursorX;
        this.cursorY = builder.cursorY;
        this.cursorZ = builder.cursorZ;
    }

    private static Builder builder(Type type, Player player, long nowNanos) {
        return new Builder(type, player, nowNanos);
    }

    public static InternalPacketEvent clientMovement(Player player, String packetName, long nowNanos) {
        return new InternalPacketEvent(builder(Type.CLIENT_MOVEMENT, player, nowNanos)
                .movementPacketName(packetName));
    }

    public static InternalPacketEvent clientMovementEx(Player player, String packetName, long nowNanos,
            boolean hasPosition, float yaw, float pitch) {
        return new InternalPacketEvent(builder(Type.CLIENT_MOVEMENT, player, nowNanos)
                .movementPacketName(packetName)
                .hasPosition(Boolean.valueOf(hasPosition))
                .yaw(Float.valueOf(yaw))
                .pitch(Float.valueOf(pitch)));
    }

    public static InternalPacketEvent clientMovementEx(Player player, String packetName, long nowNanos,
            double x, double y, double z, boolean onGround, boolean hasPosition, float yaw, float pitch) {
        return new InternalPacketEvent(builder(Type.CLIENT_MOVEMENT, player, nowNanos)
                .movementPacketName(packetName)
                .x(Double.valueOf(x))
                .y(Double.valueOf(y))
                .z(Double.valueOf(z))
                .onGround(Boolean.valueOf(onGround))
                .hasPosition(Boolean.valueOf(hasPosition))
                .yaw(Float.valueOf(yaw))
                .pitch(Float.valueOf(pitch)));
    }

    public static InternalPacketEvent clientTransactionAck(Player player, short actionId, long nowNanos) {
        return new InternalPacketEvent(builder(Type.CLIENT_TRANSACTION_ACK, player, nowNanos)
                .transactionActionId(Short.valueOf(actionId)));
    }

    public static InternalPacketEvent clientUseEntity(Player player, int entityId, boolean attackAction, long nowNanos) {
        return new InternalPacketEvent(builder(Type.CLIENT_USE_ENTITY, player, nowNanos)
                .entityId(Integer.valueOf(entityId))
                .attackAction(attackAction));
    }

    public static InternalPacketEvent clientKeepAlive(Player player, Long keepAliveId, long nowNanos) {
        return new InternalPacketEvent(builder(Type.CLIENT_KEEP_ALIVE, player, nowNanos)
                .keepAliveId(keepAliveId));
    }

    public static InternalPacketEvent serverPosition(Player player, double x, double y, double z, long nowNanos) {
        return serverPosition(player, x, y, z, (short) 0, nowNanos);
    }

    public static InternalPacketEvent serverPosition(Player player, double x, double y, double z, short anchorTxId,
            long nowNanos) {
        return new InternalPacketEvent(builder(Type.SERVER_POSITION, player, nowNanos)
                .x(Double.valueOf(x))
                .y(Double.valueOf(y))
                .z(Double.valueOf(z))
                .transactionActionId(Short.valueOf(anchorTxId)));
    }

    public static InternalPacketEvent serverEntityVelocity(Player player, int entityId, int velocityX, int velocityY,
            int velocityZ, long nowNanos) {
        return new InternalPacketEvent(builder(Type.SERVER_ENTITY_VELOCITY, player, nowNanos)
                .entityId(Integer.valueOf(entityId))
                .velocityX(Integer.valueOf(velocityX))
                .velocityY(Integer.valueOf(velocityY))
                .velocityZ(Integer.valueOf(velocityZ)));
    }

    public static InternalPacketEvent clientHeldItemChange(Player player, int slot, long nowNanos) {
        return new InternalPacketEvent(builder(Type.CLIENT_HELD_ITEM_CHANGE, player, nowNanos)
                .slot(Integer.valueOf(slot)));
    }

    public static InternalPacketEvent clientEntityAction(Player player, int entityId, int actionId, int jumpBoost,
            boolean isSprint, boolean isSneak, long nowNanos) {
        return new InternalPacketEvent(builder(Type.CLIENT_ENTITY_ACTION, player, nowNanos)
                .entityId(Integer.valueOf(entityId))
                .actionId(Integer.valueOf(actionId))
                .jumpBoost(Integer.valueOf(jumpBoost))
                .sprintAction(Boolean.valueOf(isSprint))
                .sneakAction(Boolean.valueOf(isSneak)));
    }

    public static InternalPacketEvent clientAbilities(Player player, boolean claimsFlying, long nowNanos) {
        return new InternalPacketEvent(builder(Type.CLIENT_ABILITIES, player, nowNanos)
                .claimsFlying(Boolean.valueOf(claimsFlying)));
    }

    public static InternalPacketEvent clientBlockDig(Player player, int x, int y, int z, int face, int digAction,
            long nowNanos) {
        return new InternalPacketEvent(builder(Type.CLIENT_BLOCK_DIG, player, nowNanos)
                .x(Double.valueOf(x))
                .y(Double.valueOf(y))
                .z(Double.valueOf(z))
                .face(Integer.valueOf(face))
                .digAction(Integer.valueOf(digAction)));
    }

    public static InternalPacketEvent clientBlockDig(Player player, int digAction, long nowNanos) {
        return new InternalPacketEvent(builder(Type.CLIENT_BLOCK_DIG, player, nowNanos)
                .digAction(Integer.valueOf(digAction)));
    }

    public static InternalPacketEvent clientBlockPlace(Player player, int x, int y, int z, int face,
            float cursorX, float cursorY, float cursorZ, long nowNanos) {
        return new InternalPacketEvent(builder(Type.CLIENT_BLOCK_PLACE, player, nowNanos)
                .x(Double.valueOf(x))
                .y(Double.valueOf(y))
                .z(Double.valueOf(z))
                .face(Integer.valueOf(face))
                .cursorX(Float.valueOf(cursorX))
                .cursorY(Float.valueOf(cursorY))
                .cursorZ(Float.valueOf(cursorZ)));
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
    public Integer getSlot() { return slot; }
    public Integer getActionId() { return actionId; }
    public Integer getJumpBoost() { return jumpBoost; }
    public Boolean getClaimsFlying() { return claimsFlying; }
    public Integer getDigAction() { return digAction; }
    public Boolean getSprintAction() { return sprintAction; }
    public Boolean getSneakAction() { return sneakAction; }
    public Boolean getHasPosition() { return hasPosition; }
    public Boolean getOnGround() { return onGround; }
    public Float getPitch() { return pitch; }
    public Float getYaw() { return yaw; }
    public Integer getFace() { return face; }
    public Float getCursorX() { return cursorX; }
    public Float getCursorY() { return cursorY; }
    public Float getCursorZ() { return cursorZ; }

    private static final class Builder {
        private final Type type;
        private final Player player;
        private final long createdAtNanos;
        private String movementPacketName;
        private Short transactionActionId;
        private Integer entityId;
        private boolean attackAction;
        private Long keepAliveId;
        private Double x;
        private Double y;
        private Double z;
        private Integer velocityX;
        private Integer velocityY;
        private Integer velocityZ;
        private Integer slot;
        private Integer actionId;
        private Integer jumpBoost;
        private Boolean claimsFlying;
        private Integer digAction;
        private Boolean sprintAction;
        private Boolean sneakAction;
        private Boolean hasPosition;
        private Boolean onGround;
        private Float pitch;
        private Float yaw;
        private Integer face;
        private Float cursorX;
        private Float cursorY;
        private Float cursorZ;

        private Builder(Type type, Player player, long createdAtNanos) {
            this.type = type;
            this.player = player;
            this.createdAtNanos = createdAtNanos;
        }

        private Builder movementPacketName(String movementPacketName) { this.movementPacketName = movementPacketName; return this; }
        private Builder transactionActionId(Short transactionActionId) { this.transactionActionId = transactionActionId; return this; }
        private Builder entityId(Integer entityId) { this.entityId = entityId; return this; }
        private Builder attackAction(boolean attackAction) { this.attackAction = attackAction; return this; }
        private Builder keepAliveId(Long keepAliveId) { this.keepAliveId = keepAliveId; return this; }
        private Builder x(Double x) { this.x = x; return this; }
        private Builder y(Double y) { this.y = y; return this; }
        private Builder z(Double z) { this.z = z; return this; }
        private Builder velocityX(Integer velocityX) { this.velocityX = velocityX; return this; }
        private Builder velocityY(Integer velocityY) { this.velocityY = velocityY; return this; }
        private Builder velocityZ(Integer velocityZ) { this.velocityZ = velocityZ; return this; }
        private Builder slot(Integer slot) { this.slot = slot; return this; }
        private Builder actionId(Integer actionId) { this.actionId = actionId; return this; }
        private Builder jumpBoost(Integer jumpBoost) { this.jumpBoost = jumpBoost; return this; }
        private Builder claimsFlying(Boolean claimsFlying) { this.claimsFlying = claimsFlying; return this; }
        private Builder digAction(Integer digAction) { this.digAction = digAction; return this; }
        private Builder sprintAction(Boolean sprintAction) { this.sprintAction = sprintAction; return this; }
        private Builder sneakAction(Boolean sneakAction) { this.sneakAction = sneakAction; return this; }
        private Builder hasPosition(Boolean hasPosition) { this.hasPosition = hasPosition; return this; }
        private Builder onGround(Boolean onGround) { this.onGround = onGround; return this; }
        private Builder pitch(Float pitch) { this.pitch = pitch; return this; }
        private Builder yaw(Float yaw) { this.yaw = yaw; return this; }
        private Builder face(Integer face) { this.face = face; return this; }
        private Builder cursorX(Float cursorX) { this.cursorX = cursorX; return this; }
        private Builder cursorY(Float cursorY) { this.cursorY = cursorY; return this; }
        private Builder cursorZ(Float cursorZ) { this.cursorZ = cursorZ; return this; }
    }
}
