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
import com.github.retrooper.packetevents.protocol.world.BlockFace;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.util.Vector3i;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityProperties;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class CompensatedEntities {
    private static final UUID SPRINTING_MODIFIER_UUID = UUID.fromString("662A6B8D-DA3E-4C1C-8813-96EA6097278D");
    public static final UUID SNOW_MODIFIER_UUID = UUID.fromString("1eaf83ff-7207-4596-b37a-d7a07b3ec4ce");
    public final Int2ObjectOpenHashMap<PacketEntity> entityMap = new Int2ObjectOpenHashMap<>(40, 0.7f);
    public final Int2ObjectOpenHashMap<TrackerData> serverPositionsMap = new Int2ObjectOpenHashMap<>(40, 0.7f);
    public Integer serverPlayerVehicle = null;
    public WrapperPlayServerEntityProperties.Property playerSpeed = new WrapperPlayServerEntityProperties.Property("MOVEMENT_SPEED", 0.1f, new ArrayList<>());
    public boolean hasSprintingAttributeEnabled = false;

    GrimPlayer player;

    public CompensatedEntities(GrimPlayer player) {
        this.player = player;
    }

    public void tick() {
        for (PacketEntity vehicle : entityMap.values()) {
            for (int passengerID : vehicle.passengers) {
                PacketEntity passenger = player.compensatedEntities.getEntity(passengerID);
                tickPassenger(vehicle, passenger);
            }
        }
    }

    public double getPlayerMovementSpeed() {
        return calculateAttribute(playerSpeed, 0.0, 1024.0);
    }

    public void updateAttributes(int entityID, List<WrapperPlayServerEntityProperties.Property> objects) {
        if (entityID == player.entityID) {
            for (WrapperPlayServerEntityProperties.Property snapshotWrapper : objects) {
                if (snapshotWrapper.getKey().toUpperCase().contains("MOVEMENT")) {

                    boolean found = false;
                    List<WrapperPlayServerEntityProperties.PropertyModifier> modifiers = snapshotWrapper.getModifiers();
                    for (WrapperPlayServerEntityProperties.PropertyModifier modifier : modifiers) {
                        if (modifier.getUUID().equals(SPRINTING_MODIFIER_UUID)) {
                            found = true;
                            break;
                        }
                    }

                    // The server can set the player's sprinting attribute
                    hasSprintingAttributeEnabled = found;
                    playerSpeed = snapshotWrapper;
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
                    ((PacketEntityHorse) entity).jumpStrength = calculateAttribute(snapshotWrapper, 0.0, 2.0);
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

            if (PacketEvents.getAPI().getServerManager().getVersion().isNewerThanOrEquals(ServerVersion.V_1_17)) {
                EntityData frozen = WatchableIndexUtil.getIndex(watchableObjects, 7);

                if (frozen != null) {
                    player.powderSnowFrozenTicks = (int) frozen.getValue();
                }
            }

            if (PacketEvents.getAPI().getServerManager().getVersion().isNewerThanOrEquals(ServerVersion.V_1_12)) {
                int id = 14;
                if (PacketEvents.getAPI().getServerManager().getVersion().isOlderThanOrEquals(ServerVersion.V_1_16_5)) {
                    id = 13;
                } else if (PacketEvents.getAPI().getServerManager().getVersion().isOlderThanOrEquals(ServerVersion.V_1_14_4)) {
                    id = 12;
                }

                EntityData bedObject = WatchableIndexUtil.getIndex(watchableObjects, id);
                if (bedObject != null) {
                    Optional<Vector3i> bed = (Optional<Vector3i>) bedObject.getValue();
                    if (bed.isPresent()) {
                        player.isInBed = true;
                        Vector3i bedPos = bed.get();
                        player.bedPosition = new Vector3d(bedPos.getX() + 0.5, bedPos.getY(), bedPos.getZ() + 0.5);
                    } else { // Run when we know the player is not in bed 100%
                        player.isInBed = false;
                    }
                }
            }
        }

        PacketEntity entity = player.compensatedEntities.getEntity(entityID);
        if (entity == null) return;

        if (entity.isAgeable()) {
            int id = 16;
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
            int id = 16;
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
            int id = 16;

            if (PacketEvents.getAPI().getServerManager().getVersion().isOlderThanOrEquals(ServerVersion.V_1_9_4)) {
                id = 11;
            } else if (PacketEvents.getAPI().getServerManager().getVersion().isOlderThanOrEquals(ServerVersion.V_1_13_2)) {
                id = 12;
            } else if (PacketEvents.getAPI().getServerManager().getVersion().isOlderThanOrEquals(ServerVersion.V_1_14_4)) {
                id = 14;
            } else if (PacketEvents.getAPI().getServerManager().getVersion().isOlderThanOrEquals(ServerVersion.V_1_16_5)) {
                id = 15;
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
                    player.compensatedWorld.openShulkerBoxes.add(data);
                } else {
                    ShulkerData data = new ShulkerData(entity, player.lastTransactionSent.get(), false);
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
                EntityData chestByte = WatchableIndexUtil.getIndex(watchableObjects, 19 - offset);
                if (chestByte != null && chestByte.getValue() instanceof Boolean) {
                    ((PacketEntityHorse) entity).hasChest = (boolean) chestByte.getValue();
                }
                EntityData strength = WatchableIndexUtil.getIndex(watchableObjects, 20 - offset);
                if (strength != null && strength.getValue() instanceof Integer) {
                    ((PacketEntityHorse) entity).llamaStrength = (int) strength.getValue();
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

            Optional<Integer> attachedEntityID = (Optional<Integer>) fireworkWatchableObject.getValue();

            if (attachedEntityID.isPresent() && attachedEntityID.get().equals(player.entityID)) {
                player.compensatedFireworks.addNewFirework(entityID);
            }
        }
    }
}
