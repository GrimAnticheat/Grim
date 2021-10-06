package ac.grim.grimac.utils.latency;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.data.ShulkerData;
import ac.grim.grimac.utils.data.packetentity.*;
import ac.grim.grimac.utils.data.packetentity.latency.EntityMetadataData;
import ac.grim.grimac.utils.data.packetentity.latency.EntityMountData;
import ac.grim.grimac.utils.data.packetentity.latency.EntityMoveData;
import ac.grim.grimac.utils.data.packetentity.latency.EntityPropertiesData;
import ac.grim.grimac.utils.enums.EntityType;
import ac.grim.grimac.utils.enums.Pose;
import ac.grim.grimac.utils.math.GrimMath;
import ac.grim.grimac.utils.nmsImplementations.BoundingBoxSize;
import ac.grim.grimac.utils.nmsImplementations.WatchableIndexUtil;
import io.github.retrooper.packetevents.packetwrappers.play.out.entitymetadata.WrappedWatchableObject;
import io.github.retrooper.packetevents.utils.attributesnapshot.AttributeModifierWrapper;
import io.github.retrooper.packetevents.utils.attributesnapshot.AttributeSnapshotWrapper;
import io.github.retrooper.packetevents.utils.player.ClientVersion;
import io.github.retrooper.packetevents.utils.server.ServerVersion;
import io.github.retrooper.packetevents.utils.vector.Vector3d;
import io.github.retrooper.packetevents.utils.vector.Vector3i;
import org.bukkit.block.BlockFace;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class CompensatedEntities {
    // I can't get FastUtils to work here
    public final ConcurrentHashMap<Integer, PacketEntity> entityMap = new ConcurrentHashMap<>(40, 0.7f);

    public ConcurrentLinkedQueue<EntityMoveData> moveEntityQueue = new ConcurrentLinkedQueue<>();
    public ConcurrentLinkedQueue<EntityMetadataData> importantMetadataQueue = new ConcurrentLinkedQueue<>();
    public ConcurrentLinkedQueue<EntityMountData> mountVehicleQueue = new ConcurrentLinkedQueue<>();
    public ConcurrentLinkedQueue<EntityPropertiesData> entityPropertiesData = new ConcurrentLinkedQueue<>();

    public double playerEntityMovementSpeed = 0.1f;
    public double playerEntityAttackSpeed = 4;

    GrimPlayer player;

    public CompensatedEntities(GrimPlayer player) {
        this.player = player;
    }

    public void tickUpdates(int lastTransactionReceived) {
        // Move entities + teleport (combined to prevent teleport + move position desync)
        while (true) {
            EntityMoveData moveEntity = moveEntityQueue.peek();
            if (moveEntity == null) break;

            if (moveEntity.lastTransactionSent > lastTransactionReceived) break;
            moveEntityQueue.poll();

            PacketEntity entity = getEntity(moveEntity.entityID);

            // This is impossible without the server sending bad packets, but just to be safe...
            if (entity == null) continue;

            entity.lastTickPosition = new Vector3d(entity.position.getX(), entity.position.getY(), entity.position.getZ());
            if (moveEntity.isRelative) {
                entity.position = entity.position.add(new Vector3d(moveEntity.x, moveEntity.y, moveEntity.z));
            } else {
                entity.position = new Vector3d(moveEntity.x, moveEntity.y, moveEntity.z);
            }

            if (entity instanceof PacketEntityRideable) {
                ((PacketEntityRideable) entity).entityPositions.add(entity.position);
            }
        }

        // Update entity metadata such as whether a horse has a saddle
        while (true) {
            EntityMetadataData metaData = importantMetadataQueue.peek();
            if (metaData == null) break;

            if (metaData.lastTransactionSent > lastTransactionReceived) break;
            importantMetadataQueue.poll();

            updateEntityMetadata(metaData.entityID, metaData.objects);
        }

        // Update entity properties such as movement speed and horse jump height
        while (true) {
            EntityPropertiesData metaData = entityPropertiesData.peek();
            if (metaData == null) break;

            if (metaData.lastTransactionSent > lastTransactionReceived) break;
            entityPropertiesData.poll();

            PacketEntity entity = getEntity(metaData.entityID);

            if (metaData.entityID == player.entityID) {
                for (AttributeSnapshotWrapper snapshotWrapper : metaData.objects) {
                    if (snapshotWrapper.getKey().toUpperCase().contains("MOVEMENT")) {
                        playerEntityMovementSpeed = calculateAttribute(snapshotWrapper, 0.0, 1024.0);
                    }

                    // TODO: This would allow us to check NoSlow on 1.9+ clients with OldCombatMechanics
                    if (snapshotWrapper.getKey().toUpperCase().contains("ATTACK_SPEED")) {

                    }
                }
            }

            if (entity instanceof PacketEntityHorse) {
                for (AttributeSnapshotWrapper snapshotWrapper : metaData.objects) {
                    if (snapshotWrapper.getKey().toUpperCase().contains("MOVEMENT")) {
                        ((PacketEntityHorse) entity).movementSpeedAttribute = (float) calculateAttribute(snapshotWrapper, 0.0, 1024.0);
                    }

                    if (snapshotWrapper.getKey().toUpperCase().contains("JUMP")) {
                        ((PacketEntityHorse) entity).jumpStrength = (float) calculateAttribute(snapshotWrapper, 0.0, 2.0);
                    }
                }
            }

            if (entity instanceof PacketEntityRideable) {
                for (AttributeSnapshotWrapper snapshotWrapper : metaData.objects) {
                    if (snapshotWrapper.getKey().toUpperCase().contains("MOVEMENT")) {
                        ((PacketEntityRideable) entity).movementSpeedAttribute = (float) calculateAttribute(snapshotWrapper, 0.0, 1024.0);
                    }
                }
            }
        }

        // Update what entities are riding what (needed to keep track of position accurately)
        while (true) {
            EntityMountData mountVehicle = mountVehicleQueue.peek();
            if (mountVehicle == null) break;

            if (mountVehicle.lastTransaction >= lastTransactionReceived) break;
            mountVehicleQueue.poll();

            PacketEntity vehicle = getEntity(mountVehicle.vehicleID);
            if (vehicle == null)
                continue;

            // Eject existing passengers for this vehicle
            if (vehicle.passengers != null) {
                for (int entityID : vehicle.passengers) {
                    PacketEntity passenger = getEntity(entityID);

                    if (passenger == null)
                        continue;

                    passenger.riding = null;
                }
            }

            // Add the entities as vehicles
            for (int entityID : mountVehicle.passengers) {
                PacketEntity passenger = getEntity(entityID);
                if (passenger == null)
                    continue;

                passenger.riding = vehicle;
            }

            vehicle.passengers = mountVehicle.passengers;
        }

        // Remove entities when the client despawns them
        // We do it in this strange way to avoid despawning the wrong entity
        for (Map.Entry<Integer, PacketEntity> entry : entityMap.entrySet()) {
            PacketEntity entity = entry.getValue();
            if (entity == null) continue;
            if (entity.removeTrans > lastTransactionReceived) continue;
            int entityID = entry.getKey();

            entityMap.remove(entityID);
            player.compensatedPotions.removeEntity(entityID);
            player.checkManager.getReach().removeEntity(entityID);
        }

        // Update riding positions - server should send teleport after dismount
        for (PacketEntity entity : entityMap.values()) {
            // The entity will be "ticked" by tickPassenger
            if (entity.riding != null)
                continue;

            for (int passengerID : entity.passengers) {
                PacketEntity passengerPassenger = getEntity(passengerID);
                tickPassenger(entity, passengerPassenger);
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
            passenger.lastTickPosition = passenger.position;

            passenger.position = riding.position.add(new Vector3d(0, BoundingBoxSize.getMyRidingOffset(riding) + BoundingBoxSize.getPassengerRidingOffset(passenger), 0));

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
            packetEntity = new PacketEntityHorse(entityType, position);
        } else if (EntityType.isSize(entityType)) {
            packetEntity = new PacketEntitySizeable(entityType, position);
        } else {
            switch (type) {
                case PIG:
                    packetEntity = new PacketEntityRideable(entityType, position);
                    break;
                case SHULKER:
                    packetEntity = new PacketEntityShulker(entityType, position);
                    break;
                case STRIDER:
                    packetEntity = new PacketEntityStrider(entityType, position);
                    break;
                case PLAYER:
                    packetEntity = new PacketEntityPlayer(entityType, position);
                    break;
                default:
                    packetEntity = new PacketEntity(entityType, position);
            }
        }

        entityMap.put(entityID, packetEntity);
    }

    public PacketEntity getEntity(int entityID) {
        return entityMap.get(entityID);
    }

    private void updateEntityMetadata(int entityID, List<WrappedWatchableObject> watchableObjects) {
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

        PacketEntity entity = getEntity(entityID);
        if (entity == null) return;

        // Poses only exist in 1.14+ with the new shifting mechanics
        if (entity instanceof PacketEntityPlayer) {
            if (ServerVersion.getVersion().isNewerThanOrEquals(ServerVersion.v_1_14)) {
                WrappedWatchableObject poseObject = WatchableIndexUtil.getIndex(watchableObjects, 6);
                if (poseObject != null) {
                    ((PacketEntityPlayer) entity).pose = Pose.valueOf(poseObject.getRawValue().toString().toUpperCase());
                }
            } else {
                WrappedWatchableObject mainByteArray = WatchableIndexUtil.getIndex(watchableObjects, 0);

                boolean gliding = false;
                boolean swimming = false;
                boolean sneaking = false;

                boolean riptide = false;
                if (mainByteArray != null && mainByteArray.getRawValue() instanceof Byte) {
                    Byte mainByte = (Byte) mainByteArray.getRawValue();
                    gliding = (mainByte & 0x80) != 0;
                    swimming = (mainByte & 0x10) != 0;
                    sneaking = (mainByte & 0x02) != 0;
                }

                WrappedWatchableObject handStates = WatchableIndexUtil.getIndex(watchableObjects, 7);
                if (handStates != null && handStates.getRawValue() instanceof Byte) {
                    riptide = (((Byte) handStates.getRawValue()) & 0x04) != 0;
                }

                Pose pose;
                // We don't check for sleeping to reduce complexity
                if (gliding) {
                    pose = Pose.FALL_FLYING;
                } else if (swimming) {
                    pose = Pose.SWIMMING;
                } else if (riptide) { // Index 7 0x04
                    pose = Pose.SPIN_ATTACK;
                } else if (player.getClientVersion().isNewerThanOrEquals(ClientVersion.v_1_9) && sneaking) { // 0x02
                    pose = Pose.NINE_CROUCHING;
                } else if (player.getClientVersion().isNewerThanOrEquals(ClientVersion.v_1_14) && sneaking) { // 0x02
                    pose = Pose.CROUCHING;
                } else {
                    pose = Pose.STANDING;
                }

                ((PacketEntityPlayer) entity).pose = pose;
            }

        }

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
                    Vector3i position = new Vector3i((int) Math.floor(entity.position.getX()), (int) Math.floor(entity.position.getY()), (int) Math.floor(entity.position.getZ()));
                    ShulkerData data = new ShulkerData(entity, player.lastTransactionSent.get(), true);
                    player.compensatedWorld.openShulkerBoxes.removeIf(shulkerData -> shulkerData.position.equals(position));
                    player.compensatedWorld.openShulkerBoxes.add(data);
                } else {
                    Vector3i position = new Vector3i((int) Math.floor(entity.position.getX()), (int) Math.floor(entity.position.getY()), (int) Math.floor(entity.position.getZ()));
                    ShulkerData data = new ShulkerData(entity, player.lastTransactionSent.get(), false);
                    player.compensatedWorld.openShulkerBoxes.removeIf(shulkerData -> shulkerData.position.equals(position));
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
