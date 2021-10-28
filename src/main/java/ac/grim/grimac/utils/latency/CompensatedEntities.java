package ac.grim.grimac.utils.latency;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.data.ShulkerData;
import ac.grim.grimac.utils.data.packetentity.*;
import ac.grim.grimac.utils.enums.EntityType;
import ac.grim.grimac.utils.math.GrimMath;
import ac.grim.grimac.utils.nmsImplementations.BoundingBoxSize;
import ac.grim.grimac.utils.nmsImplementations.WatchableIndexUtil;
import io.github.retrooper.packetevents.packetwrappers.play.out.entitymetadata.WrappedWatchableObject;
import io.github.retrooper.packetevents.utils.attributesnapshot.AttributeModifierWrapper;
import io.github.retrooper.packetevents.utils.attributesnapshot.AttributeSnapshotWrapper;
import io.github.retrooper.packetevents.utils.server.ServerVersion;
import io.github.retrooper.packetevents.utils.vector.Vector3d;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.bukkit.block.BlockFace;

import java.util.Collection;
import java.util.List;
import java.util.Locale;

public class CompensatedEntities {
    // I can't get FastUtils to work here
    public final Int2ObjectOpenHashMap<PacketEntity> entityMap = new Int2ObjectOpenHashMap<>(40, 0.7f);

    public double playerEntityMovementSpeed = 0.1f;
    public double playerEntityAttackSpeed = 4;

    GrimPlayer player;

    public CompensatedEntities(GrimPlayer player) {
        this.player = player;
    }

    public void updateAttributes(int entityID, List<AttributeSnapshotWrapper> objects) {
        if (entityID == player.entityID) {
            for (AttributeSnapshotWrapper snapshotWrapper : objects) {
                if (snapshotWrapper.getKey().toUpperCase().contains("MOVEMENT")) {
                    playerEntityMovementSpeed = calculateAttribute(snapshotWrapper, 0.0, 1024.0);
                }

                // TODO: This would allow us to check NoSlow on 1.9+ clients with OldCombatMechanics
                if (snapshotWrapper.getKey().toUpperCase().contains("ATTACK_SPEED")) {

                }
            }
        }

        PacketEntity entity = player.compensatedEntities.getEntity(entityID);

        if (entity instanceof PacketEntityHorse) {
            for (AttributeSnapshotWrapper snapshotWrapper : objects) {
                if (snapshotWrapper.getKey().toUpperCase().contains("MOVEMENT")) {
                    ((PacketEntityHorse) entity).movementSpeedAttribute = (float) calculateAttribute(snapshotWrapper, 0.0, 1024.0);
                }

                if (snapshotWrapper.getKey().toUpperCase().contains("JUMP")) {
                    ((PacketEntityHorse) entity).jumpStrength = (float) calculateAttribute(snapshotWrapper, 0.0, 2.0);
                }
            }
        }

        if (entity instanceof PacketEntityRideable) {
            for (AttributeSnapshotWrapper snapshotWrapper : objects) {
                if (snapshotWrapper.getKey().toUpperCase().contains("MOVEMENT")) {
                    ((PacketEntityRideable) entity).movementSpeedAttribute = (float) calculateAttribute(snapshotWrapper, 0.0, 1024.0);
                }
            }
        }
    }

    private double calculateAttribute(AttributeSnapshotWrapper snapshotWrapper, double minValue, double maxValue) {
        double d0 = snapshotWrapper.getValue();

        Collection<AttributeModifierWrapper> modifiers = snapshotWrapper.getModifiers();
        modifiers.removeIf(modifier -> modifier.getName().equalsIgnoreCase("Sprinting speed boost"));

        for (AttributeModifierWrapper attributemodifier : modifiers) {
            if (attributemodifier.getOperation() == AttributeModifierWrapper.Operation.ADDITION)
                d0 += attributemodifier.getAmount();
        }

        double d1 = d0;

        for (AttributeModifierWrapper attributemodifier : modifiers) {
            if (attributemodifier.getOperation() == AttributeModifierWrapper.Operation.MULTIPLY_BASE)
                d1 += d0 * attributemodifier.getAmount();
        }

        for (AttributeModifierWrapper attributemodifier : modifiers) {
            if (attributemodifier.getOperation() == AttributeModifierWrapper.Operation.MULTIPLY_TOTAL)
                d1 *= 1.0D + attributemodifier.getAmount();
        }

        return GrimMath.clampFloat((float) d1, (float) minValue, (float) maxValue);
    }

