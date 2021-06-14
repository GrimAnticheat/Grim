package ac.grim.grimac.utils.latency;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.data.packetentity.*;
import ac.grim.grimac.utils.data.packetentity.latency.EntityMetadataData;
import ac.grim.grimac.utils.data.packetentity.latency.EntityMoveData;
import ac.grim.grimac.utils.data.packetentity.latency.SpawnEntityData;
import ac.grim.grimac.utils.enums.Pose;
import io.github.retrooper.packetevents.packetwrappers.play.out.entitymetadata.WrappedWatchableObject;
import io.github.retrooper.packetevents.utils.vector.Vector3d;
import it.unimi.dsi.fastutil.Pair;
import it.unimi.dsi.fastutil.ints.Int2ObjectLinkedOpenHashMap;
import org.bukkit.Bukkit;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentLinkedQueue;

public class CompensatedEntities {
    private final Int2ObjectLinkedOpenHashMap<PacketEntity> entityMap = new Int2ObjectLinkedOpenHashMap<>();

    public ConcurrentLinkedQueue<SpawnEntityData> spawnEntityQueue = new ConcurrentLinkedQueue<>();
    public ConcurrentLinkedQueue<Pair<Integer, int[]>> destroyEntityQueue = new ConcurrentLinkedQueue<>();
    public ConcurrentLinkedQueue<EntityMoveData> moveEntityQueue = new ConcurrentLinkedQueue<>();
    public ConcurrentLinkedQueue<EntityMetadataData> importantMetadataQueue = new ConcurrentLinkedQueue<>();

    GrimPlayer player;

    public CompensatedEntities(GrimPlayer player) {
        this.player = player;
    }

    public void tickUpdates(int lastTransactionReceived) {
        while (true) {
            SpawnEntityData spawnEntity = spawnEntityQueue.peek();
            if (spawnEntity == null) break;

            if (spawnEntity.lastTransactionSent >= lastTransactionReceived) break;
            spawnEntityQueue.poll();

            addEntity(spawnEntity.entity, spawnEntity.position);
        }

        while (true) {
            EntityMoveData moveEntity = moveEntityQueue.peek();
            if (moveEntity == null) break;

            if (moveEntity.lastTransactionSent > lastTransactionReceived) break;
            moveEntityQueue.poll();

            PacketEntity entity = getEntity(moveEntity.entityID);

            // This is impossible without the server sending bad packets, but just to be safe...
            if (entity == null) continue;

            entity.position.add(new Vector3d(moveEntity.deltaX, moveEntity.deltaY, moveEntity.deltaZ));
        }

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

        while (true) {
            Pair<Integer, int[]> spawnEntity = destroyEntityQueue.peek();
            if (spawnEntity == null) break;

            if (spawnEntity.left() >= lastTransactionReceived) break;
            destroyEntityQueue.poll();

            for (int entityID : spawnEntity.right()) {
                entityMap.remove(entityID);
            }
        }
    }

