package ac.grim.grimac.utils.latency;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.data.ShulkerData;
import ac.grim.grimac.utils.data.TrackerData;
import ac.grim.grimac.utils.data.packetentity.*;
import ac.grim.grimac.utils.math.GrimMath;
import ac.grim.grimac.utils.nmsutil.BoundingBoxSize;
import ac.grim.grimac.utils.nmsutil.WatchableIndexUtil;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.type.EntityType;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.potion.PotionType;
import com.github.retrooper.packetevents.protocol.potion.PotionTypes;
import com.github.retrooper.packetevents.protocol.world.BlockFace;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerUpdateAttributes;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

import java.util.*;

public class CompensatedEntities {
    private static final UUID SPRINTING_MODIFIER_UUID = UUID.fromString("662A6B8D-DA3E-4C1C-8813-96EA6097278D");
    public static final UUID SNOW_MODIFIER_UUID = UUID.fromString("1eaf83ff-7207-4596-b37a-d7a07b3ec4ce");
    public final Int2ObjectOpenHashMap<PacketEntity> entityMap = new Int2ObjectOpenHashMap<>(40, 0.7f);
    public final Int2ObjectOpenHashMap<TrackerData> serverPositionsMap = new Int2ObjectOpenHashMap<>(40, 0.7f);
    public Integer serverPlayerVehicle = null;
    public boolean hasSprintingAttributeEnabled = false;

    GrimPlayer player;

    public TrackerData selfTrackedEntity;
    public PacketEntitySelf playerEntity;

    public CompensatedEntities(GrimPlayer player) {
        this.player = player;
        this.playerEntity = new PacketEntitySelf(player);
        this.selfTrackedEntity = new TrackerData(0, 0, 0, 0, 0, EntityTypes.PLAYER, player.lastTransactionSent.get());
    }

    public int getPacketEntityID(PacketEntity entity) {
        for (Map.Entry<Integer, PacketEntity> entry : entityMap.int2ObjectEntrySet()) {
            if (entry.getValue() == entity) {
                return entry.getKey();
            }
        }
        return Integer.MIN_VALUE;
    }

    public void tick() {
        this.playerEntity.setPositionRaw(player.boundingBox);
        for (PacketEntity vehicle : entityMap.values()) {
            for (PacketEntity passenger : vehicle.passengers) {
                tickPassenger(vehicle, passenger);
            }
        }
    }

    public void removeEntity(int entityID) {
        PacketEntity entity = entityMap.remove(entityID);
        if (entity == null) return;

        for (PacketEntity passenger : new ArrayList<>(entity.passengers)) {
            passenger.eject();
        }
    }

    public Integer getJumpAmplifier() {
        return getPotionLevelForPlayer(PotionTypes.JUMP_BOOST);
    }

    public Integer getLevitationAmplifier() {
        return getPotionLevelForPlayer(PotionTypes.LEVITATION);
    }

    public Integer getSlowFallingAmplifier() {
        return player.getClientVersion().isOlderThanOrEquals(ClientVersion.V_1_12_2) ? null : getPotionLevelForPlayer(PotionTypes.SLOW_FALLING);
    }

    public Integer getDolphinsGraceAmplifier() {
        return getPotionLevelForPlayer(PotionTypes.DOLPHINS_GRACE);
    }

    public Integer getPotionLevelForPlayer(PotionType type) {
        PacketEntity desiredEntity = playerEntity.getRiding() != null ? playerEntity.getRiding() : playerEntity;

        HashMap<PotionType, Integer> effects = desiredEntity.potionsMap;
        if (effects == null) return null;

        return effects.get(type);
    }

    public double getPlayerMovementSpeed() {
        return calculateAttribute(player.compensatedEntities.getSelf().playerSpeed, 0.0, 1024.0);
    }

