// This file was designed and is an original check for GrimAC
// Copyright (C) 2021 DefineOutside
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program.  If not, see <http://www.gnu.org/licenses/>.
package ac.grim.grimac.utils.data.packetentity;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.collisions.datatypes.SimpleCollisionBox;
import ac.grim.grimac.utils.data.ReachInterpolationData;
import ac.grim.grimac.utils.data.TrackedPosition;
import ac.grim.grimac.utils.data.attribute.ValuedAttribute;
import com.github.retrooper.packetevents.protocol.attribute.Attribute;
import com.github.retrooper.packetevents.protocol.attribute.Attributes;
import com.github.retrooper.packetevents.protocol.entity.type.EntityType;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.player.EquipmentSlot;
import com.github.retrooper.packetevents.protocol.potion.PotionType;
import com.github.retrooper.packetevents.util.Vector3d;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import lombok.Getter;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;

// You may not copy this check unless your anticheat is licensed under GPL
public class PacketEntity extends TypedPacketEntity {

    public final TrackedPosition trackedServerPosition;
    protected final Map<Attribute, ValuedAttribute> attributeMap = new IdentityHashMap<>();
    // TODO in what cases is UUID null in 1.9+?
    @Getter
    private final UUID uuid; // NULL ON VERSIONS BELOW 1.9 (or for some entities, apparently??)
    @Getter
    public PacketEntity riding;
    public final List<PacketEntity> passengers = new ArrayList<>(0);
    public boolean isDead = false;
    public boolean isBaby = false;
    public boolean hasGravity = true;
    private ReachInterpolationData oldPacketLocation;
    private ReachInterpolationData newPacketLocation;
    private Object2IntMap<PotionType> potionsMap = null;
    public boolean trackEntityEquipment = false;
    private EnumMap<EquipmentSlot, ItemStack> equipment = null;

    public PacketEntity(GrimPlayer player, EntityType type) {
        super(type);
        this.uuid = null;
        initAttributes(player);
        this.trackedServerPosition = new TrackedPosition();
    }

    public PacketEntity(GrimPlayer player, UUID uuid, EntityType type, double x, double y, double z) {
        super(type);
        this.uuid = uuid;
        initAttributes(player);
        this.trackedServerPosition = new TrackedPosition();
        this.trackedServerPosition.setPos(new Vector3d(x, y, z));
        if (player.getClientVersion().isOlderThan(ClientVersion.V_1_9)) { // Thanks ViaVersion
            trackedServerPosition.setPos(new Vector3d(((int) (x * 32)) / 32d, ((int) (y * 32)) / 32d, ((int) (z * 32)) / 32d));
        }
        final Vector3d pos = trackedServerPosition.getPos();
        this.newPacketLocation = new ReachInterpolationData(player, new SimpleCollisionBox(pos.x, pos.y, pos.z, pos.x, pos.y, pos.z, false), trackedServerPosition, this);
    }

    protected void trackAttribute(ValuedAttribute valuedAttribute) {
        if (attributeMap.containsKey(valuedAttribute.attribute())) {
            throw new IllegalArgumentException("Attribute already exists on entity!");
        }
        attributeMap.put(valuedAttribute.attribute(), valuedAttribute);
    }

    protected void initAttributes(GrimPlayer player) {
        trackAttribute(ValuedAttribute.ranged(Attributes.SCALE, 1.0, 0.0625, 16)
                .requiredVersion(player, ClientVersion.V_1_20_5));
        trackAttribute(ValuedAttribute.ranged(Attributes.STEP_HEIGHT, 0.6f, 0, 10)
                .requiredVersion(player, ClientVersion.V_1_20_5));
        trackAttribute(ValuedAttribute.ranged(Attributes.GRAVITY, 0.08, -1, 1)
                .requiredVersion(player, ClientVersion.V_1_20_5));
    }

    public Optional<ValuedAttribute> getAttribute(Attribute attribute) {
        if (attribute == null) return Optional.empty();
        return Optional.ofNullable(attributeMap.get(attribute));
    }

    public void setAttribute(Attribute attribute, double value) {
        ValuedAttribute property = attributeMap.get(attribute);
        if (property == null) {
            throw new IllegalArgumentException("Cannot set attribute " + attribute.getName() + " for entity " + type.getName() + "!");
        }
        property.override(value);
    }