    private void tickPassenger(PacketEntity riding, PacketEntity passenger) {
        if (riding == null || passenger == null) {
            return;
        }

        if (riding.isDead && passenger.riding == riding) {
            passenger.riding = null;
        } else {
            passenger.setPositionRaw(riding.getPossibleCollisionBoxes().offset(0, BoundingBoxSize.getMyRidingOffset(riding) + BoundingBoxSize.getPassengerRidingOffset(passenger), 0));

            for (int entity : riding.passengers) {
                PacketEntity passengerPassenger = getEntity(entity);
                tickPassenger(passenger, passengerPassenger);
            }
        }
    }

    public void addEntity(int entityID, org.bukkit.entity.EntityType entityType, Vector3d position) {
        // Dropped items are all server sided and players can't interact with them (except create them!), save the performance
        if (entityType == org.bukkit.entity.EntityType.DROPPED_ITEM) return;

        PacketEntity packetEntity;
        EntityType type = EntityType.valueOf(entityType.toString().toUpperCase(Locale.ROOT));

        if (EntityType.isHorse(type)) {
            packetEntity = new PacketEntityHorse(player, type, position.getX(), position.getY(), position.getZ());
        } else if (EntityType.isSize(entityType)) {
            packetEntity = new PacketEntitySizeable(player, type, position.getX(), position.getY(), position.getZ());
        } else {
            switch (type) {
                case PIG:
                    packetEntity = new PacketEntityRideable(player, type, position.getX(), position.getY(), position.getZ());
                    break;
                case SHULKER:
                    packetEntity = new PacketEntityShulker(player, type, position.getX(), position.getY(), position.getZ());
                    break;
                case STRIDER:
                    packetEntity = new PacketEntityStrider(player, type, position.getX(), position.getY(), position.getZ());
                    break;
                default:
                    packetEntity = new PacketEntity(player, type, position.getX(), position.getY(), position.getZ());
            }
        }

        synchronized (player.compensatedEntities.entityMap) {
            entityMap.put(entityID, packetEntity);
        }
    }

    public PacketEntity getEntity(int entityID) {
        synchronized (player.compensatedEntities.entityMap) {
            return entityMap.get(entityID);
        }
    }

