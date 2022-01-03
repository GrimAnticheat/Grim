package ac.grim.grimac.events.packets;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.update.BlockPlace;
import ac.grim.grimac.utils.anticheat.update.PositionUpdate;
import ac.grim.grimac.utils.anticheat.update.RotationUpdate;
import ac.grim.grimac.utils.anticheat.update.VehiclePositionUpdate;
import ac.grim.grimac.utils.blockplace.BlockPlaceResult;
import ac.grim.grimac.utils.blockplace.ConsumesBlockPlace;
import ac.grim.grimac.utils.collisions.HitboxData;
import ac.grim.grimac.utils.collisions.datatypes.CollisionBox;
import ac.grim.grimac.utils.collisions.datatypes.SimpleCollisionBox;
import ac.grim.grimac.utils.data.HitData;
import ac.grim.grimac.utils.data.Pair;
import ac.grim.grimac.utils.data.TeleportAcceptData;
import ac.grim.grimac.utils.enums.FluidTag;
import ac.grim.grimac.utils.math.GrimMath;
import ac.grim.grimac.utils.math.VectorUtils;
import ac.grim.grimac.utils.nmsutil.*;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.impl.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.impl.PacketSendEvent;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.ConnectionState;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.item.enchantment.type.EnchantmentTypes;
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.player.InteractionHand;
import com.github.retrooper.packetevents.protocol.potion.PotionTypes;
import com.github.retrooper.packetevents.protocol.world.BlockFace;
import com.github.retrooper.packetevents.protocol.world.MaterialType;
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import com.github.retrooper.packetevents.protocol.world.states.defaulttags.BlockTags;
import com.github.retrooper.packetevents.protocol.world.states.type.StateType;
import com.github.retrooper.packetevents.protocol.world.states.type.StateTypes;
import com.github.retrooper.packetevents.protocol.world.states.type.StateValue;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.util.Vector3i;
import com.github.retrooper.packetevents.wrapper.play.client.*;
import org.bukkit.GameMode;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

public class CheckManagerListener extends PacketListenerAbstract {

    long lastPosLook = 0;

    public CheckManagerListener() {
        super(PacketListenerPriority.LOW);
    }

    // Copied from MCP...
    // Returns null if there isn't anything.
    //
    // I do have to admit that I'm starting to like bifunctions/new java 8 things more than I originally did.
    // although I still don't understand Mojang's obsession with streams in some of the hottest methods... that kills performance
    static HitData traverseBlocks(GrimPlayer player, Vector3d start, Vector3d end, BiFunction<WrappedBlockState, Vector3i, HitData> predicate) {
        // I guess go back by the collision epsilon?
        double endX = GrimMath.lerp(-1.0E-7D, end.x, start.x);
        double endY = GrimMath.lerp(-1.0E-7D, end.y, start.y);
        double endZ = GrimMath.lerp(-1.0E-7D, end.z, start.z);
        double startX = GrimMath.lerp(-1.0E-7D, start.x, end.x);
        double startY = GrimMath.lerp(-1.0E-7D, start.y, end.y);
        double startZ = GrimMath.lerp(-1.0E-7D, start.z, end.z);
        int floorStartX = GrimMath.floor(startX);
        int floorStartY = GrimMath.floor(startY);
        int floorStartZ = GrimMath.floor(startZ);


        if (start.equals(end)) return null;

        WrappedBlockState state = player.compensatedWorld.getWrappedBlockStateAt(floorStartX, floorStartY, floorStartZ);
        HitData apply = predicate.apply(state, new Vector3i(floorStartX, floorStartY, floorStartZ));

        if (apply != null) {
            return apply;
        }

        double xDiff = endX - startX;
        double yDiff = endY - startY;
        double zDiff = endZ - startZ;
        int xSign = GrimMath.sign(xDiff);
        int ySign = GrimMath.sign(yDiff);
        int zSign = GrimMath.sign(zDiff);

        double posXInverse = xSign == 0 ? Double.MAX_VALUE : xSign / xDiff;
        double posYInverse = ySign == 0 ? Double.MAX_VALUE : ySign / yDiff;
        double posZInverse = zSign == 0 ? Double.MAX_VALUE : zSign / zDiff;

        double d12 = posXInverse * (xSign > 0 ? 1.0D - GrimMath.frac(startX) : GrimMath.frac(startX));
        double d13 = posYInverse * (ySign > 0 ? 1.0D - GrimMath.frac(startY) : GrimMath.frac(startY));
        double d14 = posZInverse * (zSign > 0 ? 1.0D - GrimMath.frac(startZ) : GrimMath.frac(startZ));

        // Can't figure out what this code does currently
        while (d12 <= 1.0D || d13 <= 1.0D || d14 <= 1.0D) {
            if (d12 < d13) {
                if (d12 < d14) {
                    floorStartX += xSign;
                    d12 += posXInverse;
                } else {
                    floorStartZ += zSign;
                    d14 += posZInverse;
                }
            } else if (d13 < d14) {
                floorStartY += ySign;
                d13 += posYInverse;
            } else {
                floorStartZ += zSign;
                d14 += posZInverse;
            }

            state = player.compensatedWorld.getWrappedBlockStateAt(floorStartX, floorStartY, floorStartZ);
            apply = predicate.apply(state, new Vector3i(floorStartX, floorStartY, floorStartZ));

            if (apply != null) {
                return apply;
            }
        }

        return null;
    }