    public double getAttributeValue(Attribute attribute) {
        final ValuedAttribute property = attributeMap.get(attribute);
        if (property == null) {
            throw new IllegalArgumentException("Cannot get attribute " + attribute.getName() + " for entity " + type.getName() + "!");
        }
        return property.get();
    }

    public void resetAttributes() {
        attributeMap.values().forEach(ValuedAttribute::reset);
    }

    // Set the old packet location to the new one
    // Set the new packet location to the updated packet location
    public void onFirstTransaction(boolean relative, boolean hasPos, double relX, double relY, double relZ, GrimPlayer player) {
        if (hasPos) {
            if (relative) {
                // This only matters for 1.9+ clients, but it won't hurt 1.8 clients either... align for imprecision
                final double scale = trackedServerPosition.getScale();
                Vector3d vec3d;
                if (player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_16)) {
                    vec3d = trackedServerPosition.withDelta(TrackedPosition.pack(relX, scale), TrackedPosition.pack(relY, scale), TrackedPosition.pack(relZ, scale));
                } else {
                    vec3d = trackedServerPosition.withDeltaLegacy(TrackedPosition.packLegacy(relX, scale), TrackedPosition.packLegacy(relY, scale), TrackedPosition.packLegacy(relZ, scale));
                }
                trackedServerPosition.setPos(vec3d);
            } else {
                trackedServerPosition.setPos(new Vector3d(relX, relY, relZ));
                // ViaVersion desync's here for teleports
                // It simply teleports the entity with its position divided by 32... ignoring the offset this causes.
                // Thanks a lot ViaVersion!  Please don't fix this, or it will be a pain to support.
                if (player.getClientVersion().isOlderThan(ClientVersion.V_1_9)) {
                    trackedServerPosition.setPos(new Vector3d(((int) (relX * 32)) / 32d, ((int) (relY * 32)) / 32d, ((int) (relZ * 32)) / 32d));
                }
            }
        }
        this.oldPacketLocation = newPacketLocation;
        // BUG FIX LOGIC for https://bugs.mojang.com/browse/MC-255263
        // 1. We MUST check !hasPos. If hasPos is true, we must let standard interpolation (4-arg) run.
        // 2. The 3-arg constructor is for versions where the client FREEZES (targets current pos) when rot only packets come in
        if (!hasPos &&
                // Logic for versions that FREEZE (Target = Current)
                // 1.21.5 -> 1.21.8 (regression)
                ((player.getClientVersion().isOlderThan(ClientVersion.V_1_21_9) && player.getClientVersion().isNewerThan(ClientVersion.V_1_21_4)) ||
                        // 1.15 -> 1.20.1 (Old bug)
                        (player.getClientVersion().isOlderThan(ClientVersion.V_1_20_2) && player.getClientVersion().isNewerThan(ClientVersion.V_1_14_4)))
        ) {
            // Apply Freeze Fix (Start = Box, Target = Box)
            this.newPacketLocation = new ReachInterpolationData(
                    player,
                    oldPacketLocation.getPossibleLocationCombined(),
                    this
            );
        } else {
            // Standard Interpolation (Start = Box, Target = ServerPos)
            // This naturally fixes the "Slowdown"/Interpolation Reset in 1.20.2-1.21.4 and 1.21.9+ resetting the lerp timer
            this.newPacketLocation = new ReachInterpolationData(player, oldPacketLocation.getPossibleLocationCombined(), trackedServerPosition, this);
        }