    public void updateAttributes(int entityID, List<WrapperPlayServerUpdateAttributes.Property> objects) {
        if (entityID == player.entityID) {
            for (WrapperPlayServerUpdateAttributes.Property snapshotWrapper : objects) {
                if (snapshotWrapper.getKey().toUpperCase().contains("MOVEMENT")) {

                    boolean found = false;
                    List<WrapperPlayServerUpdateAttributes.PropertyModifier> modifiers = snapshotWrapper.getModifiers();
                    for (WrapperPlayServerUpdateAttributes.PropertyModifier modifier : modifiers) {
                        if (modifier.getUUID().equals(SPRINTING_MODIFIER_UUID)) {
                            found = true;
                            break;
                        }
                    }

                    // The server can set the player's sprinting attribute
                    hasSprintingAttributeEnabled = found;
                    player.compensatedEntities.getSelf().playerSpeed = snapshotWrapper;
                }
            }
        }

        PacketEntity entity = player.compensatedEntities.getEntity(entityID);

        if (entity instanceof PacketEntityHorse) {
            for (WrapperPlayServerUpdateAttributes.Property snapshotWrapper : objects) {
                if (snapshotWrapper.getKey().toUpperCase().contains("MOVEMENT")) {
                    ((PacketEntityHorse) entity).movementSpeedAttribute = (float) calculateAttribute(snapshotWrapper, 0.0, 1024.0);
                }

                if (snapshotWrapper.getKey().toUpperCase().contains("JUMP")) {
                    ((PacketEntityHorse) entity).jumpStrength = calculateAttribute(snapshotWrapper, 0.0, 2.0);
                }
            }
        }

        if (entity instanceof PacketEntityRideable) {
            for (WrapperPlayServerUpdateAttributes.Property snapshotWrapper : objects) {
                if (snapshotWrapper.getKey().toUpperCase().contains("MOVEMENT")) {
                    ((PacketEntityRideable) entity).movementSpeedAttribute = (float) calculateAttribute(snapshotWrapper, 0.0, 1024.0);
                }
            }
        }
    }

    private double calculateAttribute(WrapperPlayServerUpdateAttributes.Property snapshotWrapper, double minValue, double maxValue) {
        double d0 = snapshotWrapper.getValue();

        List<WrapperPlayServerUpdateAttributes.PropertyModifier> modifiers = snapshotWrapper.getModifiers();
        modifiers.removeIf(modifier -> modifier.getUUID().equals(SPRINTING_MODIFIER_UUID));

        for (WrapperPlayServerUpdateAttributes.PropertyModifier attributemodifier : modifiers) {
            if (attributemodifier.getOperation() == WrapperPlayServerUpdateAttributes.PropertyModifier.Operation.ADDITION)
                d0 += attributemodifier.getAmount();
        }

        double d1 = d0;

        for (WrapperPlayServerUpdateAttributes.PropertyModifier attributemodifier : modifiers) {
            if (attributemodifier.getOperation() == WrapperPlayServerUpdateAttributes.PropertyModifier.Operation.MULTIPLY_BASE)
                d1 += d0 * attributemodifier.getAmount();
        }

        for (WrapperPlayServerUpdateAttributes.PropertyModifier attributemodifier : modifiers) {
            if (attributemodifier.getOperation() == WrapperPlayServerUpdateAttributes.PropertyModifier.Operation.MULTIPLY_TOTAL)
                d1 *= 1.0D + attributemodifier.getAmount();
        }

        return GrimMath.clampFloat((float) d1, (float) minValue, (float) maxValue);
    }

    private void tickPassenger(PacketEntity riding, PacketEntity passenger) {
        if (riding == null || passenger == null) {
            return;
        }

        passenger.setPositionRaw(riding.getPossibleCollisionBoxes().offset(0, BoundingBoxSize.getMyRidingOffset(riding) + BoundingBoxSize.getPassengerRidingOffset(player, passenger), 0));

        for (PacketEntity passengerPassenger : riding.passengers) {
            tickPassenger(passenger, passengerPassenger);
        }
    }

