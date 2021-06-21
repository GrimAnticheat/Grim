package ac.grim.grimac.utils.latency;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.data.ShulkerData;
import ac.grim.grimac.utils.data.packetentity.*;
import ac.grim.grimac.utils.data.packetentity.latency.EntityMetadataData;
import ac.grim.grimac.utils.data.packetentity.latency.EntityMountData;
import ac.grim.grimac.utils.data.packetentity.latency.EntityMoveData;
import ac.grim.grimac.utils.data.packetentity.latency.SpawnEntityData;
import ac.grim.grimac.utils.enums.EntityType;
import ac.grim.grimac.utils.enums.Pose;
import ac.grim.grimac.utils.nmsImplementations.XMaterial;
import io.github.retrooper.packetevents.packetwrappers.play.out.entitymetadata.WrappedWatchableObject;
import io.github.retrooper.packetevents.utils.vector.Vector3d;
import io.github.retrooper.packetevents.utils.vector.Vector3i;
import it.unimi.dsi.fastutil.Pair;
import it.unimi.dsi.fastutil.ints.Int2ObjectLinkedOpenHashMap;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ConcurrentLinkedQueue;

public class CompensatedEntities {
    public final Int2ObjectLinkedOpenHashMap<PacketEntity> entityMap = new Int2ObjectLinkedOpenHashMap<>();

    public ConcurrentLinkedQueue<SpawnEntityData> spawnEntityQueue = new ConcurrentLinkedQueue<>();
    public ConcurrentLinkedQueue<Pair<Integer, int[]>> destroyEntityQueue = new ConcurrentLinkedQueue<>();
    public ConcurrentLinkedQueue<EntityMoveData> moveEntityQueue = new ConcurrentLinkedQueue<>();
    public ConcurrentLinkedQueue<EntityMetadataData> importantMetadataQueue = new ConcurrentLinkedQueue<>();
    public ConcurrentLinkedQueue<EntityMountData> mountVehicleQueue = new ConcurrentLinkedQueue<>();

    GrimPlayer player;

    public CompensatedEntities(GrimPlayer player) {
        this.player = player;
    }

