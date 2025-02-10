package ac.grim.grimac.utils.latency;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.collisions.datatypes.SimpleCollisionBox;
import ac.grim.grimac.utils.data.ShulkerData;
import ac.grim.grimac.utils.data.TrackerData;
import ac.grim.grimac.utils.data.attribute.ValuedAttribute;
import ac.grim.grimac.utils.data.packetentity.*;
import ac.grim.grimac.utils.data.packetentity.dragon.PacketEntityEnderDragon;
import ac.grim.grimac.utils.nmsutil.BoundingBoxSize;
import ac.grim.grimac.utils.nmsutil.WatchableIndexUtil;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.attribute.Attribute;
import com.github.retrooper.packetevents.protocol.attribute.Attributes;
import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.type.EntityType;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.player.UserProfile;
import com.github.retrooper.packetevents.protocol.potion.PotionType;
import com.github.retrooper.packetevents.protocol.potion.PotionTypes;
import com.github.retrooper.packetevents.protocol.world.BlockFace;
import com.github.retrooper.packetevents.protocol.world.Direction;
import com.github.retrooper.packetevents.resources.ResourceLocation;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerUpdateAttributes;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

import java.util.*;

public class CompensatedEntities {

    public static final UUID SPRINTING_MODIFIER_UUID = UUID.fromString("662A6B8D-DA3E-4C1C-8813-96EA6097278D");
    public static final UUID SNOW_MODIFIER_UUID = UUID.fromString("1eaf83ff-7207-4596-b37a-d7a07b3ec4ce");

    public final Int2ObjectOpenHashMap<PacketEntity> entityMap = new Int2ObjectOpenHashMap<>(40, 0.7f);
    public final Int2ObjectOpenHashMap<TrackerData> serverPositionsMap = new Int2ObjectOpenHashMap<>(40, 0.7f);
    public final Object2ObjectOpenHashMap<UUID, UserProfile> profiles = new Object2ObjectOpenHashMap<>();
    public Integer serverPlayerVehicle = null;
    public boolean hasSprintingAttributeEnabled = false;

    GrimPlayer player;

    public TrackerData selfTrackedEntity;
    public PacketEntitySelf self;

    public CompensatedEntities(GrimPlayer player) {
        this.player = player;
        this.self = new PacketEntitySelf(player);
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
        this.self.setPositionRaw(player, new SimpleCollisionBox(player.x, player.y, player.z, player.x, player.y, player.z));
        for (PacketEntity vehicle : entityMap.values()) {
            for (PacketEntity passenger : vehicle.passengers) {
                tickPassenger(vehicle, passenger);
            }
        }
    }

    public void removeEntity(int entityID) {
        PacketEntity entity = entityMap.remove(entityID);
        if (entity == null) return;

        if (entity instanceof PacketEntityEnderDragon dragon) {
            for (int i = 1; i < dragon.getParts().size() + 1; i++) {
                entityMap.remove(entityID + i);
            }
        }

        for (PacketEntity passenger : new ArrayList<>(entity.passengers)) {
            passenger.eject();
        }
    }

    public OptionalInt getSlowFallingAmplifier() {
        return player.getClientVersion().isOlderThanOrEquals(ClientVersion.V_1_12_2) ? OptionalInt.empty() : getPotionLevelForPlayer(PotionTypes.SLOW_FALLING);
    }

    public OptionalInt getPotionLevelForPlayer(PotionType type) {
        return getEntityInControl().getPotionEffectLevel(type);
    }

    public boolean hasPotionEffect(PotionType type) {
        return getEntityInControl().hasPotionEffect(type);
    }

    public PacketEntity getEntityInControl() {
        return self.getRiding() != null ? self.getRiding() : self;
    }