    public void addEntity(int entityID, EntityType entityType, Vector3d position, float xRot, int data) {
        // Dropped items are all server sided and players can't interact with them (except create them!), save the performance
        if (entityType == EntityTypes.ITEM) return;

        PacketEntity packetEntity;

        if (EntityTypes.isTypeInstanceOf(entityType, EntityTypes.ABSTRACT_HORSE)) {
            packetEntity = new PacketEntityHorse(player, entityType, position.getX(), position.getY(), position.getZ(), xRot);
        } else if (entityType == EntityTypes.SLIME || entityType == EntityTypes.MAGMA_CUBE || entityType == EntityTypes.PHANTOM) {
            packetEntity = new PacketEntitySizeable(player, entityType, position.getX(), position.getY(), position.getZ());
        } else {
            if (EntityTypes.PIG.equals(entityType)) {
                packetEntity = new PacketEntityRideable(player, entityType, position.getX(), position.getY(), position.getZ());
            } else if (EntityTypes.SHULKER.equals(entityType)) {
                packetEntity = new PacketEntityShulker(player, entityType, position.getX(), position.getY(), position.getZ());
            } else if (EntityTypes.STRIDER.equals(entityType)) {
                packetEntity = new PacketEntityStrider(player, entityType, position.getX(), position.getY(), position.getZ());
            } else if (EntityTypes.isTypeInstanceOf(entityType, EntityTypes.BOAT) || EntityTypes.CHICKEN.equals(entityType)) {
                packetEntity = new PacketEntityTrackXRot(player, entityType, position.getX(), position.getY(), position.getZ(), xRot);
            } else if (EntityTypes.FISHING_BOBBER.equals(entityType)) {
                packetEntity = new PacketEntityHook(player, entityType, position.getX(), position.getY(), position.getZ(), data);
            } else {
                packetEntity = new PacketEntity(player, entityType, position.getX(), position.getY(), position.getZ());
            }
        }

        entityMap.put(entityID, packetEntity);
    }

    public PacketEntity getEntity(int entityID) {
        if (entityID == player.entityID) {
            return playerEntity;
        }
        return entityMap.get(entityID);
    }

    public PacketEntitySelf getSelf() {
        return playerEntity;
    }

    public TrackerData getTrackedEntity(int id) {
        if (id == player.entityID) {
            return selfTrackedEntity;
        }
        return serverPositionsMap.get(id);
    }