    public void tickUpdates(int lastTransactionReceived) {
        // Spawn entities first, as metadata is often in the same tick
        while (true) {
            SpawnEntityData spawnEntity = spawnEntityQueue.peek();
            if (spawnEntity == null) break;

            if (spawnEntity.lastTransactionSent >= lastTransactionReceived) break;
            spawnEntityQueue.poll();

            addEntity(spawnEntity.entity, spawnEntity.position);
        }

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
                    // Handle scenario transferring from entity to entity with the following packet order:
                    // Player boards the new entity and a packet is sent for that
                    // Player is removed from the old entity
                    // Without the second check the player wouldn't be riding anything
                    if (player.entityID == entityID && player.packetStateData.vehicle == mountVehicle.vehicleID) {
                        player.packetStateData.vehicle = null;
                    }

                    PacketEntity passenger = getEntity(entityID);

                    if (passenger == null)
                        continue;

                    passenger.riding = null;
                }
            }

            // Add the entities as vehicles
            for (int entityID : mountVehicle.passengers) {
                if (player.entityID == entityID) {
                    player.packetStateData.vehicle = mountVehicle.vehicleID;
                }

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
                PacketEntity deadEntity = getEntity(entityID);
                if (deadEntity != null)
                    deadEntity.isDead = true;
                entityMap.remove(entityID);
            }
        }

        // Update riding positions - server should send teleport after dismount
        for (PacketEntity entity : entityMap.values()) {
            // The entity will be "ticked" by tickPassenger
            if (entity.riding != null)
                continue;

            for (int passengerID : entity.passengers) {
                PacketEntity passengerPassenger = player.compensatedEntities.getEntity(passengerID);
                tickPassenger(entity, passengerPassenger);
            }
        }
    }

    private void tickPassenger(PacketEntity riding, PacketEntity passenger) {
        if (riding == null || passenger == null) {
            return;
        }

        if (riding.isDead && passenger.riding == riding) {
            passenger.riding = null;
        } else {
            passenger.lastTickPosition = passenger.position;

            // TODO: Calculate offset
            passenger.position = riding.position;

            for (int entity : riding.passengers) {
                PacketEntity passengerPassenger = player.compensatedEntities.getEntity(entity);
                tickPassenger(passenger, passengerPassenger);
            }
        }
    }

    private void addEntity(Entity entity, Vector3d position) {
        PacketEntity packetEntity;
        EntityType type = EntityType.valueOf(entity.getType().toString().toUpperCase(Locale.ROOT));

        switch (type) {
            case PIG:
                packetEntity = new PacketEntityRideable(entity, position);
                break;
            case SHULKER:
                packetEntity = new PacketEntityShulker(entity, position);
                break;
            case STRIDER:
                packetEntity = new PacketEntityStrider(entity, position);
                break;
            case DONKEY:
            case HORSE:
            case LLAMA:
            case MULE:
            case SKELETON_HORSE:
            case ZOMBIE_HORSE:
            case TRADER_LLAMA:
                packetEntity = new PacketEntityHorse(entity, position);
                break;
            default:
                packetEntity = new PacketEntity(entity, position);
        }

        entityMap.put(entity.getEntityId(), packetEntity);
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

        if (entity instanceof PacketEntityShulker) {
            Optional<WrappedWatchableObject> shulkerAttached = watchableObjects.stream().filter(o -> o.getIndex() == 15).findFirst();
            // This NMS -> Bukkit conversion is great and works in all 11 versions.
            shulkerAttached.ifPresent(wrappedWatchableObject -> ((PacketEntityShulker) entity).facing = BlockFace.valueOf(wrappedWatchableObject.getRawValue().toString().toUpperCase()));

            Optional<WrappedWatchableObject> height = watchableObjects.stream().filter(o -> o.getIndex() == 17).findFirst();
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
                Optional<WrappedWatchableObject> pigSaddle = watchableObjects.stream().filter(o -> o.getIndex() == 16).findFirst();
                pigSaddle.ifPresent(wrappedWatchableObject -> ((PacketEntityRideable) entity).hasSaddle = (boolean) wrappedWatchableObject.getRawValue());

                Optional<WrappedWatchableObject> pigBoost = watchableObjects.stream().filter(o -> o.getIndex() == 17).findFirst();
                if (pigBoost.isPresent()) {
                    ((PacketEntityRideable) entity).boostTimeMax = (int) pigBoost.get().getRawValue();
                    ((PacketEntityRideable) entity).currentBoostTime = 0;
                }
            } else if (entity instanceof PacketEntityStrider) {
                Optional<WrappedWatchableObject> striderBoost = watchableObjects.stream().filter(o -> o.getIndex() == 16).findFirst();
                if (striderBoost.isPresent()) {
                    ((PacketEntityRideable) entity).boostTimeMax = (int) striderBoost.get().getRawValue();
                    ((PacketEntityRideable) entity).currentBoostTime = 0;
                }

                Optional<WrappedWatchableObject> striderShaking = watchableObjects.stream().filter(o -> o.getIndex() == 17).findFirst();
                striderShaking.ifPresent(wrappedWatchableObject -> ((PacketEntityStrider) entity).isShaking = (boolean) wrappedWatchableObject.getRawValue());

                Optional<WrappedWatchableObject> striderSaddle = watchableObjects.stream().filter(o -> o.getIndex() == 18).findFirst();
                striderSaddle.ifPresent(wrappedWatchableObject -> ((PacketEntityRideable) entity).hasSaddle = (boolean) wrappedWatchableObject.getRawValue());
            }
        }

        if (entity instanceof PacketEntityHorse) {
            Optional<WrappedWatchableObject> horseByte = watchableObjects.stream().filter(o -> o.getIndex() == 16).findFirst();
            if (horseByte.isPresent()) {
                byte info = (byte) horseByte.get().getRawValue();

                ((PacketEntityHorse) entity).hasSaddle = (info & 0x04) != 0;
                ((PacketEntityHorse) entity).isRearing = (info & 0x20) != 0;
            }
        }
    }
}