        // In versions < 1.16.2 when the client receives non-relative teleport for an entity
        // And they move less by the thresholds given, the entity does not move client side
        if (hasPos && !relative && player.getClientVersion().isOlderThanOrEquals(ClientVersion.V_1_16_1)) {
            SimpleCollisionBox clientArea = newPacketLocation.getPossibleLocationCombined();
            if (clientArea.distanceX(relX) < 0.03125D
                    && clientArea.distanceY(relY) < 0.015625D
                    && clientArea.distanceZ(relZ) < 0.03125D) {
                newPacketLocation.expandNonRelative();
            }
        }
    }

    // Remove the possibility of the old packet location
    public void onSecondTransaction() {
        this.oldPacketLocation = null;
    }

    // If the old and new packet location are split, we need to combine bounding boxes
    public void onMovement(boolean tickingReliably) {
        newPacketLocation.tickMovement(oldPacketLocation == null, tickingReliably);

        // Handle uncertainty of second transaction spanning over multiple ticks
        if (oldPacketLocation != null) {
            oldPacketLocation.tickMovement(true, tickingReliably);
            newPacketLocation.updatePossibleStartingLocation(oldPacketLocation.getPossibleLocationCombined());
        }
    }

    public boolean hasPassenger(PacketEntity entity) {
        return passengers.contains(entity);
    }

    public void mount(PacketEntity vehicle) {
        if (riding != null) eject();
        vehicle.passengers.add(this);
        riding = vehicle;
    }

    public void eject() {
        if (riding != null) {
            riding.passengers.remove(this);
        }
        this.riding = null;
    }

    // This is for handling riding and entities attached to one another.
    public void setPositionRaw(GrimPlayer player, SimpleCollisionBox box) {
        // I'm disappointed in you mojang.  Please don't set the packet position as it desyncs it...
        // But let's follow this flawed client-sided logic!
        this.trackedServerPosition.setPos(new Vector3d((box.maxX - box.minX) / 2 + box.minX, box.minY, (box.maxZ - box.minZ) / 2 + box.minZ));
        // This disables interpolation
        this.newPacketLocation = new ReachInterpolationData(player, box, this);
    }

    public SimpleCollisionBox getPossibleLocationBoxes() {
        if (oldPacketLocation == null) {
            return newPacketLocation.getPossibleLocationCombined();
        }

        return ReachInterpolationData.combineCollisionBox(oldPacketLocation.getPossibleLocationCombined(), newPacketLocation.getPossibleLocationCombined());
    }

    public SimpleCollisionBox getPossibleCollisionBoxes() {
        if (oldPacketLocation == null) {
            return newPacketLocation.getPossibleHitboxCombined();
        }

        return ReachInterpolationData.combineCollisionBox(oldPacketLocation.getPossibleHitboxCombined(), newPacketLocation.getPossibleHitboxCombined());
    }

    public OptionalInt getPotionEffectLevel(PotionType effect) {
        final int amplifier = potionsMap == null ? -1 : potionsMap.getInt(effect);
        return amplifier == -1 ? OptionalInt.empty() : OptionalInt.of(amplifier);
    }

    public boolean hasPotionEffect(PotionType effect) {
        return potionsMap != null && potionsMap.containsKey(effect);
    }

    public void addPotionEffect(PotionType effect, int amplifier) {
        if (potionsMap == null) {
            potionsMap = new Object2IntOpenHashMap<>();
            potionsMap.defaultReturnValue(-1);
        }
        potionsMap.put(effect, amplifier);
    }

    public void removePotionEffect(PotionType effect) {
        if (potionsMap == null) return;
        potionsMap.removeInt(effect);
    }

    // Mojang makes this default to true and overrides it for everything where it isn't
    // That's too much work for us to replicate...
    // This is temporary hack and technically wrong
    /* By Default every entity in the game cannot be hit by player crosshair. This is overwritten as follows as of 1.21.1:
      Most Boats, Minecart's, TNT, Falling Blocks, and LivingEntities can only be hit if they're not removed
      Every single BlockAttachedEntity can be hit (Leashes and other decorations)
      End Crystals and IntersecetionEntities can be hit
      Ender Dragon entity itself cannot be hit but its parts can be
      ArmorStands can only be hit if they're not removed AND they're not markers.
      Of all Projectiles, only redirectable ones (Fireballs - not blaze fireballs, Wind Charge, and Breeze Wind charges) can be hit
      Persistent Projectiles can only be hit if they're not on the ground and redirectable
    */
    // TLDR If we want to get 90% of the way there everything can be hit except for fishing rod bobbers, arrows, and marker armor stands
    public boolean canHit() {
        return !this.isDead;
    }

    public void setItemBySlot(EquipmentSlot slot, ItemStack item) {
        if (item == ItemStack.EMPTY && getItemBySlot(slot) == ItemStack.EMPTY) {
            return;
        }

        if (equipment == null) {
            equipment = new EnumMap<>(EquipmentSlot.class);
        }

        equipment.put(slot, item);
    }

    public ItemStack getItemBySlot(EquipmentSlot slot) {
        if (equipment == null) {
            return ItemStack.EMPTY;
        }

        return equipment.getOrDefault(slot, ItemStack.EMPTY);
    }

    public boolean hasItemInSlot(EquipmentSlot slot) {
        if (equipment == null) {
            return false;
        }

        ItemStack item = equipment.get(slot);
        return item != null && !item.isEmpty();
    }

}