    public void updateAttributes(int entityID, List<WrapperPlayServerUpdateAttributes.Property> objects) {
        if (entityID == player.entityID) {
            // Check for sprinting attribute. Note that this value can desync: https://bugs.mojang.com/browse/MC-69459
            for (WrapperPlayServerUpdateAttributes.Property snapshotWrapper : objects) {
                final Attribute attribute = snapshotWrapper.getAttribute();
                if (attribute != Attributes.MOVEMENT_SPEED) continue;

                boolean found = false;
                List<WrapperPlayServerUpdateAttributes.PropertyModifier> modifiers = snapshotWrapper.getModifiers();
                for (WrapperPlayServerUpdateAttributes.PropertyModifier modifier : modifiers) {
                    final ResourceLocation name = modifier.getName();
                    if (name.getKey().equals(SPRINTING_MODIFIER_UUID.toString()) || name.getKey().equals("sprinting")) {
                        found = true;
                        break;
                    }
                }

                // The server can set the player's sprinting attribute
                hasSprintingAttributeEnabled = found;
                break;
            }
        }

        PacketEntity entity = player.compensatedEntities.getEntity(entityID);
        if (entity == null) return;

        for (WrapperPlayServerUpdateAttributes.Property snapshotWrapper : objects) {
            Attribute attribute = snapshotWrapper.getAttribute();
            if (attribute == null) continue; // TODO: Warn if this happens? Either modded server or bug in packetevents.

            // Rewrite horse.jumpStrength -> modern equivalent
            if (attribute == Attributes.HORSE_JUMP_STRENGTH) {
                attribute = Attributes.JUMP_STRENGTH;
            }

            final Optional<ValuedAttribute> valuedAttribute = entity.getAttribute(attribute);
            if (valuedAttribute.isEmpty()) {
                // Not an attribute we want to track
                continue;
            }

            valuedAttribute.get().with(snapshotWrapper);
        }
    }

    private void tickPassenger(PacketEntity riding, PacketEntity passenger) {
        if (riding == null || passenger == null) {
            return;
        }

        passenger.setPositionRaw(player, riding.getPossibleLocationBoxes().offset(0, BoundingBoxSize.getMyRidingOffset(riding) + BoundingBoxSize.getPassengerRidingOffset(player, passenger), 0));

        for (PacketEntity passengerPassenger : riding.passengers) {
            tickPassenger(passenger, passengerPassenger);
        }
    }

    public void addEntity(int entityID, UUID uuid, EntityType entityType, Vector3d position, float xRot, int data) {
        // Dropped items are all server sided and players can't interact with them (except create them!), save the performance
        if (entityType == EntityTypes.ITEM) return;

        PacketEntity packetEntity;

        if (EntityTypes.CAMEL.equals(entityType)) {
            packetEntity = new PacketEntityCamel(player, uuid, entityType, position.getX(), position.getY(), position.getZ(), xRot);
        } else if (EntityTypes.isTypeInstanceOf(entityType, EntityTypes.ABSTRACT_HORSE)) {
            packetEntity = new PacketEntityHorse(player, uuid, entityType, position.getX(), position.getY(), position.getZ(), xRot);
        } else if (entityType == EntityTypes.SLIME || entityType == EntityTypes.MAGMA_CUBE || entityType == EntityTypes.PHANTOM) {
            packetEntity = new PacketEntitySizeable(player, uuid, entityType, position.getX(), position.getY(), position.getZ());
        } else if (EntityTypes.PIG.equals(entityType)) {
            packetEntity = new PacketEntityRideable(player, uuid, entityType, position.getX(), position.getY(), position.getZ());
        } else if (EntityTypes.SHULKER.equals(entityType)) {
            packetEntity = new PacketEntityShulker(player, uuid, entityType, position.getX(), position.getY(), position.getZ());
        } else if (EntityTypes.STRIDER.equals(entityType)) {
            packetEntity = new PacketEntityStrider(player, uuid, entityType, position.getX(), position.getY(), position.getZ());
        } else if (EntityTypes.isTypeInstanceOf(entityType, EntityTypes.BOAT) || EntityTypes.CHICKEN.equals(entityType)) {
            packetEntity = new PacketEntityTrackXRot(player, uuid, entityType, position.getX(), position.getY(), position.getZ(), xRot);
        } else if (EntityTypes.FISHING_BOBBER.equals(entityType)) {
            packetEntity = new PacketEntityHook(player, uuid, entityType, position.getX(), position.getY(), position.getZ(), data);
        } else if (EntityTypes.ENDER_DRAGON.equals(entityType)) {
            packetEntity = new PacketEntityEnderDragon(player, uuid, entityID, position.getX(), position.getY(), position.getZ());
        } else if (EntityTypes.PAINTING.equals(entityType)) {
            packetEntity = new PacketEntityPainting(player, uuid, position.x, position.y, position.z, Direction.getByHorizontalIndex(data));
        } else {
            packetEntity = new PacketEntity(player, uuid, entityType, position.getX(), position.getY(), position.getZ());
        }

        entityMap.put(entityID, packetEntity);
    }