    private void handleFlying(GrimPlayer player, double x, double y, double z, float yaw, float pitch, boolean hasPosition, boolean hasLook, boolean onGround, PacketReceiveEvent event) {
        player.packetStateData.lastPacketWasTeleport = false;
        TeleportAcceptData teleportData = null;
        if (hasPosition) {
            Vector3d position = VectorUtils.clampVector(new Vector3d(x, y, z));
            teleportData = player.getSetbackTeleportUtil().checkTeleportQueue(position.getX(), position.getY(), position.getZ());
            player.packetStateData.lastPacketWasTeleport = teleportData.isTeleport();
        }

        // Don't check duplicate 1.17 packets (Why would you do this mojang?)
        // Don't check rotation since it changes between these packets, with the second being irrelevant.
        //
        // If a player sends a POS LOOK in a vehicle... then it was this stupid fucking mechanic
        if (hasPosition && hasLook && !player.packetStateData.lastPacketWasTeleport &&
                (player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_17) &&
                        new Vector3d(player.x, player.y, player.z).equals(new Vector3d(x, y, z))) || player.inVehicle) {
            // We will take the rotation though
            player.lastXRot = player.xRot;
            player.lastYRot = player.yRot;

            player.xRot = yaw;
            player.yRot = pitch;

            float deltaXRot = player.xRot - player.lastXRot;
            float deltaYRot = player.yRot - player.lastYRot;

            final RotationUpdate update = new RotationUpdate(player.lastXRot, player.lastYRot, player.xRot, player.yRot, deltaXRot, deltaYRot);
            player.checkManager.onRotationUpdate(update);

            lastPosLook = System.currentTimeMillis();
            player.packetStateData.lastPacketWasOnePointSeventeenDuplicate = true;

            // Don't let players on 1.17+ clients on 1.8- servers FastHeal by right-clicking
            // the ground with a bucket... ViaVersion marked this as a WONTFIX, so I'll include the fix.
            if (PacketEvents.getAPI().getServerManager().getVersion().isOlderThanOrEquals(ServerVersion.V_1_8_8)) {
                event.setCancelled(true);
            }
            return;
        }

        lastPosLook = System.currentTimeMillis();

