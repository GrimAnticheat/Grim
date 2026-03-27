package ac.grim.legacyac.check;

import ac.grim.legacyac.LegacyAntiCheatPlugin;
import ac.grim.legacyac.check.impl.VelocityCheck;
import ac.grim.legacyac.check.impl.badpackets.BadPacketsA;
import ac.grim.legacyac.check.impl.badpackets.BadPacketsC;
import ac.grim.legacyac.check.impl.badpackets.BadPacketsD;
import ac.grim.legacyac.check.impl.badpackets.BadPacketsE;
import ac.grim.legacyac.check.impl.badpackets.BadPacketsF;
import ac.grim.legacyac.check.impl.badpackets.BadPacketsG;
import ac.grim.legacyac.check.impl.badpackets.BadPacketsI;
import ac.grim.legacyac.check.impl.badpackets.BadPacketsL;
import ac.grim.legacyac.check.impl.badpackets.BadPacketsQ;
import ac.grim.legacyac.check.impl.badpackets.CrashA;
import ac.grim.legacyac.data.PlayerData;
import ac.grim.legacyac.network.InternalPacketEvent;
import java.util.List;
import org.bukkit.entity.Player;

final class PacketIntakeCoordinator {
    private final LegacyAntiCheatPlugin plugin;
    private final List<VelocityCheck> velocityChecks;
    private final List<BadPacketsA> badPacketsAChecks;
    private final List<BadPacketsC> badPacketsCChecks;
    private final List<BadPacketsD> badPacketsDChecks;
    private final List<BadPacketsE> badPacketsEChecks;
    private final List<BadPacketsF> badPacketsFChecks;
    private final List<BadPacketsG> badPacketsGChecks;
    private final List<BadPacketsI> badPacketsIChecks;
    private final List<BadPacketsL> badPacketsLChecks;
    private final List<BadPacketsQ> badPacketsQChecks;
    private final List<CrashA> crashAChecks;

    PacketIntakeCoordinator(LegacyAntiCheatPlugin plugin,
            List<VelocityCheck> velocityChecks,
            List<BadPacketsA> badPacketsAChecks,
            List<BadPacketsC> badPacketsCChecks,
            List<BadPacketsD> badPacketsDChecks,
            List<BadPacketsE> badPacketsEChecks,
            List<BadPacketsF> badPacketsFChecks,
            List<BadPacketsG> badPacketsGChecks,
            List<BadPacketsI> badPacketsIChecks,
            List<BadPacketsL> badPacketsLChecks,
            List<BadPacketsQ> badPacketsQChecks,
            List<CrashA> crashAChecks) {
        this.plugin = plugin;
        this.velocityChecks = velocityChecks;
        this.badPacketsAChecks = badPacketsAChecks;
        this.badPacketsCChecks = badPacketsCChecks;
        this.badPacketsDChecks = badPacketsDChecks;
        this.badPacketsEChecks = badPacketsEChecks;
        this.badPacketsFChecks = badPacketsFChecks;
        this.badPacketsGChecks = badPacketsGChecks;
        this.badPacketsIChecks = badPacketsIChecks;
        this.badPacketsLChecks = badPacketsLChecks;
        this.badPacketsQChecks = badPacketsQChecks;
        this.crashAChecks = crashAChecks;
    }

