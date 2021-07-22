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
import ac.grim.grimac.utils.math.GrimMathHelper;
import ac.grim.grimac.utils.nmsImplementations.BoundingBoxSize;
import ac.grim.grimac.utils.nmsImplementations.XMaterial;
import io.github.retrooper.packetevents.packetwrappers.play.out.entitymetadata.WrappedWatchableObject;
import io.github.retrooper.packetevents.utils.attributesnapshot.AttributeModifierWrapper;
import io.github.retrooper.packetevents.utils.attributesnapshot.AttributeSnapshotWrapper;
import io.github.retrooper.packetevents.utils.server.ServerVersion;
import io.github.retrooper.packetevents.utils.vector.Vector3d;
import io.github.retrooper.packetevents.utils.vector.Vector3i;
import it.unimi.dsi.fastutil.Pair;
import it.unimi.dsi.fastutil.ints.Int2ObjectLinkedOpenHashMap;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ConcurrentLinkedQueue;

public class CompensatedEntities {
    public final Int2ObjectLinkedOpenHashMap<PacketEntity> entityMap = new Int2ObjectLinkedOpenHashMap<>();

    public ConcurrentLinkedQueue<Pair<Integer, int[]>> destroyEntityQueue = new ConcurrentLinkedQueue<>();
    public ConcurrentLinkedQueue<EntityMoveData> moveEntityQueue = new ConcurrentLinkedQueue<>();
    public ConcurrentLinkedQueue<EntityMetadataData> importantMetadataQueue = new ConcurrentLinkedQueue<>();
    public ConcurrentLinkedQueue<EntityMountData> mountVehicleQueue = new ConcurrentLinkedQueue<>();
    public ConcurrentLinkedQueue<EntityPropertiesData> entityPropertiesData = new ConcurrentLinkedQueue<>();

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
        }

        // Update entity metadata such as whether a horse has a saddle
        while (true) {
            EntityMetadataData metaData = importantMetadataQueue.peek();
            if (metaData == null) break;

            if (metaData.lastTransactionSent > lastTransactionReceived) break;
            importantMetadataQueue.poll();

            PacketEntity entity = getEntity(metaData.entityID);

            // This is impossible without the server sending bad packets, but just to be safe...
            if (entity == null) continue;

            updateEntityMetadata(entity, metaData.objects);
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
                    if (snapshotWrapper.getKey().equalsIgnoreCase("attribute.name.generic.movement_speed")) {
                        player.playerMovementSpeed = calculateAttribute(snapshotWrapper, 0.0, 1024.0);
                    }
                }
            }

            if (entity instanceof PacketEntityHorse) {
                for (AttributeSnapshotWrapper snapshotWrapper : metaData.objects) {
                    if (snapshotWrapper.getKey().equalsIgnoreCase("attribute.name.generic.movement_speed")) {
                        ((PacketEntityHorse) entity).movementSpeedAttribute = (float) calculateAttribute(snapshotWrapper, 0.0, 1024.0);
                    }

                    if (snapshotWrapper.getKey().equalsIgnoreCase("attribute.name.horse.jump_strength")) {
                        ((PacketEntityHorse) entity).jumpStrength = (float) calculateAttribute(snapshotWrapper, 0.0, 2.0);
                    }
                }
            }

            if (entity instanceof PacketEntityRideable) {
                for (AttributeSnapshotWrapper snapshotWrapper : metaData.objects) {
                    if (snapshotWrapper.getKey().equalsIgnoreCase("attribute.name.generic.movement_speed")) {
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
        while (true) {
            Pair<Integer, int[]> spawnEntity = destroyEntityQueue.peek();
            if (spawnEntity == null) break;

            if (spawnEntity.left() >= lastTransactionReceived) break;
            destroyEntityQueue.poll();

            for (int entityID : spawnEntity.right()) {
                entityMap.remove(entityID);
                player.compensatedPotions.removeEntity(entityID);
            }
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

        return GrimMathHelper.clampFloat((float) d1, (float) minValue, (float) maxValue);
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

    public void addEntity(int entityID, Entity entity, org.bukkit.entity.EntityType entityType, Vector3d position) {

        PacketEntity packetEntity;
        EntityType type = EntityType.valueOf(entityType.toString().toUpperCase(Locale.ROOT));

        if (EntityType.isHorse(type)) {
            try {
                packetEntity = new PacketEntityHorse(entity, entityType, position);
            } catch (Exception e) {
                packetEntity = new PacketEntityHorse(null, entityType, position);
            }
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
                default:
                    packetEntity = new PacketEntity(entityType, position);
            }
        }

        entityMap.put(entityID, packetEntity);
    }

    public PacketEntity getEntity(int entityID) {
        return entityMap.get(entityID);
    }

    private void updateEntityMetadata(PacketEntity entity, List<WrappedWatchableObject> watchableObjects) {
        // Poses only exist in 1.14+ with the new shifting mechanics
        if (XMaterial.supports(14)) {
            Optional<WrappedWatchableObject> poseObject = watchableObjects.stream().filter(o -> o.getIndex() == 6).findFirst();
            poseObject.ifPresent(wrappedWatchableObject -> entity.pose = Pose.valueOf(wrappedWatchableObject.getRawValue().toString().toUpperCase()));
        }

        if (EntityType.isAgeableEntity(entity.bukkitEntityType)) {
            Optional<WrappedWatchableObject> ageableObject = watchableObjects.stream().filter(o -> o.getIndex() == (ServerVersion.getVersion().isNewerThanOrEquals(ServerVersion.v_1_17) ? 16 : 15)).findFirst();
            if (ageableObject.isPresent()) {
                Object value = ageableObject.get().getRawValue();
                // Required because bukkit Ageable doesn't align with minecraft's ageable
                if (value instanceof Boolean) {
                    entity.isBaby = (boolean) value;
                }
            }
        }

        if (entity instanceof PacketEntitySizeable) {
            Optional<WrappedWatchableObject> sizeObject = watchableObjects.stream().filter(o -> o.getIndex() == (ServerVersion.getVersion().isNewerThanOrEquals(ServerVersion.v_1_17) ? 16 : 15)).findFirst();
            if (sizeObject.isPresent()) {
                Object value = sizeObject.get().getRawValue();
                if (value instanceof Integer) {
                    ((PacketEntitySizeable) entity).size = (int) value;
                }
            }
        }

        if (entity instanceof PacketEntityShulker) {
            Optional<WrappedWatchableObject> shulkerAttached = watchableObjects.stream().filter(o -> o.getIndex() == (ServerVersion.getVersion().isNewerThanOrEquals(ServerVersion.v_1_17) ? 16 : 15)).findFirst();
            // This NMS -> Bukkit conversion is great and works in all 11 versions.
            shulkerAttached.ifPresent(wrappedWatchableObject -> ((PacketEntityShulker) entity).facing = BlockFace.valueOf(wrappedWatchableObject.getRawValue().toString().toUpperCase()));

            Optional<WrappedWatchableObject> height = watchableObjects.stream().filter(o -> o.getIndex() == (ServerVersion.getVersion().isNewerThanOrEquals(ServerVersion.v_1_17) ? 18 : 17)).findFirst();
            if (height.isPresent()) {
                if ((byte) height.get().getRawValue() == 0) {
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
                Optional<WrappedWatchableObject> pigSaddle = watchableObjects.stream().filter(o -> o.getIndex() == (ServerVersion.getVersion().isNewerThanOrEquals(ServerVersion.v_1_17) ? 17 : 16)).findFirst();
                pigSaddle.ifPresent(wrappedWatchableObject -> ((PacketEntityRideable) entity).hasSaddle = (boolean) wrappedWatchableObject.getRawValue());

                Optional<WrappedWatchableObject> pigBoost = watchableObjects.stream().filter(o -> o.getIndex() == (ServerVersion.getVersion().isNewerThanOrEquals(ServerVersion.v_1_17) ? 18 : 17)).findFirst();
                if (pigBoost.isPresent()) {
                    ((PacketEntityRideable) entity).boostTimeMax = (int) pigBoost.get().getRawValue();
                    ((PacketEntityRideable) entity).currentBoostTime = 1;
                }
            } else if (entity instanceof PacketEntityStrider) {
                Optional<WrappedWatchableObject> striderBoost = watchableObjects.stream().filter(o -> o.getIndex() == (ServerVersion.getVersion().isNewerThanOrEquals(ServerVersion.v_1_17) ? 17 : 16)).findFirst();
                if (striderBoost.isPresent()) {
                    ((PacketEntityRideable) entity).boostTimeMax = (int) striderBoost.get().getRawValue();
                    ((PacketEntityRideable) entity).currentBoostTime = 1;
                }

                Optional<WrappedWatchableObject> striderSaddle = watchableObjects.stream().filter(o -> o.getIndex() == (ServerVersion.getVersion().isNewerThanOrEquals(ServerVersion.v_1_17) ? 19 : 18)).findFirst();
                striderSaddle.ifPresent(wrappedWatchableObject -> ((PacketEntityRideable) entity).hasSaddle = (boolean) wrappedWatchableObject.getRawValue());
            }
        }

        if (entity instanceof PacketEntityHorse) {
            Optional<WrappedWatchableObject> horseByte = watchableObjects.stream().filter(o -> o.getIndex() == (ServerVersion.getVersion().isNewerThanOrEquals(ServerVersion.v_1_17) ? 17 : 16)).findFirst();
            if (horseByte.isPresent()) {
                byte info = (byte) horseByte.get().getRawValue();

                ((PacketEntityHorse) entity).hasSaddle = (info & 0x04) != 0;
                ((PacketEntityHorse) entity).isRearing = (info & 0x20) != 0;
            }
        }
    }
}