    public void updateEntityMetadata(int entityID, List<WrappedWatchableObject> watchableObjects) {
        if (entityID == player.entityID) {
            if (ServerVersion.getVersion().isNewerThanOrEquals(ServerVersion.v_1_9)) {
                WrappedWatchableObject gravity = WatchableIndexUtil.getIndex(watchableObjects, 5);

                if (gravity != null) {
                    Object gravityObject = gravity.getRawValue();

                    if (gravityObject instanceof Boolean) {
                        // Vanilla uses hasNoGravity, which is a bad name IMO
                        // hasGravity > hasNoGravity
                        player.playerEntityHasGravity = !((Boolean) gravityObject);
                    }
                }
            }
        }

        PacketEntity entity = player.compensatedEntities.getEntity(entityID);
        if (entity == null) return;

        if (EntityType.isAgeableEntity(entity.bukkitEntityType)) {
            WrappedWatchableObject ageableObject = WatchableIndexUtil.getIndex(watchableObjects, ServerVersion.getVersion().isNewerThanOrEquals(ServerVersion.v_1_17) ? 16 : 15);
            if (ageableObject != null) {
                Object value = ageableObject.getRawValue();
                // Required because bukkit Ageable doesn't align with minecraft's ageable
                if (value instanceof Boolean) {
                    entity.isBaby = (boolean) value;
                }
            }
        }

        if (entity instanceof PacketEntitySizeable) {
            WrappedWatchableObject sizeObject = WatchableIndexUtil.getIndex(watchableObjects, ServerVersion.getVersion().isNewerThanOrEquals(ServerVersion.v_1_17) ? 16 : 15);
            if (sizeObject != null) {
                Object value = sizeObject.getRawValue();
                if (value instanceof Integer) {
                    ((PacketEntitySizeable) entity).size = (int) value;
                }
            }
        }

        if (entity instanceof PacketEntityShulker) {
            WrappedWatchableObject shulkerAttached = WatchableIndexUtil.getIndex(watchableObjects, ServerVersion.getVersion().isNewerThanOrEquals(ServerVersion.v_1_17) ? 16 : 15);

            if (shulkerAttached != null) {
                // This NMS -> Bukkit conversion is great and works in all 11 versions.
                ((PacketEntityShulker) entity).facing = BlockFace.valueOf(shulkerAttached.getRawValue().toString().toUpperCase());
            }

            WrappedWatchableObject height = WatchableIndexUtil.getIndex(watchableObjects, ServerVersion.getVersion().isNewerThanOrEquals(ServerVersion.v_1_17) ? 18 : 17);
            if (height != null) {
                if ((byte) height.getRawValue() == 0) {
                    ShulkerData data = new ShulkerData(entity, player.lastTransactionSent.get(), true);
                    player.compensatedWorld.openShulkerBoxes.add(data);
                } else {
                    ShulkerData data = new ShulkerData(entity, player.lastTransactionSent.get(), false);
                    player.compensatedWorld.openShulkerBoxes.add(data);
                }
            }
        }

        if (entity instanceof PacketEntityRideable) {
            if (entity.type == EntityType.PIG) {
                WrappedWatchableObject pigSaddle = WatchableIndexUtil.getIndex(watchableObjects, ServerVersion.getVersion().isNewerThanOrEquals(ServerVersion.v_1_17) ? 17 : 16);
                if (pigSaddle != null) {
                    ((PacketEntityRideable) entity).hasSaddle = (boolean) pigSaddle.getRawValue();
                }

                WrappedWatchableObject pigBoost = WatchableIndexUtil.getIndex(watchableObjects, ServerVersion.getVersion().isNewerThanOrEquals(ServerVersion.v_1_17) ? 18 : 17);
                if (pigBoost != null) {
                    ((PacketEntityRideable) entity).boostTimeMax = (int) pigBoost.getRawValue();
                    ((PacketEntityRideable) entity).currentBoostTime = 0;
                }
            } else if (entity instanceof PacketEntityStrider) {
                WrappedWatchableObject striderBoost = WatchableIndexUtil.getIndex(watchableObjects, ServerVersion.getVersion().isNewerThanOrEquals(ServerVersion.v_1_17) ? 17 : 16);
                if (striderBoost != null) {
                    ((PacketEntityRideable) entity).boostTimeMax = (int) striderBoost.getRawValue();
                    ((PacketEntityRideable) entity).currentBoostTime = 0;
                }

                WrappedWatchableObject striderSaddle = WatchableIndexUtil.getIndex(watchableObjects, ServerVersion.getVersion().isNewerThanOrEquals(ServerVersion.v_1_17) ? 19 : 18);
                if (striderSaddle != null) {
                    ((PacketEntityRideable) entity).hasSaddle = (boolean) striderSaddle.getRawValue();
                }
            }
        }

        if (entity instanceof PacketEntityHorse) {
            WrappedWatchableObject horseByte = WatchableIndexUtil.getIndex(watchableObjects, ServerVersion.getVersion().isNewerThanOrEquals(ServerVersion.v_1_17) ? 17 : 16);
            if (horseByte != null) {
                byte info = (byte) horseByte.getRawValue();

                ((PacketEntityHorse) entity).hasSaddle = (info & 0x04) != 0;
                ((PacketEntityHorse) entity).isRearing = (info & 0x20) != 0;
            }
        }

        if (ServerVersion.getVersion().isNewerThanOrEquals(ServerVersion.v_1_9)) {
            WrappedWatchableObject gravity = WatchableIndexUtil.getIndex(watchableObjects, 5);

            if (gravity != null) {
                Object gravityObject = gravity.getRawValue();

                if (gravityObject instanceof Boolean) {
                    // Vanilla uses hasNoGravity, which is a bad name IMO
                    // hasGravity > hasNoGravity
                    entity.hasGravity = !((Boolean) gravityObject);
                }
            }
        }
    }
}