    public PacketEntity getEntity(int entityID) {
        if (entityID == player.entityID) {
            return self;
        }
        return entityMap.get(entityID);
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

        if (entity instanceof PacketEntitySizeable sizeable) {
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
                    sizeable.size = Math.max((int) value, 1);
                } else if (value instanceof Byte) {
                    sizeable.size = Math.max((byte) value, 1);
                }
            }
        }

        if (entity instanceof PacketEntityShulker shulker) {
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
                shulker.facing = BlockFace.valueOf(shulkerAttached.getValue().toString().toUpperCase());
            }

            EntityData height = WatchableIndexUtil.getIndex(watchableObjects, id + 2);
            if (height != null) {
                if ((byte) height.getValue() == 0) {
                    ShulkerData data = new ShulkerData(shulker, player.lastTransactionSent.get(), true);
                    player.compensatedWorld.openShulkerBoxes.remove(data);
                    player.compensatedWorld.openShulkerBoxes.add(data);
                } else {
                    ShulkerData data = new ShulkerData(shulker, player.lastTransactionSent.get(), false);
                    player.compensatedWorld.openShulkerBoxes.remove(data);
                    player.compensatedWorld.openShulkerBoxes.add(data);
                }
            }
        }

        if (entity instanceof PacketEntityRideable rideable) {
            int offset = 0;
            if (PacketEvents.getAPI().getServerManager().getVersion().isOlderThanOrEquals(ServerVersion.V_1_8_8)) {
                if (entity.getType() == EntityTypes.PIG) {
                    EntityData pigSaddle = WatchableIndexUtil.getIndex(watchableObjects, 16);
                    if (pigSaddle != null) {
                        rideable.hasSaddle = ((byte) pigSaddle.getValue()) != 0;
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

            if (entity.getType() == EntityTypes.PIG) {
                EntityData pigSaddle = WatchableIndexUtil.getIndex(watchableObjects, 17 - offset);
                if (pigSaddle != null) {
                    rideable.hasSaddle = (boolean) pigSaddle.getValue();
                }

                EntityData pigBoost = WatchableIndexUtil.getIndex(watchableObjects, 18 - offset);
                if (pigBoost != null) { // What does 1.9-1.10 do here? Is this feature even here?
                    rideable.boostTimeMax = (int) pigBoost.getValue();
                    rideable.currentBoostTime = 0;
                }
            } else if (entity instanceof PacketEntityStrider) {
                EntityData striderBoost = WatchableIndexUtil.getIndex(watchableObjects, 17 - offset);
                if (striderBoost != null) {
                    rideable.boostTimeMax = (int) striderBoost.getValue();
                    rideable.currentBoostTime = 0;
                }

                EntityData striderSaddle = WatchableIndexUtil.getIndex(watchableObjects, 19 - offset);
                if (striderSaddle != null) {
                    rideable.hasSaddle = (boolean) striderSaddle.getValue();
                }
            }
        }

        if (entity instanceof PacketEntityHorse horse) {
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

                    horse.isTame = (info & 0x02) != 0;
                    horse.hasSaddle = (info & 0x04) != 0;
                    horse.isRearing = (info & 0x20) != 0;
                }

                // track camel dashing
                if (PacketEvents.getAPI().getServerManager().getVersion().isNewerThanOrEquals(ServerVersion.V_1_20)) {
                    if (entity instanceof PacketEntityCamel camel) {
                        EntityData entityData = WatchableIndexUtil.getIndex(watchableObjects, 18);
                        if (entityData != null) {
                            camel.dashing = (boolean) entityData.getValue();
                        }
                    }
                }

            } else {
                EntityData horseByte = WatchableIndexUtil.getIndex(watchableObjects, 16);
                if (horseByte != null) {
                    int info = (int) horseByte.getValue();

                    horse.isTame = (info & 0x02) != 0;
                    // TODO: Check this
                    horse.hasSaddle = (info & 0x04) != 0;
                    horse.hasSaddle = (info & 0x08) != 0;
                    horse.isRearing = (info & 0x40) != 0;
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

        if (entity.getType() == EntityTypes.FIREWORK_ROCKET) {
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
                    player.fireworks.addNewFirework(entityID);
                }
            } else { // 1.14+
                Optional<Integer> attachedEntityID = (Optional<Integer>) fireworkWatchableObject.getValue();

                if (attachedEntityID.isPresent() && attachedEntityID.get().equals(player.entityID)) {
                    player.fireworks.addNewFirework(entityID);
                }
            }
        }

        if (entity instanceof PacketEntityHook hook) {
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
            hook.attached = attachedEntityID - 1; // the server adds 1 to the ID
        }
    }
}