        SimpleCollisionBox oldBB = player.boundingBox;
        player.boundingBox = GetBoundingBox.getBoundingBoxFromPosAndSize(player.x, player.y, player.z, 0.66, 1.8);
        // Check for blocks within 0.03 of the player's position before allowing ground to be true - if 0.03
        boolean nearGround = Collisions.collide(player, 0, -0.03, 0).getY() != -0.03;
        player.boundingBox = oldBB;
        if (!hasPosition && onGround != player.packetStateData.packetPlayerOnGround && nearGround && player.clientVelocity.getY() < 0.03) {
            player.lastOnGround = true;
            player.uncertaintyHandler.onGroundUncertain = true;
            player.uncertaintyHandler.lastTickWasNearGroundZeroPointZeroThree = true;
            player.clientClaimsLastOnGround = true;
        }

        player.lastX = player.x;
        player.lastY = player.y;
        player.lastZ = player.z;
        player.lastXRot = player.xRot;
        player.lastYRot = player.yRot;

        player.packetStateData.packetPlayerOnGround = onGround;

        if (hasLook) {
            player.xRot = yaw;
            player.yRot = pitch;
        }

        if (hasPosition) {
            Vector3d position = new Vector3d(x, y, z);
            Vector3d clampVector = VectorUtils.clampVector(position);

            player.x = clampVector.getX();
            player.y = clampVector.getY();
            player.z = clampVector.getZ();

            final PositionUpdate update = new PositionUpdate(new Vector3d(player.x, player.y, player.z), position, onGround, teleportData.isTeleport(), teleportData.getSetback());
            player.checkManager.onPositionUpdate(update);
        }

        if (hasLook && !player.packetStateData.lastPacketWasOnePointSeventeenDuplicate) {
            float deltaXRot = player.xRot - player.lastXRot;
            float deltaYRot = player.yRot - player.lastYRot;

            final RotationUpdate update = new RotationUpdate(player.lastXRot, player.lastYRot, player.xRot, player.yRot, deltaXRot, deltaYRot);
            player.checkManager.onRotationUpdate(update);
        }

        player.packetStateData.lastPacketWasOnePointSeventeenDuplicate = false;