    void onInternalPacketEvent(InternalPacketEvent event) {
        Player player = event.getPlayer();
        if (player == null) {
            return;
        }
        PlayerData data = plugin.getPlayerData(player);

        if (event.getType() == InternalPacketEvent.Type.CLIENT_MOVEMENT) {
            data.network().packetOrder().recordMovementPacket(event.getMovementPacketName(), event.getCreatedAtNanos());
            data.setLastRawMovementPacketAt(event.getCreatedAtNanos());
            data.incrementRawMovementPacketCounter();
            Double x = event.getX();
            Double y = event.getY();
            Double z = event.getZ();
            Float yaw = event.getYaw();
            Float pitch = event.getPitch();
            Boolean onGround = event.getOnGround();
            if (x != null && y != null && z != null && yaw != null && pitch != null && onGround != null) {
                data.recordClaimedMovement(x.doubleValue(), y.doubleValue(), z.doubleValue(),
                        yaw.floatValue(), pitch.floatValue(), onGround.booleanValue());
            }
            Boolean hasPos = event.getHasPosition();
            if (pitch != null && yaw != null) {
                for (CrashA check : crashAChecks) {
                    check.onRotation(player, data, yaw.floatValue(), pitch.floatValue());
                }
                for (BadPacketsD check : badPacketsDChecks) {
                    check.onRotation(player, data, pitch.floatValue());
                }
            }
            if (hasPos != null) {
                for (BadPacketsE check : badPacketsEChecks) {
                    check.onFlyingPacket(player, data, hasPos.booleanValue());
                }
            }
            return;
        }

        if (event.getType() == InternalPacketEvent.Type.CLIENT_TRANSACTION_ACK) {
            Short actionId = event.getTransactionActionId();
            if (actionId != null) {
                data.acknowledgeTransaction(actionId.shortValue(), event.getCreatedAtNanos());
            }
            return;
        }

        if (event.getType() == InternalPacketEvent.Type.CLIENT_KEEP_ALIVE) {
            data.acknowledgeKeepAlive(System.currentTimeMillis());
            return;
        }

        if (event.getType() == InternalPacketEvent.Type.SERVER_POSITION) {
            data.network().packetOrder().recordServerSync("server-position", event.getCreatedAtNanos());
            data.setLastServerPositionSyncAt(event.getCreatedAtNanos());
            Double x = event.getX();
            Double y = event.getY();
            Double z = event.getZ();
            if (x != null && y != null && z != null) {
                Short anchorTxId = event.getTransactionActionId();
                data.beginTeleportSync(x.doubleValue(), y.doubleValue(), z.doubleValue(),
                        anchorTxId == null ? (short) 0 : anchorTxId.shortValue());
            }
            return;
        }

        if (event.getType() == InternalPacketEvent.Type.CLIENT_USE_ENTITY) {
            Integer entityId = event.getEntityId();
            if (entityId != null) {
                data.network().packetOrder().recordActionPacket(
                        event.isAttackAction() ? "use-entity-attack" : "use-entity-interact",
                        event.getCreatedAtNanos());
                for (BadPacketsC check : badPacketsCChecks) {
                    check.onUseEntity(player, data, entityId.intValue());
                }
            }
            if (event.isAttackAction() && entityId != null) {
                data.queueAttackSnapshot(entityId.intValue(), event.getCreatedAtNanos());
            }
            return;
        }

        if (event.getType() == InternalPacketEvent.Type.SERVER_ENTITY_VELOCITY) {
            data.network().packetOrder().recordServerSync("entity-velocity", event.getCreatedAtNanos());
            Integer entityId = event.getEntityId();
            Integer vx = event.getVelocityX();
            Integer vy = event.getVelocityY();
            Integer vz = event.getVelocityZ();
            if (entityId == null || vx == null || vy == null || vz == null) {
                return;
            }
            for (VelocityCheck check : velocityChecks) {
                check.onVelocityPacket(player, data, entityId.intValue(), vx.intValue(), vy.intValue(), vz.intValue(),
                        event.getCreatedAtNanos());
            }
            return;
        }

        if (event.getType() == InternalPacketEvent.Type.CLIENT_HELD_ITEM_CHANGE) {
            data.network().packetOrder().recordActionPacket("held-item-change", event.getCreatedAtNanos());
            Integer slot = event.getSlot();
            if (slot != null) {
                data.clearUsingItemPacket();
                for (BadPacketsA check : badPacketsAChecks) {
                    check.onHeldItemChange(player, data, slot.intValue());
                }
            }
            return;
        }

        if (event.getType() == InternalPacketEvent.Type.CLIENT_ENTITY_ACTION) {
            data.network().packetOrder().recordActionPacket("entity-action", event.getCreatedAtNanos());
            Integer entityId = event.getEntityId();
            Integer actionId = event.getActionId();
            Integer jumpBoost = event.getJumpBoost();
            Boolean isSprint = event.getSprintAction();
            Boolean isSneak = event.getSneakAction();
            if (entityId != null && actionId != null && jumpBoost != null) {
                for (BadPacketsQ check : badPacketsQChecks) {
                    check.onEntityAction(player, data, entityId.intValue(), actionId.intValue(), jumpBoost.intValue());
                }
            }
            if (isSprint != null && isSprint.booleanValue()) {
                boolean startSprint = actionId != null && actionId.intValue() == 4;
                for (BadPacketsF check : badPacketsFChecks) {
                    check.onSprintAction(player, data, startSprint);
                }
            }
            if (isSneak != null && isSneak.booleanValue()) {
                boolean startSneak = actionId != null && actionId.intValue() == 1;
                for (BadPacketsG check : badPacketsGChecks) {
                    check.onSneakAction(player, data, startSneak);
                }
            }
            return;
        }

        if (event.getType() == InternalPacketEvent.Type.CLIENT_ABILITIES) {
            data.network().packetOrder().recordActionPacket("abilities", event.getCreatedAtNanos());
            Boolean claimsFlying = event.getClaimsFlying();
            if (claimsFlying != null) {
                for (BadPacketsI check : badPacketsIChecks) {
                    check.onAbilitiesPacket(player, data, claimsFlying.booleanValue());
                }
            }
            return;
        }

        if (event.getType() == InternalPacketEvent.Type.CLIENT_BLOCK_DIG) {
            data.network().packetOrder().recordActionPacket("block-dig", event.getCreatedAtNanos());
            Integer digAction = event.getDigAction();
            if (digAction != null) {
                if (digAction.intValue() == 5) {
                    data.clearUsingItemPacket();
                }
                for (BadPacketsL check : badPacketsLChecks) {
                    check.onDigAction(player, data, digAction.intValue());
                }
                Double x = event.getX();
                Double y = event.getY();
                Double z = event.getZ();
                Integer face = event.getFace();
                if (x != null && y != null && z != null && face != null) {
                    data.queuePacketBlockDig(x.intValue(), y.intValue(), z.intValue(), face.intValue(),
                            digAction.intValue(), event.getCreatedAtNanos());
                }
            }
            return;
        }

        if (event.getType() == InternalPacketEvent.Type.CLIENT_BLOCK_PLACE) {
            data.network().packetOrder().recordActionPacket("block-place", event.getCreatedAtNanos());
            Double x = event.getX();
            Double y = event.getY();
            Double z = event.getZ();
            Integer face = event.getFace();
            Float cursorX = event.getCursorX();
            Float cursorY = event.getCursorY();
            Float cursorZ = event.getCursorZ();
            if (x != null && y != null && z != null && face != null
                    && cursorX != null && cursorY != null && cursorZ != null) {
                data.queuePacketBlockPlace(x.intValue(), y.intValue(), z.intValue(), face.intValue(),
                        cursorX.floatValue(), cursorY.floatValue(), cursorZ.floatValue(),
                        event.getCreatedAtNanos());
            }
        }
    }
}