    private void addEntity(Entity entity, Vector3d position) {
        PacketEntity packetEntity;

        // Uses strings instead of enum for version compatibility
        switch (entity.getType().toString().toUpperCase()) {
            case "PIG":
                packetEntity = new PacketEntityRideable(entity, position);
                break;
            case "SHULKER":
                packetEntity = new PacketEntityShulker(entity, position);
                break;
            case "STRIDER":
                packetEntity = new PacketEntityStrider(entity, position);
                break;
            case "DONKEY":
            case "HORSE":
            case "LLAMA":
            case "MULE":
            case "SKELETON_HORSE":
            case "ZOMBIE_HORSE":
            case "TRADER_LLAMA":
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
        Optional<WrappedWatchableObject> poseObject = watchableObjects.stream().filter(o -> o.getIndex() == 6).findFirst();
        if (poseObject.isPresent()) {
            Pose pose = Pose.valueOf(poseObject.get().getRawValue().toString().toUpperCase());

            Bukkit.broadcastMessage("Pose is " + pose);
            entity.pose = pose;
        }

        if (entity instanceof PacketEntityShulker) {
            Optional<WrappedWatchableObject> shulkerAttached = watchableObjects.stream().filter(o -> o.getIndex() == 15).findFirst();
            if (shulkerAttached.isPresent()) {
                // This NMS -> Bukkit conversion is great and works in all 11 versions.
                BlockFace face = BlockFace.valueOf(shulkerAttached.get().getRawValue().toString().toUpperCase());

                Bukkit.broadcastMessage("Shulker blockface is " + face);
                ((PacketEntityShulker) entity).facing = face;
            }

            Optional<WrappedWatchableObject> height = watchableObjects.stream().filter(o -> o.getIndex() == 17).findFirst();
            if (height.isPresent()) {
                Bukkit.broadcastMessage("Shulker has opened it's shell! " + height.get().getRawValue());
                ((PacketEntityShulker) entity).wantedShieldHeight = (byte) height.get().getRawValue();
                ((PacketEntityShulker) entity).lastShieldChange = System.currentTimeMillis();
            }
        }

        if (entity instanceof PacketEntityRideable) {
            if (entity.entity.getType() == EntityType.PIG) {
                Optional<WrappedWatchableObject> pigSaddle = watchableObjects.stream().filter(o -> o.getIndex() == 16).findFirst();
                if (pigSaddle.isPresent()) {
                    // Set saddle code
                    Bukkit.broadcastMessage("Pig saddled " + pigSaddle.get().getRawValue());
                    ((PacketEntityRideable) entity).hasSaddle = (boolean) pigSaddle.get().getRawValue();
                }

                Optional<WrappedWatchableObject> pigBoost = watchableObjects.stream().filter(o -> o.getIndex() == 17).findFirst();
                if (pigBoost.isPresent()) {
                    // Set pig boost code
                    Bukkit.broadcastMessage("Pig boost " + pigBoost.get().getRawValue());
                    ((PacketEntityRideable) entity).boostTimeMax = (int) pigBoost.get().getRawValue();
                    ((PacketEntityRideable) entity).currentBoostTime = 0;
                }
            } else if (entity instanceof PacketEntityStrider) {
                Optional<WrappedWatchableObject> striderBoost = watchableObjects.stream().filter(o -> o.getIndex() == 16).findFirst();
                if (striderBoost.isPresent()) {
                    // Set strider boost code
                    Bukkit.broadcastMessage("Strider boost " + striderBoost.get().getRawValue());
                    ((PacketEntityRideable) entity).boostTimeMax = (int) striderBoost.get().getRawValue();
                    ((PacketEntityRideable) entity).currentBoostTime = 0;
                }

                Optional<WrappedWatchableObject> striderShaking = watchableObjects.stream().filter(o -> o.getIndex() == 17).findFirst();
                if (striderShaking.isPresent()) {
                    // Set strider shaking code
                    Bukkit.broadcastMessage("Strider shaking " + striderShaking.get().getRawValue());
                    ((PacketEntityStrider) entity).isShaking = (boolean) striderShaking.get().getRawValue();
                }

                Optional<WrappedWatchableObject> striderSaddle = watchableObjects.stream().filter(o -> o.getIndex() == 18).findFirst();
                if (striderSaddle.isPresent()) {
                    // Set saddle code
                    Bukkit.broadcastMessage("Strider saddled " + striderSaddle.get().getRawValue());
                    ((PacketEntityRideable) entity).hasSaddle = (boolean) striderSaddle.get().getRawValue();
                }
            }
        }

        if (entity instanceof PacketEntityHorse) {
            Optional<WrappedWatchableObject> horseByte = watchableObjects.stream().filter(o -> o.getIndex() == 16).findFirst();
            if (horseByte.isPresent()) {
                byte info = (byte) horseByte.get().getRawValue();

                Bukkit.broadcastMessage("Horse " + (info & 0x04) + " " + (info & 0x20));
                ((PacketEntityHorse) entity).hasSaddle = (info & 0x04) != 0;
                ((PacketEntityHorse) entity).isRearing = (info & 0x20) != 0;
            }
        }
    }
}
