package ac.grim.grimac.utils.latency;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.data.ShulkerData;
import ac.grim.grimac.utils.data.packetentity.*;
import ac.grim.grimac.utils.math.GrimMath;
import ac.grim.grimac.utils.nmsutil.BoundingBoxSize;
import ac.grim.grimac.utils.nmsutil.WatchableIndexUtil;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.type.EntityType;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.world.BlockFace;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityProperties;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

import java.util.List;
import java.util.UUID;

public class CompensatedEntities {
    public final Int2ObjectOpenHashMap<PacketEntity> entityMap = new Int2ObjectOpenHashMap<>(40, 0.7f);

    private static final UUID SPRINTING_MODIFIER_UUID = UUID.fromString("662A6B8D-DA3E-4C1C-8813-96EA6097278D");

    public double playerEntityMovementSpeed = 0.1f;
    public double playerEntityAttackSpeed = 4;

    GrimPlayer player;

    public CompensatedEntities(GrimPlayer player) {
        this.player = player;
    }

    public void updateAttributes(int entityID, List<WrapperPlayServerEntityProperties.Property> objects) {
        if (entityID == player.entityID) {
            for (WrapperPlayServerEntityProperties.Property snapshotWrapper : objects) {
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
            for (WrapperPlayServerEntityProperties.Property snapshotWrapper : objects) {
                if (snapshotWrapper.getKey().toUpperCase().contains("MOVEMENT")) {
                    ((PacketEntityHorse) entity).movementSpeedAttribute = (float) calculateAttribute(snapshotWrapper, 0.0, 1024.0);
                }

                if (snapshotWrapper.getKey().toUpperCase().contains("JUMP")) {
                    ((PacketEntityHorse) entity).jumpStrength = (float) calculateAttribute(snapshotWrapper, 0.0, 2.0);
                }
            }
        }

        if (entity instanceof PacketEntityRideable) {
            for (WrapperPlayServerEntityProperties.Property snapshotWrapper : objects) {
                if (snapshotWrapper.getKey().toUpperCase().contains("MOVEMENT")) {
                    ((PacketEntityRideable) entity).movementSpeedAttribute = (float) calculateAttribute(snapshotWrapper, 0.0, 1024.0);
                }
            }
        }
    }

    private double calculateAttribute(WrapperPlayServerEntityProperties.Property snapshotWrapper, double minValue, double maxValue) {
        double d0 = snapshotWrapper.getValue();

        List<WrapperPlayServerEntityProperties.PropertyModifier> modifiers = snapshotWrapper.getModifiers();
        modifiers.removeIf(modifier -> modifier.getUUID().equals(SPRINTING_MODIFIER_UUID));

        for (WrapperPlayServerEntityProperties.PropertyModifier attributemodifier : modifiers) {
            if (attributemodifier.getOperation() == WrapperPlayServerEntityProperties.PropertyModifier.Operation.ADDITION)
                d0 += attributemodifier.getAmount();
        }

        double d1 = d0;

        for (WrapperPlayServerEntityProperties.PropertyModifier attributemodifier : modifiers) {
            if (attributemodifier.getOperation() == WrapperPlayServerEntityProperties.PropertyModifier.Operation.MULTIPLY_BASE)
                d1 += d0 * attributemodifier.getAmount();
        }

        for (WrapperPlayServerEntityProperties.PropertyModifier attributemodifier : modifiers) {
            if (attributemodifier.getOperation() == WrapperPlayServerEntityProperties.PropertyModifier.Operation.MULTIPLY_TOTAL)
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

    public void addEntity(int entityID, EntityType entityType, Vector3d position) {
        // Dropped items are all server sided and players can't interact with them (except create them!), save the performance
        if (entityType == EntityTypes.ITEM) return;

        PacketEntity packetEntity;

        if (EntityTypes.isTypeInstanceOf(entityType, EntityTypes.ABSTRACT_HORSE)) {
            packetEntity = new PacketEntityHorse(player, entityType, position.getX(), position.getY(), position.getZ());
        } else if (entityType == EntityTypes.SLIME || entityType == EntityTypes.MAGMA_CUBE || entityType == EntityTypes.PHANTOM) {
            packetEntity = new PacketEntitySizeable(player, entityType, position.getX(), position.getY(), position.getZ());
        } else {
            if (EntityTypes.PIG.equals(entityType)) {
                packetEntity = new PacketEntityRideable(player, entityType, position.getX(), position.getY(), position.getZ());
            } else if (EntityTypes.SHULKER.equals(entityType)) {
                packetEntity = new PacketEntityShulker(player, entityType, position.getX(), position.getY(), position.getZ());
            } else if (EntityTypes.STRIDER.equals(entityType)) {
                packetEntity = new PacketEntityStrider(player, entityType, position.getX(), position.getY(), position.getZ());
            } else {
                packetEntity = new PacketEntity(player, entityType, position.getX(), position.getY(), position.getZ());
            }
        }

        entityMap.put(entityID, packetEntity);
    }

    public PacketEntity getEntity(int entityID) {
        return entityMap.get(entityID);
    }

    public void updateEntityMetadata(int entityID, List<EntityData> watchableObjects) {
        if (entityID == player.entityID) {
            if (PacketEvents.getAPI().getServerManager().getVersion().isNewerThanOrEquals(ServerVersion.V_1_9)) {
                EntityData gravity = WatchableIndexUtil.getIndex(watchableObjects, 5);

                if (gravity != null) {
                    Object gravityObject = gravity.getValue();

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

        if (entity.isAgeable()) {
            EntityData ageableObject = WatchableIndexUtil.getIndex(watchableObjects, PacketEvents.getAPI().getServerManager().getVersion().isNewerThanOrEquals(ServerVersion.V_1_17) ? 16 : 15);
            if (ageableObject != null) {
                Object value = ageableObject.getValue();
                // Required because bukkit Ageable doesn't align with minecraft's ageable
                if (value instanceof Boolean) {
                    entity.isBaby = (boolean) value;
                }
            }
        }

        if (entity.isSize()) {
            EntityData sizeObject = WatchableIndexUtil.getIndex(watchableObjects, PacketEvents.getAPI().getServerManager().getVersion().isNewerThanOrEquals(ServerVersion.V_1_17) ? 16 : 15);
            if (sizeObject != null) {
                Object value = sizeObject.getValue();
                if (value instanceof Integer) {
                    ((PacketEntitySizeable) entity).size = (int) value;
                }
            }
        }

        if (entity instanceof PacketEntityShulker) {
            EntityData shulkerAttached = WatchableIndexUtil.getIndex(watchableObjects, PacketEvents.getAPI().getServerManager().getVersion().isNewerThanOrEquals(ServerVersion.V_1_17) ? 16 : 15);

            if (shulkerAttached != null) {
                // This NMS -> Bukkit conversion is great and works in all 11 versions.
                ((PacketEntityShulker) entity).facing = BlockFace.valueOf(shulkerAttached.getValue().toString().toUpperCase());
            }

            EntityData height = WatchableIndexUtil.getIndex(watchableObjects, PacketEvents.getAPI().getServerManager().getVersion().isNewerThanOrEquals(ServerVersion.V_1_17) ? 18 : 17);
            if (height != null) {
                if ((byte) height.getValue() == 0) {
                    ShulkerData data = new ShulkerData(entity, player.lastTransactionSent.get(), true);
                    player.compensatedWorld.openShulkerBoxes.add(data);
                } else {
                    ShulkerData data = new ShulkerData(entity, player.lastTransactionSent.get(), false);
                    player.compensatedWorld.openShulkerBoxes.add(data);
                }
            }
        }

        if (entity instanceof PacketEntityRideable) {
            if (entity.type == EntityTypes.PIG) {
                EntityData pigSaddle = WatchableIndexUtil.getIndex(watchableObjects, PacketEvents.getAPI().getServerManager().getVersion().isNewerThanOrEquals(ServerVersion.V_1_17) ? 17 : 16);
                if (pigSaddle != null) {
                    ((PacketEntityRideable) entity).hasSaddle = (boolean) pigSaddle.getValue();
                }

                EntityData pigBoost = WatchableIndexUtil.getIndex(watchableObjects, PacketEvents.getAPI().getServerManager().getVersion().isNewerThanOrEquals(ServerVersion.V_1_17) ? 18 : 17);
                if (pigBoost != null) {
                    ((PacketEntityRideable) entity).boostTimeMax = (int) pigBoost.getValue();
                    ((PacketEntityRideable) entity).currentBoostTime = 0;
                }
            } else if (entity instanceof PacketEntityStrider) {
                EntityData striderBoost = WatchableIndexUtil.getIndex(watchableObjects, PacketEvents.getAPI().getServerManager().getVersion().isNewerThanOrEquals(ServerVersion.V_1_17) ? 17 : 16);
                if (striderBoost != null) {
                    ((PacketEntityRideable) entity).boostTimeMax = (int) striderBoost.getValue();
                    ((PacketEntityRideable) entity).currentBoostTime = 0;
                }

                EntityData striderSaddle = WatchableIndexUtil.getIndex(watchableObjects, PacketEvents.getAPI().getServerManager().getVersion().isNewerThanOrEquals(ServerVersion.V_1_17) ? 19 : 18);
                if (striderSaddle != null) {
                    ((PacketEntityRideable) entity).hasSaddle = (boolean) striderSaddle.getValue();
                }
            }
        }

        if (entity instanceof PacketEntityHorse) {
            EntityData horseByte = WatchableIndexUtil.getIndex(watchableObjects, PacketEvents.getAPI().getServerManager().getVersion().isNewerThanOrEquals(ServerVersion.V_1_17) ? 17 : 16);
            if (horseByte != null) {
                byte info = (byte) horseByte.getValue();

                ((PacketEntityHorse) entity).hasSaddle = (info & 0x04) != 0;
                ((PacketEntityHorse) entity).isRearing = (info & 0x20) != 0;
            }
        }

        if (PacketEvents.getAPI().getServerManager().getVersion().isNewerThanOrEquals(ServerVersion.V_1_9)) {
            EntityData gravity = WatchableIndexUtil.getIndex(watchableObjects, 5);

            if (gravity != null) {
                Object gravityObject = gravity.getValue();

                if (gravityObject instanceof Boolean) {
                    // Vanilla uses hasNoGravity, which is a bad name IMO
                    // hasGravity > hasNoGravity
                    entity.hasGravity = !((Boolean) gravityObject);
                }
            }
        }
    }
}