    public void updateEntityMetadata(int entityID, List<EntityData> watchableObjects) {
        PacketEntity entity = player.compensatedEntities.getEntity(entityID);
        if (entity == null) return;

        if (entity.isAgeable()) {
            int id;
            if (PacketEvents.getAPI().getServerManager().getVersion().isOlderThanOrEquals(ServerVersion.V_1_8_8)) {
                id = 12;
            } else if (PacketEvents.getAPI().getServerManager().getVersion().isOlderThanOrEquals(ServerVersion.V_1_9_4)) {
                id = 11;
            } else if (PacketEvents.getAPI().getServerManager().getVersion().isOlderThanOrEquals(ServerVersion.V_1_13_2)) {
                id = 12;
            } else if (PacketEvents.getAPI().getServerManager().getVersion().isOlderThanOrEquals(ServerVersion.V_1_14_4)) {
                id = 14;
            } else if (PacketEvents.getAPI().getServerManager().getVersion().isOlderThanOrEquals(ServerVersion.V_1_16_5)) {
                id = 15;
            } else {
                id = 16;
            }

            // 1.14 good
            EntityData ageableObject = WatchableIndexUtil.getIndex(watchableObjects, id);
            if (ageableObject != null) {
                Object value = ageableObject.getValue();
                // Required because bukkit Ageable doesn't align with minecraft's ageable
                if (value instanceof Boolean) {
                    entity.isBaby = (boolean) value;
                } else if (value instanceof Byte) {
                    entity.isBaby = ((Byte) value) < 0;
                }
            }
        }

        if (entity.isSize()) {
            int id;
            if (PacketEvents.getAPI().getServerManager().getVersion().isOlderThanOrEquals(ServerVersion.V_1_8_8)) {
                id = 16;
            } else if (PacketEvents.getAPI().getServerManager().getVersion().isOlderThanOrEquals(ServerVersion.V_1_9_4)) {
                id = 11;
            } else if (PacketEvents.getAPI().getServerManager().getVersion().isOlderThanOrEquals(ServerVersion.V_1_13_2)) {
                id = 12;
            } else if (PacketEvents.getAPI().getServerManager().getVersion().isOlderThanOrEquals(ServerVersion.V_1_14_4)) {
                id = 14;
            } else if (PacketEvents.getAPI().getServerManager().getVersion().isOlderThanOrEquals(ServerVersion.V_1_16_5)) {
                id = 15;
            } else {
                id = 16;
            }

            EntityData sizeObject = WatchableIndexUtil.getIndex(watchableObjects, id);
            if (sizeObject != null) {
                Object value = sizeObject.getValue();
                if (value instanceof Integer) {
                    ((PacketEntitySizeable) entity).size = (int) value;
                } else if (value instanceof Byte) {
                    ((PacketEntitySizeable) entity).size = (byte) value;
                }
            }
        }

        if (entity instanceof PacketEntityShulker) {
            int id;

            if (PacketEvents.getAPI().getServerManager().getVersion().isOlderThanOrEquals(ServerVersion.V_1_9_4)) {
                id = 11;
            } else if (PacketEvents.getAPI().getServerManager().getVersion().isOlderThanOrEquals(ServerVersion.V_1_13_2)) {
                id = 12;
            } else if (PacketEvents.getAPI().getServerManager().getVersion().isOlderThanOrEquals(ServerVersion.V_1_14_4)) {
                id = 14;
            } else if (PacketEvents.getAPI().getServerManager().getVersion().isOlderThanOrEquals(ServerVersion.V_1_16_5)) {
                id = 15;
            } else {
                id = 16;
            }

            EntityData shulkerAttached = WatchableIndexUtil.getIndex(watchableObjects, id);

            if (shulkerAttached != null) {
                // This NMS -> Bukkit conversion is great and works in all 11 versions.
                ((PacketEntityShulker) entity).facing = BlockFace.valueOf(shulkerAttached.getValue().toString().toUpperCase());
            }

            EntityData height = WatchableIndexUtil.getIndex(watchableObjects, id + 2);
            if (height != null) {
                if ((byte) height.getValue() == 0) {
                    ShulkerData data = new ShulkerData(entity, player.lastTransactionSent.get(), true);
                    player.compensatedWorld.openShulkerBoxes.remove(data);
                    player.compensatedWorld.openShulkerBoxes.add(data);
                } else {
                    ShulkerData data = new ShulkerData(entity, player.lastTransactionSent.get(), false);
                    player.compensatedWorld.openShulkerBoxes.remove(data);
                    player.compensatedWorld.openShulkerBoxes.add(data);
                }
            }
        }

        if (entity instanceof PacketEntityRideable) {
            int offset = 0;
            if (PacketEvents.getAPI().getServerManager().getVersion().isOlderThanOrEquals(ServerVersion.V_1_8_8)) {
                if (entity.type == EntityTypes.PIG) {
                    EntityData pigSaddle = WatchableIndexUtil.getIndex(watchableObjects, 16);
                    if (pigSaddle != null) {
                        ((PacketEntityRideable) entity).hasSaddle = ((byte) pigSaddle.getValue()) != 0;
                    }
                }
            } else if (PacketEvents.getAPI().getServerManager().getVersion().isOlderThanOrEquals(ServerVersion.V_1_9_4)) {
                offset = 5;
            } else if (PacketEvents.getAPI().getServerManager().getVersion().isOlderThanOrEquals(ServerVersion.V_1_13_2)) {
                offset = 4;
            } else if (PacketEvents.getAPI().getServerManager().getVersion().isOlderThanOrEquals(ServerVersion.V_1_14_4)) {
                offset = 2;
            } else if (PacketEvents.getAPI().getServerManager().getVersion().isOlderThanOrEquals(ServerVersion.V_1_16_5)) {
                offset = 1;
            }

            if (entity.type == EntityTypes.PIG) {
                EntityData pigSaddle = WatchableIndexUtil.getIndex(watchableObjects, 17 - offset);
                if (pigSaddle != null) {
                    ((PacketEntityRideable) entity).hasSaddle = (boolean) pigSaddle.getValue();
                }

                EntityData pigBoost = WatchableIndexUtil.getIndex(watchableObjects, 18 - offset);
                if (pigBoost != null) { // What does 1.9-1.10 do here? Is this feature even here?
                    ((PacketEntityRideable) entity).boostTimeMax = (int) pigBoost.getValue();
                    ((PacketEntityRideable) entity).currentBoostTime = 0;
                }
            } else if (entity instanceof PacketEntityStrider) {
                EntityData striderBoost = WatchableIndexUtil.getIndex(watchableObjects, 17 - offset);
                if (striderBoost != null) {
                    ((PacketEntityRideable) entity).boostTimeMax = (int) striderBoost.getValue();
                    ((PacketEntityRideable) entity).currentBoostTime = 0;
                }

                EntityData striderSaddle = WatchableIndexUtil.getIndex(watchableObjects, 19 - offset);
                if (striderSaddle != null) {
                    ((PacketEntityRideable) entity).hasSaddle = (boolean) striderSaddle.getValue();
                }
            }
        }

        if (entity instanceof PacketEntityHorse) {
            if (PacketEvents.getAPI().getServerManager().getVersion().isNewerThanOrEquals(ServerVersion.V_1_9_4)) {
                int offset = 0;

                if (PacketEvents.getAPI().getServerManager().getVersion().isOlderThanOrEquals(ServerVersion.V_1_9_4)) {
                    offset = 5;
                } else if (PacketEvents.getAPI().getServerManager().getVersion().isOlderThanOrEquals(ServerVersion.V_1_13_2)) {
                    offset = 4;
                } else if (PacketEvents.getAPI().getServerManager().getVersion().isOlderThanOrEquals(ServerVersion.V_1_14_4)) {
                    offset = 2;
                } else if (PacketEvents.getAPI().getServerManager().getVersion().isOlderThanOrEquals(ServerVersion.V_1_16_5)) {
                    offset = 1;
                }

                EntityData horseByte = WatchableIndexUtil.getIndex(watchableObjects, 17 - offset);
                if (horseByte != null) {
                    byte info = (byte) horseByte.getValue();

                    ((PacketEntityHorse) entity).isTame = (info & 0x02) != 0;
                    ((PacketEntityHorse) entity).hasSaddle = (info & 0x04) != 0;
                    ((PacketEntityHorse) entity).isRearing = (info & 0x20) != 0;
                }
            } else {
                EntityData horseByte = WatchableIndexUtil.getIndex(watchableObjects, 16);
                if (horseByte != null) {
                    int info = (int) horseByte.getValue();

                    ((PacketEntityHorse) entity).isTame = (info & 0x02) != 0;
                    ((PacketEntityHorse) entity).hasSaddle = (info & 0x04) != 0;
                    ((PacketEntityHorse) entity).hasSaddle = (info & 0x08) != 0;
                    ((PacketEntityHorse) entity).isRearing = (info & 0x40) != 0;
                }
            }
        }

        if (PacketEvents.getAPI().getServerManager().getVersion().isNewerThanOrEquals(ServerVersion.V_1_9_4)) {
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

        if (entity.type == EntityTypes.FIREWORK_ROCKET) {
            int offset = 0;
            if (PacketEvents.getAPI().getServerManager().getVersion().isOlderThanOrEquals(ServerVersion.V_1_12_2)) {
                offset = 2;
            } else if (PacketEvents.getAPI().getServerManager().getVersion().isOlderThanOrEquals(ServerVersion.V_1_16_5)) {
                offset = 1;
            }

            EntityData fireworkWatchableObject = WatchableIndexUtil.getIndex(watchableObjects, 9 - offset);
            if (fireworkWatchableObject == null) return;

            if (fireworkWatchableObject.getValue() instanceof Integer) { // Pre 1.14
                int attachedEntityID = (Integer) fireworkWatchableObject.getValue();
                if (attachedEntityID == player.entityID) {
                    player.compensatedFireworks.addNewFirework(entityID);
                }
            } else { // 1.14+
                Optional<Integer> attachedEntityID = (Optional<Integer>) fireworkWatchableObject.getValue();

                if (attachedEntityID.isPresent() && attachedEntityID.get().equals(player.entityID)) {
                    player.compensatedFireworks.addNewFirework(entityID);
                }
            }
        }

        if (entity instanceof PacketEntityHook) {
            int index;
            if (PacketEvents.getAPI().getServerManager().getVersion().isOlderThanOrEquals(ServerVersion.V_1_9_4)) {
                index = 5;
            } else if (PacketEvents.getAPI().getServerManager().getVersion().isOlderThanOrEquals(ServerVersion.V_1_14_4)) {
                index = 6;
            } else if (PacketEvents.getAPI().getServerManager().getVersion().isOlderThanOrEquals(ServerVersion.V_1_16_5)) {
                index = 7;
            } else {
                index = 8;
            }

            EntityData hookWatchableObject = WatchableIndexUtil.getIndex(watchableObjects, index);
            if (hookWatchableObject == null) return;

            Integer attachedEntityID = (Integer) hookWatchableObject.getValue();
            ((PacketEntityHook) entity).attached = attachedEntityID - 1; // the server adds 1 to the ID
        }
    }
}