        player.packetStateData.didLastLastMovementIncludePosition = player.packetStateData.didLastMovementIncludePosition;
        player.packetStateData.didLastMovementIncludePosition = hasPosition;
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getConnectionState() != ConnectionState.PLAY) return;
        GrimPlayer player = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer((Player) event.getPlayer());
        if (player == null) return;

        // Flying packet types
        if (event.getPacketType() == PacketType.Play.Client.PLAYER_POSITION) {
            WrapperPlayClientPlayerPosition wrapper = new WrapperPlayClientPlayerPosition(event);
            Vector3d pos = wrapper.getPosition();
            handleFlying(player, pos.getX(), pos.getY(), pos.getZ(), 0, 0, true, false, wrapper.isOnGround(), event);
        } else if (event.getPacketType() == PacketType.Play.Client.PLAYER_POSITION_AND_ROTATION) {
            WrapperPlayClientPlayerPositionAndRotation wrapper = new WrapperPlayClientPlayerPositionAndRotation(event);
            Vector3d pos = wrapper.getPosition();
            handleFlying(player, pos.getX(), pos.getY(), pos.getZ(), wrapper.getYaw(), wrapper.getPitch(), true, true, wrapper.isOnGround(), event);
        } else if (event.getPacketType() == PacketType.Play.Client.PLAYER_ROTATION) {
            WrapperPlayClientPlayerRotation wrapper = new WrapperPlayClientPlayerRotation(event);
            handleFlying(player, 0, 0, 0, wrapper.getYaw(), wrapper.getPitch(), false, true, wrapper.isOnGround(), event);
        } else if (event.getPacketType() == PacketType.Play.Client.PLAYER_FLYING) {
            WrapperPlayClientPlayerFlying wrapper = new WrapperPlayClientPlayerFlying(event);
            handleFlying(player, 0, 0, 0, 0, 0, false, false, wrapper.isOnGround(), event);
        }

        if (event.getPacketType() == PacketType.Play.Client.VEHICLE_MOVE) {
            WrapperPlayClientVehicleMove move = new WrapperPlayClientVehicleMove(event);
            Vector3d position = move.getPosition();

            player.lastX = player.x;
            player.lastY = player.y;
            player.lastZ = player.z;

            Vector3d clamp = VectorUtils.clampVector(position);
            player.x = clamp.getX();
            player.y = clamp.getY();
            player.z = clamp.getZ();

            final boolean isTeleport = player.getSetbackTeleportUtil().checkVehicleTeleportQueue(position.getX(), position.getY(), position.getZ());
            player.packetStateData.lastPacketWasTeleport = isTeleport;
            final VehiclePositionUpdate update = new VehiclePositionUpdate(clamp, position, move.getYaw(), move.getPitch(), isTeleport);
            player.checkManager.onVehiclePositionUpdate(update);

            player.packetStateData.receivedSteerVehicle = false;
        }

        if (event.getPacketType() == PacketType.Play.Client.PLAYER_DIGGING) {
            WrapperPlayClientPlayerDigging dig = new WrapperPlayClientPlayerDigging(event);

            if (dig.getAction() == WrapperPlayClientPlayerDigging.Action.FINISHED_DIGGING) {
                WrappedBlockState block = player.compensatedWorld.getWrappedBlockStateAt(dig.getBlockPosition());
                // Not unbreakable
                if (block.getType().getHardness() != -1.0f) {
                    player.compensatedWorld.updateBlock(dig.getBlockPosition().getX(), dig.getBlockPosition().getY(), dig.getBlockPosition().getZ(), 0);
                }
            }

            if (dig.getAction() == WrapperPlayClientPlayerDigging.Action.START_DIGGING) {
                // GET destroy speed
                // Starts with itemstack get destroy speed
                ItemStack tool = player.getInventory().getHeldItem();

                // A creative mode player cannot break things with a sword!
                if (player.gamemode == GameMode.CREATIVE && tool.getType().toString().contains("SWORD")) {
                    return;
                }

                WrappedBlockState block = player.compensatedWorld.getWrappedBlockStateAt(dig.getBlockPosition());

                boolean isBestTool = false;
                float speedMultiplier = 1.0f;

                // 1.13 and below need their own huge methods to support this...
                if (tool.getType().hasAttribute(ItemTypes.ItemAttribute.AXE)) {
                    isBestTool = BlockTags.MINEABLE_WITH_AXE.contains(block.getType());
                } else if (tool.getType().hasAttribute(ItemTypes.ItemAttribute.PICKAXE)) {
                    isBestTool = BlockTags.MINEABLE_WITH_PICKAXE.contains(block.getType());
                } else if (tool.getType().hasAttribute(ItemTypes.ItemAttribute.SHOVEL)) {
                    isBestTool = BlockTags.MINEABLE_WITH_SHOVEL.contains(block.getType());
                }

                if (isBestTool) {
                    int tier = 0;
                    if (tool.getType().hasAttribute(ItemTypes.ItemAttribute.WOOD_TIER)) { // Tier 0
                        speedMultiplier = 2.0f;
                    } else if (tool.getType().hasAttribute(ItemTypes.ItemAttribute.STONE_TIER)) { // Tier 1
                        speedMultiplier = 4.0f;
                        tier = 1;
                    } else if (tool.getType().hasAttribute(ItemTypes.ItemAttribute.IRON_TIER)) { // Tier 2
                        speedMultiplier = 6.0f;
                        tier = 2;
                    } else if (tool.getType().hasAttribute(ItemTypes.ItemAttribute.DIAMOND_TIER)) { // Tier 3
                        speedMultiplier = 8.0f;
                        tier = 3;
                    } else if (tool.getType().hasAttribute(ItemTypes.ItemAttribute.GOLD_TIER)) { // Tier 0
                        speedMultiplier = 12.0f;
                    } else if (tool.getType().hasAttribute(ItemTypes.ItemAttribute.NETHERITE_TIER)) { // Tier 4
                        speedMultiplier = 9.0f;
                        tier = 4;
                    }

                    if (tier < 3 && BlockTags.NEEDS_DIAMOND_TOOL.contains(block.getType())) {
                        isBestTool = false;
                    } else if (tier < 2 && BlockTags.NEEDS_IRON_TOOL.contains(block.getType())) {
                        isBestTool = false;
                    } else if (tier < 1 && BlockTags.NEEDS_STONE_TOOL.contains(block.getType())) {
                        isBestTool = false;
                    }
                }

                // Shears can mine some blocks faster
                if (tool.getType() == ItemTypes.SHEARS) {
                    if (block.getType() == StateTypes.COBWEB || Materials.isLeaves(block.getType())) {
                        speedMultiplier = 15.0f;
                    } else if (BlockTags.WOOL.contains(block.getType())) {
                        speedMultiplier = 5.0f;
                    } else if (block.getType() == StateTypes.VINE ||
                            block.getType() == StateTypes.GLOW_LICHEN) {
                        speedMultiplier = 2.0f;
                    }

                    isBestTool = block.getType() == StateTypes.COBWEB ||
                            block.getType() == StateTypes.REDSTONE_WIRE ||
                            block.getType() == StateTypes.TRIPWIRE;
                }

                // Swords can also mine some blocks faster
                if (tool.getType().hasAttribute(ItemTypes.ItemAttribute.SWORD)) {
                    if (block.getType() == StateTypes.COBWEB) {
                        speedMultiplier = 15.0f;
                    } else if (block.getType().getMaterialType() == MaterialType.PLANT ||
                            BlockTags.LEAVES.contains(block.getType()) ||
                            block.getType() == StateTypes.PUMPKIN ||
                            block.getType() == StateTypes.MELON) {
                        speedMultiplier = 1.5f;
                    }

                    isBestTool = block.getType() == StateTypes.COBWEB;
                }

                float blockHardness = block.getType().getHardness();

                if (isBestTool) {
                    if (blockHardness == -1.0f) {
                        speedMultiplier = 0;
                    } else {
                        int digSpeed = tool.getEnchantmentLevel(EnchantmentTypes.BLOCK_EFFICIENCY);
                        if (digSpeed > 0) {
                            speedMultiplier += digSpeed * digSpeed + 1;
                        }
                    }
                }

                Integer digSpeed = player.compensatedPotions.getPotionLevel(PotionTypes.HASTE);
                Integer conduit = player.compensatedPotions.getPotionLevel(PotionTypes.CONDUIT_POWER);

                if (digSpeed != null || conduit != null) {
                    int i = 0;
                    int j = 0;
                    if (digSpeed != null) {
                        i = digSpeed;
                    }

                    if (conduit != null) {
                        j = conduit;
                    }

                    int hasteLevel = Math.max(i, j);

                    speedMultiplier *= 1 + (0.2 * hasteLevel);
                }

                Integer miningFatigue = player.compensatedPotions.getPotionLevel(PotionTypes.MINING_FATIGUE);

                if (miningFatigue != null) {
                    switch (miningFatigue) {
                        case 0:
                            speedMultiplier *= 0.3;
                            break;
                        case 1:
                            speedMultiplier *= 0.09;
                            break;
                        case 2:
                            speedMultiplier *= 0.0027;
                            break;
                        default:
                            speedMultiplier *= 0.00081;
                    }
                }

                boolean hasAquaAffinity = false;

                ItemStack helmet = player.getInventory().getHelmet();
                ItemStack chestplate = player.getInventory().getChestplate();
                ItemStack leggings = player.getInventory().getLeggings();
                ItemStack boots = player.getInventory().getBoots();

                if ((helmet != null && helmet.getEnchantmentLevel(EnchantmentTypes.AQUA_AFFINITY) > 0) ||
                        (chestplate != null && chestplate.getEnchantmentLevel(EnchantmentTypes.AQUA_AFFINITY) > 0) ||
                        (leggings != null && leggings.getEnchantmentLevel(EnchantmentTypes.AQUA_AFFINITY) > 0) ||
                        (boots != null && boots.getEnchantmentLevel(EnchantmentTypes.AQUA_AFFINITY) > 0)) {
                    hasAquaAffinity = true;
                }

                if (player.fluidOnEyes == FluidTag.WATER && !hasAquaAffinity) {
                    speedMultiplier /= 5;
                }

                if (!player.onGround) {
                    speedMultiplier /= 5;
                }

                float damage = speedMultiplier / blockHardness;

                boolean canHarvest = !block.getType().isRequiresCorrectTool() || isBestTool;
                if (canHarvest) {
                    damage /= 30;
                } else {
                    damage /= 100;
                }

                //Instant breaking
                if (damage > 1 || player.gamemode == GameMode.CREATIVE) {
                    player.compensatedWorld.updateBlock(dig.getBlockPosition().getX(), dig.getBlockPosition().getY(), dig.getBlockPosition().getZ(),
                            0);
                }
            }

        }

        boolean isBlockPlace = event.getPacketType() == PacketType.Play.Client.PLAYER_BLOCK_PLACEMENT;

        // Check for interactable first (door, etc)
        if (isBlockPlace) {
            WrapperPlayClientBlockPlacement place = new WrapperPlayClientBlockPlacement(event);

            ItemStack placedWith = player.getInventory().getHeldItem();
            ItemStack offhand = player.getInventory().getOffHand();

            boolean onlyAir = placedWith.isEmpty() && offhand.isEmpty();

            // The offhand is unable to interact with blocks like this... try to stop some desync points before they happen
            if ((!player.isSneaking || onlyAir) && place.getHand() == InteractionHand.MAIN_HAND) {
                Vector3i blockPosition = place.getBlockPosition();
                BlockPlace blockPlace = new BlockPlace(player, blockPosition, place.getFace(), placedWith.getType(), getNearestHitResult(player, null, true));

                // Right-clicking a trapdoor/door/etc.
                if (Materials.isClientSideInteractable(blockPlace.getPlacedAgainstMaterial())) {
                    Vector3i location = blockPlace.getPlacedAgainstBlockLocation();
                    player.compensatedWorld.tickOpenable(location.getX(), location.getY(), location.getZ());
                    return;
                }

                // This also has side effects
                // This method is for when the block doesn't always consume the click
                // This causes a ton of desync's but mojang doesn't seem to care...
                if (ConsumesBlockPlace.consumesPlace(player, player.compensatedWorld.getWrappedBlockStateAt(blockPlace.getPlacedAgainstBlockLocation()), blockPlace)) {
                    return;
                }
            }
        }

        if (event.getPacketType() == PacketType.Play.Client.USE_ITEM) {
            WrapperPlayClientUseItem place = new WrapperPlayClientUseItem(event);

            ItemStack placedWith = player.getInventory().getHeldItem();
            if (place.getHand() == InteractionHand.OFF_HAND) {
                placedWith = player.getInventory().getOffHand();
            }

            // Lilypads are USE_ITEM (THIS CAN DESYNC, WTF MOJANG)
            if (placedWith.getType() == ItemTypes.LILY_PAD) {
                placeLilypad(player); // Pass a block place because lily pads have a hitbox
                return;
            }

            StateType toBucketMat = Materials.transformBucketMaterial(placedWith.getType());
            if (toBucketMat != null) {
                placeWaterLavaSnowBucket(player, toBucketMat);
            }

            if (placedWith.getType() == ItemTypes.BUCKET) {
                placeBucket(player);
            }
        }

        if (isBlockPlace) {
            WrapperPlayClientBlockPlacement place = new WrapperPlayClientBlockPlacement(event);
            Vector3i blockPosition = place.getBlockPosition();
            BlockFace face = place.getFace();

            ItemStack placedWith = player.getInventory().getHeldItem();
            if (place.getHand() == InteractionHand.OFF_HAND) {
                placedWith = player.getInventory().getOffHand();
            }

            BlockPlace blockPlace = new BlockPlace(player, blockPosition, face, placedWith.getType(), getNearestHitResult(player, null, true));

            if (placedWith.getType().getPlacedType() != null || placedWith.getType() == ItemTypes.FIRE_CHARGE) {
                player.checkManager.onBlockPlace(blockPlace);

                if (!blockPlace.isCancelled()) {
                    BlockPlaceResult.getMaterialData(placedWith.getType()).applyBlockPlaceToWorld(player, blockPlace);
                }
            }
        }

        // Call the packet checks last as they can modify the contents of the packet
        // Such as the NoFall check setting the player to not be on the ground
        player.checkManager.onPacketReceive(event);
    }

    private void placeWaterLavaSnowBucket(GrimPlayer player, StateType toPlace) {
        HitData data = getNearestHitResult(player, StateTypes.AIR, false);
        if (data != null) {
            BlockPlace blockPlace = new BlockPlace(player, data.getPosition(), data.getClosestDirection(), ItemTypes.AIR, data);
            // Powder snow, lava, and water all behave like placing normal blocks after checking for waterlogging (replace clicked always false though)
            // If we hit a waterloggable block, then the bucket is directly placed
            // Otherwise, use the face to determine where to place the bucket
            if (Materials.isPlaceableLiquidBucket(blockPlace.getItemType()) && PacketEvents.getAPI().getServerManager().getVersion().isNewerThanOrEquals(ServerVersion.V_1_13)) {
                blockPlace.setReplaceClicked(true); // See what's in the existing place
                WrappedBlockState existing = blockPlace.getExistingBlockData();
                if (existing.getInternalData().containsKey(StateValue.WATERLOGGED)) {
                    existing.setWaterlogged(true);
                    blockPlace.set(existing);
                    return;
                }
            }

            // Powder snow, lava, and water all behave like placing normal blocks after checking for waterlogging (replace clicked always false though)
            blockPlace.setReplaceClicked(false);
            blockPlace.set(toPlace);
        }
    }

    private void placeBucket(GrimPlayer player) {
        HitData data = getNearestHitResult(player, null, true);

        if (data != null) {
            BlockPlace blockPlace = new BlockPlace(player, data.getPosition(), data.getClosestDirection(), ItemTypes.BUCKET, data);
            blockPlace.setReplaceClicked(true); // Replace the block clicked, not the block in the direction

            if (data.getState().getType() == StateTypes.POWDER_SNOW) {
                blockPlace.set(StateTypes.AIR);
                return;
            }

            // We didn't hit fluid source
            if (!player.compensatedWorld.isWaterSourceBlock(data.getPosition().getX(), data.getPosition().getY(), data.getPosition().getZ()))
                return;

            if (PacketEvents.getAPI().getServerManager().getVersion().isNewerThanOrEquals(ServerVersion.V_1_13)) {
                WrappedBlockState existing = blockPlace.getExistingBlockData();
                if (existing.getInternalData().containsKey(StateValue.WATERLOGGED)) { // waterloggable
                    existing.setWaterlogged(false);
                    blockPlace.set(existing);
                    return;
                }
            }

            // Therefore, not waterlogged and is a fluid, and is therefore a source block
            blockPlace.set(StateTypes.AIR);
        }
    }

    private void placeLilypad(GrimPlayer player) {
        HitData data = getNearestHitResult(player, null, true);

        if (data != null) {
            // A lilypad cannot replace a fluid
            if (player.compensatedWorld.getFluidLevelAt(data.getPosition().getX(), data.getPosition().getY() + 1, data.getPosition().getZ()) > 0)
                return;

            BlockPlace blockPlace = new BlockPlace(player, data.getPosition(), data.getClosestDirection(), ItemTypes.LILY_PAD, data);
            blockPlace.setReplaceClicked(false); // Not possible with use item

            // We checked for a full fluid block below here.
            if (player.compensatedWorld.getWaterFluidLevelAt(data.getPosition().getX(), data.getPosition().getY(), data.getPosition().getZ()) > 0
                    || data.getState().getType() == StateTypes.ICE || data.getState().getType() == StateTypes.FROSTED_ICE) {
                Vector3i pos = data.getPosition().clone();
                pos.setY(pos.getY() + 1);

                blockPlace.set(pos, StateTypes.LILY_PAD.createBlockState());
            }
        }
    }

    private HitData getNearestHitResult(GrimPlayer player, StateType heldItem, boolean sourcesHaveHitbox) {
        // TODO: When we do this post-tick (fix desync) switch to lastX
        Vector3d startingPos = new Vector3d(player.x, player.y + player.getEyeHeight(), player.z);
        Vector startingVec = new Vector(startingPos.getX(), startingPos.getY(), startingPos.getZ());
        Ray trace = new Ray(player, startingPos.getX(), startingPos.getY(), startingPos.getZ(), player.xRot, player.yRot);
        Vector endVec = trace.getPointAtDistance(6);
        Vector3d endPos = new Vector3d(endVec.getX(), endVec.getY(), endVec.getZ());

        return traverseBlocks(player, startingPos, endPos, (block, vector3i) -> {
            CollisionBox data = HitboxData.getBlockHitbox(player, heldItem, player.getClientVersion(), block, vector3i.getX(), vector3i.getY(), vector3i.getZ());
            List<SimpleCollisionBox> boxes = new ArrayList<>();
            data.downCast(boxes);

            double bestHitResult = Double.MAX_VALUE;
            Vector bestHitLoc = null;
            BlockFace bestFace = null;

            for (SimpleCollisionBox box : boxes) {
                Pair<Vector, BlockFace> intercept = ReachUtils.calculateIntercept(box, trace.getOrigin(), trace.getPointAtDistance(6));
                if (intercept.getFirst() == null) continue; // No intercept

                Vector hitLoc = intercept.getFirst();

                if (hitLoc.distanceSquared(startingVec) < bestHitResult) {
                    bestHitResult = hitLoc.distanceSquared(startingVec);
                    bestHitLoc = hitLoc;
                    bestFace = intercept.getSecond();
                }
            }
            if (bestHitLoc != null) {
                return new HitData(vector3i, bestHitLoc, bestFace, block);
            }

            if (sourcesHaveHitbox &&
                    (player.compensatedWorld.isWaterSourceBlock(vector3i.getX(), vector3i.getY(), vector3i.getZ())
                            || player.compensatedWorld.getLavaFluidLevelAt(vector3i.getX(), vector3i.getY(), vector3i.getZ()) == (8 / 9f))) {
                double waterHeight = player.compensatedWorld.getFluidLevelAt(vector3i.getX(), vector3i.getY(), vector3i.getZ());
                SimpleCollisionBox box = new SimpleCollisionBox(vector3i.getX(), vector3i.getY(), vector3i.getZ(), vector3i.getX() + 1, vector3i.getY() + waterHeight, vector3i.getZ() + 1);

                Pair<Vector, BlockFace> intercept = ReachUtils.calculateIntercept(box, trace.getOrigin(), trace.getPointAtDistance(6));

                if (intercept.getFirst() != null) {
                    return new HitData(vector3i, intercept.getFirst(), intercept.getSecond(), block);
                }
            }

            return null;
        });
    }

    @Override
    public void onPacketSend(PacketSendEvent event) {
        if (event.getConnectionState() != ConnectionState.PLAY) return;
        GrimPlayer player = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer((Player) event.getPlayer());
        if (player == null) return;

        player.checkManager.onPacketSend(event);
    }
}
