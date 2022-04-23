package ac.grim.grimac.events.packets;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.update.*;
import ac.grim.grimac.utils.blockplace.BlockPlaceResult;
import ac.grim.grimac.utils.blockplace.ConsumesBlockPlace;
import ac.grim.grimac.utils.collisions.HitboxData;
import ac.grim.grimac.utils.collisions.datatypes.CollisionBox;
import ac.grim.grimac.utils.collisions.datatypes.SimpleCollisionBox;
import ac.grim.grimac.utils.data.HeadRotation;
import ac.grim.grimac.utils.data.HitData;
import ac.grim.grimac.utils.data.Pair;
import ac.grim.grimac.utils.data.TeleportAcceptData;
import ac.grim.grimac.utils.enums.FluidTag;
import ac.grim.grimac.utils.inventory.Inventory;
import ac.grim.grimac.utils.latency.CompensatedWorld;
import ac.grim.grimac.utils.math.GrimMath;
import ac.grim.grimac.utils.math.VectorUtils;
import ac.grim.grimac.utils.nmsutil.*;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.ConnectionState;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.item.enchantment.type.EnchantmentTypes;
import com.github.retrooper.packetevents.protocol.item.type.ItemType;
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.player.DiggingAction;
import com.github.retrooper.packetevents.protocol.player.GameMode;
import com.github.retrooper.packetevents.protocol.player.InteractionHand;
import com.github.retrooper.packetevents.protocol.potion.PotionTypes;
import com.github.retrooper.packetevents.protocol.world.BlockFace;
import com.github.retrooper.packetevents.protocol.world.Location;
import com.github.retrooper.packetevents.protocol.world.MaterialType;
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import com.github.retrooper.packetevents.protocol.world.states.defaulttags.BlockTags;
import com.github.retrooper.packetevents.protocol.world.states.type.StateType;
import com.github.retrooper.packetevents.protocol.world.states.type.StateTypes;
import com.github.retrooper.packetevents.protocol.world.states.type.StateValue;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.util.Vector3f;
import com.github.retrooper.packetevents.util.Vector3i;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import com.github.retrooper.packetevents.wrapper.play.client.*;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerBlockChange;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSetSlot;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

public class CheckManagerListener extends PacketListenerAbstract {

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

    private static void placeWaterLavaSnowBucket(GrimPlayer player, ItemStack held, StateType toPlace, InteractionHand hand) {
        HitData data = getNearestHitResult(player, StateTypes.AIR, false);
        if (data != null) {
            BlockPlace blockPlace = new BlockPlace(player, data.getPosition(), data.getClosestDirection(), held, data);

            boolean didPlace = false;

            // Powder snow, lava, and water all behave like placing normal blocks after checking for waterlogging (replace clicked always false though)
            // If we hit a waterloggable block, then the bucket is directly placed
            // Otherwise, use the face to determine where to place the bucket
            if (Materials.isPlaceableWaterBucket(blockPlace.getItemStack().getType()) && PacketEvents.getAPI().getServerManager().getVersion().isNewerThanOrEquals(ServerVersion.V_1_13)) {
                blockPlace.setReplaceClicked(true); // See what's in the existing place
                WrappedBlockState existing = blockPlace.getExistingBlockData();
                if (existing.getInternalData().containsKey(StateValue.WATERLOGGED)) {
                    // Strangely, the client does not predict waterlogged placements
                    didPlace = true;
                }
            }

            if (!didPlace) {
                // Powder snow, lava, and water all behave like placing normal blocks after checking for waterlogging (replace clicked always false though)
                blockPlace.setReplaceClicked(false);
                blockPlace.set(toPlace);
            }

            if (player.gamemode != GameMode.CREATIVE) {
                if (hand == InteractionHand.MAIN_HAND) {
                    player.getInventory().inventory.setHeldItem(ItemStack.builder().type(ItemTypes.BUCKET).amount(1).build());
                } else {
                    player.getInventory().inventory.setPlayerInventoryItem(Inventory.SLOT_OFFHAND, ItemStack.builder().type(ItemTypes.BUCKET).amount(1).build());
                }
            }
        }
    }

    public static void handleQueuedPlaces(GrimPlayer player, boolean hasLook, float pitch, float yaw, long now) {
        // Handle queue'd block places
        PacketWrapper packet;
        while ((packet = player.placeUseItemPackets.poll()) != null) {
            double lastX = player.x;
            double lastY = player.y;
            double lastZ = player.z;

            player.x = player.packetStateData.lastClaimedPosition.getX();
            player.y = player.packetStateData.lastClaimedPosition.getY();
            player.z = player.packetStateData.lastClaimedPosition.getZ();

            if (player.compensatedEntities.getSelf().getRiding() != null) {
                Vector3d posFromVehicle = BoundingBoxSize.getRidingOffsetFromVehicle(player.compensatedEntities.getSelf().getRiding(), player);
                player.x = posFromVehicle.getX();
                player.y = posFromVehicle.getY();
                player.z = posFromVehicle.getZ();
            }

            // Less than 15 milliseconds ago means this is likely (fix all look vectors being a tick behind server sided)
            // Or mojang had the idle packet... for the 1.7/1.8 clients
            // No idle packet on 1.9+
            if ((now - player.lastBlockPlaceUseItem < 15 || player.getClientVersion().isOlderThan(ClientVersion.V_1_9)) && hasLook) {
                player.xRot = yaw;
                player.yRot = pitch;

                handleBlockPlaceOrUseItem(packet, player);
            } else {
                // Store the prediction positions/look
                float lastXRot = player.xRot;
                float lastYRot = player.yRot;

                handleBlockPlaceOrUseItem(packet, player);

                // Reset positions/look to prediction
                player.xRot = lastXRot;
                player.yRot = lastYRot;
            }

            player.x = lastX;
            player.y = lastY;
            player.z = lastZ;
        }
    }

    private static void handleUseItem(GrimPlayer player, ItemStack placedWith, InteractionHand hand) {
        // Lilypads are USE_ITEM (THIS CAN DESYNC, WTF MOJANG)
        if (placedWith.getType() == ItemTypes.LILY_PAD) {
            placeLilypad(player, hand); // Pass a block place because lily pads have a hitbox
            return;
        }

        StateType toBucketMat = Materials.transformBucketMaterial(placedWith.getType());
        if (toBucketMat != null) {
            placeWaterLavaSnowBucket(player, placedWith, toBucketMat, hand);
        }

        if (placedWith.getType() == ItemTypes.BUCKET) {
            placeBucket(player, hand);
        }
    }

    private static void handleBlockPlaceOrUseItem(PacketWrapper packet, GrimPlayer player) {
        // Legacy "use item" packet
        if (packet instanceof WrapperPlayClientPlayerBlockPlacement &&
                PacketEvents.getAPI().getServerManager().getVersion().isOlderThan(ServerVersion.V_1_9)) {
            WrapperPlayClientPlayerBlockPlacement place = (WrapperPlayClientPlayerBlockPlacement) packet;

            if (player.gamemode == GameMode.SPECTATOR || player.gamemode == GameMode.ADVENTURE) return;

            if (place.getFace() == BlockFace.OTHER) {
                ItemStack placedWith = player.getInventory().getHeldItem();
                if (place.getHand() == InteractionHand.OFF_HAND) {
                    placedWith = player.getInventory().getOffHand();
                }

                handleUseItem(player, placedWith, place.getHand());
                return;
            }
        }

        if (packet instanceof WrapperPlayClientUseItem) {
            WrapperPlayClientUseItem place = (WrapperPlayClientUseItem) packet;

            if (player.gamemode == GameMode.SPECTATOR || player.gamemode == GameMode.ADVENTURE) return;

            ItemStack placedWith = player.getInventory().getHeldItem();
            if (place.getHand() == InteractionHand.OFF_HAND) {
                placedWith = player.getInventory().getOffHand();
            }

            handleUseItem(player, placedWith, place.getHand());
        }

        // Check for interactable first (door, etc)
        if (packet instanceof WrapperPlayClientPlayerBlockPlacement) {
            WrapperPlayClientPlayerBlockPlacement place = (WrapperPlayClientPlayerBlockPlacement) packet;

            ItemStack placedWith = player.getInventory().getHeldItem();
            ItemStack offhand = player.getInventory().getOffHand();

            boolean onlyAir = placedWith.isEmpty() && offhand.isEmpty();

            // The offhand is unable to interact with blocks like this... try to stop some desync points before they happen
            if ((!player.isSneaking || onlyAir) && place.getHand() == InteractionHand.MAIN_HAND) {
                Vector3i blockPosition = place.getBlockPosition();
                BlockPlace blockPlace = new BlockPlace(player, blockPosition, place.getFace(), placedWith, getNearestHitResult(player, null, true));

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

        if (packet instanceof WrapperPlayClientPlayerBlockPlacement) {
            WrapperPlayClientPlayerBlockPlacement place = (WrapperPlayClientPlayerBlockPlacement) packet;
            Vector3i blockPosition = place.getBlockPosition();
            BlockFace face = place.getFace();

            if (player.gamemode == GameMode.SPECTATOR || player.gamemode == GameMode.ADVENTURE) return;

            ItemStack placedWith = player.getInventory().getHeldItem();
            if (place.getHand() == InteractionHand.OFF_HAND) {
                placedWith = player.getInventory().getOffHand();
            }

            BlockPlace blockPlace = new BlockPlace(player, blockPosition, face, placedWith, getNearestHitResult(player, null, true));

            if (place.getInsideBlock().isPresent()) {
                blockPlace.setInside(place.getInsideBlock().get());
            }

            if (placedWith.getType().getPlacedType() != null || placedWith.getType() == ItemTypes.FIRE_CHARGE) {
                BlockPlaceResult.getMaterialData(placedWith.getType()).applyBlockPlaceToWorld(player, blockPlace);
            }
        }
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getConnectionState() != ConnectionState.PLAY) return;
        GrimPlayer player = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(event.getUser());
        if (player == null) return;

        player.checkManager.onPrePredictionReceivePacket(event);

        if (WrapperPlayClientPlayerFlying.isFlying(event.getPacketType())) {
            WrapperPlayClientPlayerFlying flying = new WrapperPlayClientPlayerFlying(event);

            Location pos = flying.getLocation();

            if (flying.hasPositionChanged()) {
                if (Double.isNaN(pos.getX()) || Double.isNaN(pos.getY()) || Double.isNaN(pos.getZ())) {
                    event.setCancelled(true);
                    return;
                }
            }

            handleFlying(player, pos.getX(), pos.getY(), pos.getZ(), pos.getYaw(), pos.getPitch(), flying.hasPositionChanged(), flying.hasRotationChanged(), flying.isOnGround(), event);
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

            player.xRot = move.getYaw();
            player.yRot = move.getPitch();

            final boolean isTeleport = player.getSetbackTeleportUtil().checkVehicleTeleportQueue(position.getX(), position.getY(), position.getZ());
            player.packetStateData.lastPacketWasTeleport = isTeleport;
            final VehiclePositionUpdate update = new VehiclePositionUpdate(clamp, position, move.getYaw(), move.getPitch(), isTeleport);
            player.checkManager.onVehiclePositionUpdate(update);

            player.packetStateData.receivedSteerVehicle = false;
        }

        if (event.getPacketType() == PacketType.Play.Client.PLAYER_DIGGING) {
            WrapperPlayClientPlayerDigging dig = new WrapperPlayClientPlayerDigging(event);

            if (dig.getAction() == DiggingAction.FINISHED_DIGGING) {
                WrappedBlockState block = player.compensatedWorld.getWrappedBlockStateAt(dig.getBlockPosition());
                // Not unbreakable
                if (block.getType().getHardness() != -1.0f) {
                    player.compensatedWorld.updateBlock(dig.getBlockPosition().getX(), dig.getBlockPosition().getY(), dig.getBlockPosition().getZ(), 0);
                }
            }

            if (dig.getAction() == DiggingAction.START_DIGGING) {
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
                        int digSpeed = tool.getEnchantmentLevel(EnchantmentTypes.BLOCK_EFFICIENCY, PacketEvents.getAPI().getServerManager().getVersion().toClientVersion());
                        if (digSpeed > 0) {
                            speedMultiplier += digSpeed * digSpeed + 1;
                        }
                    }
                }

                Integer digSpeed = player.compensatedEntities.getPotionLevelForPlayer(PotionTypes.HASTE);
                Integer conduit = player.compensatedEntities.getPotionLevelForPlayer(PotionTypes.CONDUIT_POWER);

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

                Integer miningFatigue = player.compensatedEntities.getPotionLevelForPlayer(PotionTypes.MINING_FATIGUE);

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

                if ((helmet != null && helmet.getEnchantmentLevel(EnchantmentTypes.AQUA_AFFINITY, PacketEvents.getAPI().getServerManager().getVersion().toClientVersion()) > 0) ||
                        (chestplate != null && chestplate.getEnchantmentLevel(EnchantmentTypes.AQUA_AFFINITY, PacketEvents.getAPI().getServerManager().getVersion().toClientVersion()) > 0) ||
                        (leggings != null && leggings.getEnchantmentLevel(EnchantmentTypes.AQUA_AFFINITY, PacketEvents.getAPI().getServerManager().getVersion().toClientVersion()) > 0) ||
                        (boots != null && boots.getEnchantmentLevel(EnchantmentTypes.AQUA_AFFINITY, PacketEvents.getAPI().getServerManager().getVersion().toClientVersion()) > 0)) {
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

        if (event.getPacketType() == PacketType.Play.Client.PLAYER_BLOCK_PLACEMENT) {
            WrapperPlayClientPlayerBlockPlacement packet = new WrapperPlayClientPlayerBlockPlacement(event);
            player.lastBlockPlaceUseItem = System.currentTimeMillis();

            ItemStack placedWith = player.getInventory().getHeldItem();
            if (packet.getHand() == InteractionHand.OFF_HAND) {
                placedWith = player.getInventory().getOffHand();
            }

            // This is the use item packet
            if (packet.getFace() == BlockFace.OTHER && PacketEvents.getAPI().getServerManager().getVersion().isOlderThan(ServerVersion.V_1_9)) {
                player.placeUseItemPackets.add(packet);
                PacketPlayerDigging.handleUseItem(player, player.getInventory().getHeldItem(), InteractionHand.MAIN_HAND);
                return;
            }

            // Anti-air place
            BlockPlace blockPlace = new BlockPlace(player, packet.getBlockPosition(), packet.getFace(), placedWith, getNearestHitResult(player, null, true));
            blockPlace.setCursor(packet.getCursorPosition());

            if (PacketEvents.getAPI().getServerManager().getVersion().isNewerThanOrEquals(ServerVersion.V_1_11) && player.getClientVersion().isOlderThan(ClientVersion.V_1_11)) {
                // ViaRewind is stupid and divides the byte by 15 to get the float
                // We must undo this to get the correct block place... why?
                if (packet.getCursorPosition().getX() * 15 % 1 == 0 && packet.getCursorPosition().getY() * 15 % 1 == 0 && packet.getCursorPosition().getZ() * 15 % 1 == 0) {
                    // This is impossible to occur without ViaRewind, fix their stupidity
                    int trueByteX = (int) (packet.getCursorPosition().getX() * 15);
                    int trueByteY = (int) (packet.getCursorPosition().getY() * 15);
                    int trueByteZ = (int) (packet.getCursorPosition().getZ() * 15);

                    blockPlace.setCursor(new Vector3f(trueByteX / 16f, trueByteY / 16f, trueByteZ / 16f));
                }
            }


            if (placedWith.getType().getPlacedType() != null || placedWith.getType() == ItemTypes.FIRE_CHARGE)
                player.checkManager.onBlockPlace(blockPlace);

            if (blockPlace.isCancelled() && !player.disableGrim) { // The player tried placing blocks in air/water
                event.setCancelled(true);

                Vector3i facePos = new Vector3i(packet.getBlockPosition().getX() + packet.getFace().getModX(), packet.getBlockPosition().getY() + packet.getFace().getModY(), packet.getBlockPosition().getZ() + packet.getFace().getModZ());
                int placed = player.compensatedWorld.getWrappedBlockStateAt(packet.getBlockPosition()).getGlobalId();
                int face = player.compensatedWorld.getWrappedBlockStateAt(facePos).getGlobalId();
                player.user.sendPacket(new WrapperPlayServerBlockChange(blockPlace.getPlacedBlockPos(), placed));
                player.user.sendPacket(new WrapperPlayServerBlockChange(facePos, face));

                // Stop inventory desync from cancelling place
                if (packet.getHand() == InteractionHand.MAIN_HAND) {
                    player.user.sendPacket(new WrapperPlayServerSetSlot(0, player.getInventory().stateID, 36 + player.packetStateData.lastSlotSelected, player.getInventory().getHeldItem()));
                } else {
                    player.user.sendPacket(new WrapperPlayServerSetSlot(0, player.getInventory().stateID, 45, player.getInventory().getOffHand()));
                }

            } else { // Legit place
                player.placeUseItemPackets.add(packet);
            }
        }

        if (event.getPacketType() == PacketType.Play.Client.USE_ITEM) {
            WrapperPlayClientUseItem packet = new WrapperPlayClientUseItem(event);
            player.placeUseItemPackets.add(packet);
            player.lastBlockPlaceUseItem = System.currentTimeMillis();
        }

        // Call the packet checks last as they can modify the contents of the packet
        // Such as the NoFall check setting the player to not be on the ground
        player.checkManager.onPacketReceive(event);
    }

    private static void placeBucket(GrimPlayer player, InteractionHand hand) {
        HitData data = getNearestHitResult(player, null, true);

        if (data != null) {
            BlockPlace blockPlace = new BlockPlace(player, data.getPosition(), data.getClosestDirection(), ItemStack.EMPTY, data);
            blockPlace.setReplaceClicked(true); // Replace the block clicked, not the block in the direction

            boolean placed = false;
            ItemType type = null;

            if (data.getState().getType() == StateTypes.POWDER_SNOW) {
                blockPlace.set(StateTypes.AIR);
                type = ItemTypes.POWDER_SNOW_BUCKET;
                placed = true;
            }

            if (data.getState().getType() == StateTypes.LAVA) {
                blockPlace.set(StateTypes.AIR);
                type = ItemTypes.LAVA_BUCKET;
                placed = true;
            }

            // We didn't hit fluid source
            if (!placed && !player.compensatedWorld.isWaterSourceBlock(data.getPosition().getX(), data.getPosition().getY(), data.getPosition().getZ()))
                return;

            if (!placed) {
                type = ItemTypes.WATER_BUCKET;
            }

            if (PacketEvents.getAPI().getServerManager().getVersion().isNewerThanOrEquals(ServerVersion.V_1_13)) {
                WrappedBlockState existing = blockPlace.getExistingBlockData();
                if (existing.getInternalData().containsKey(StateValue.WATERLOGGED)) { // waterloggable
                    existing.setWaterlogged(false);
                    blockPlace.set(existing);
                    placed = true;
                }
            }

            // Therefore, not waterlogged and is a fluid, and is therefore a source block
            if (!placed) {
                blockPlace.set(StateTypes.AIR);
            }

            // Give the player a water bucket
            if (player.gamemode != GameMode.CREATIVE) {
                if (hand == InteractionHand.MAIN_HAND) {
                    if (player.getInventory().getHeldItem().getAmount() == 1) {
                        player.getInventory().inventory.setHeldItem(ItemStack.builder().type(type).amount(1).build());
                    } else { // Give the player a water bucket
                        player.getInventory().inventory.add(ItemStack.builder().type(type).amount(1).build());
                        // and reduce the held item
                        player.getInventory().getHeldItem().setAmount(player.getInventory().getHeldItem().getAmount() - 1);
                    }
                } else {
                    if (player.getInventory().getOffHand().getAmount() == 1) {
                        player.getInventory().inventory.setPlayerInventoryItem(Inventory.SLOT_OFFHAND, ItemStack.builder().type(type).amount(1).build());
                    } else { // Give the player a water bucket
                        player.getInventory().inventory.add(Inventory.SLOT_OFFHAND, ItemStack.builder().type(type).amount(1).build());
                        // and reduce the held item
                        player.getInventory().getOffHand().setAmount(player.getInventory().getOffHand().getAmount() - 1);
                    }
                }
            }
        }
    }

    private void handleFlying(GrimPlayer player, double x, double y, double z, float yaw, float pitch, boolean hasPosition, boolean hasLook, boolean onGround, PacketReceiveEvent event) {
        long now = System.currentTimeMillis();

        player.packetStateData.lastPacketWasTeleport = false;
        TeleportAcceptData teleportData = null;
        if (hasPosition) {
            Vector3d position = VectorUtils.clampVector(new Vector3d(x, y, z));
            teleportData = player.getSetbackTeleportUtil().checkTeleportQueue(position.getX(), position.getY(), position.getZ());
            player.packetStateData.lastPacketWasTeleport = teleportData.isTeleport();
        }

        double threshold = player.getMovementThreshold();
        // Don't check duplicate 1.17 packets (Why would you do this mojang?)
        // Don't check rotation since it changes between these packets, with the second being irrelevant.
        //
        // removed a large rant, but I'm keeping this out of context insult below
        // EVEN A BUNCH OF MONKEYS ON A TYPEWRITER COULDNT WRITE WORSE NETCODE THAN MOJANG
        if (!player.packetStateData.lastPacketWasTeleport && hasPosition && hasLook &&
                // Ground status will never change in this stupidity packet
                ((onGround == player.packetStateData.packetPlayerOnGround
                        // Mojang added this stupid mechanic in 1.17
                        && (player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_17) &&
                        // Due to 0.03, we can't check exact position, only within 0.03
                        player.filterMojangStupidityOnMojangStupidity.distanceSquared(new Vector3d(x, y, z)) < threshold * threshold))
                        // If the player was in a vehicle, has position and look, and wasn't a teleport, then it was this stupid packet
                        || player.compensatedEntities.getSelf().inVehicle())) {
            player.packetStateData.lastPacketWasOnePointSeventeenDuplicate = true;

            if (player.xRot != yaw || player.yRot != pitch) {
                player.lastXRot = player.xRot;
                player.lastYRot = player.yRot;
            }

            // Take the pitch and yaw, just in case we were wrong about this being a stupidity packet
            player.xRot = yaw;
            player.yRot = pitch;

            player.packetStateData.lastClaimedPosition = new Vector3d(x, y, z);

            // Don't let players on 1.17+ clients on 1.8- servers FastHeal by right-clicking
            // the ground with a bucket... ViaVersion marked this as a WONTFIX, so I'll include the fix.
            if (PacketEvents.getAPI().getServerManager().getVersion().isOlderThanOrEquals(ServerVersion.V_1_8_8) &&
                    new Vector(player.x, player.y, player.z).equals(new Vector(x, y, z)) && !player.disableGrim) {
                event.setCancelled(true);
            }
            return;
        }

        // We can't set the look if this is actually the stupidity packet
        // If the last packet wasn't stupid, then ignore this logic
        // If it was stupid, only change the look if it's different
        // Otherwise, reach and fireworks can false
        if (hasLook && (!player.packetStateData.lastPacketWasOnePointSeventeenDuplicate ||
                player.xRot != yaw || player.yRot != pitch)) {
            player.lastXRot = player.xRot;
            player.lastYRot = player.yRot;
        }

        handleQueuedPlaces(player, hasLook, pitch, yaw, now);

        // We can set the new pos after the places
        if (hasPosition) {
            player.packetStateData.lastClaimedPosition = new Vector3d(x, y, z);
        }

        // This stupid mechanic has been measured with 0.03403409022229198 y velocity... DAMN IT MOJANG, use 0.06 to be safe...
        if (!hasPosition && onGround != player.packetStateData.packetPlayerOnGround && !player.compensatedEntities.getSelf().inVehicle()) {
            player.lastOnGround = onGround;
            player.clientClaimsLastOnGround = onGround;
            player.uncertaintyHandler.onGroundUncertain = true;

            // Ghost block/0.03 abuse
            // Check for blocks within 0.03 of the player's position before allowing ground to be true - if 0.03
            // Cannot use collisions like normal because stepping messes it up :(
            //
            // This may need to be secured better, but limiting the new setback positions seems good enough for now...
            boolean canFeasiblyPointThree = Collisions.slowCouldPointThreeHitGround(player, player.x, player.y, player.z);
            if ((!canFeasiblyPointThree && !player.compensatedWorld.isNearHardEntity(player.boundingBox.copy().expand(4))) || player.clientVelocity.getY() > 0.06) {
                player.getSetbackTeleportUtil().executeForceResync();
            }
        }

        player.lastX = player.x;
        player.lastY = player.y;
        player.lastZ = player.z;

        if (!player.packetStateData.lastPacketWasTeleport) {
            player.packetStateData.packetPlayerOnGround = onGround;
        }

        if (hasLook) {
            player.xRot = yaw;
            player.yRot = pitch;

            player.uncertaintyHandler.claimedLookChangedBetweenTick = !hasPosition;
        }

        if (hasPosition) {
            Vector3d position = new Vector3d(x, y, z);
            Vector3d clampVector = VectorUtils.clampVector(position);
            final PositionUpdate update = new PositionUpdate(new Vector3d(player.x, player.y, player.z), position, onGround, teleportData.getSetback(), teleportData.isTeleport());

            player.filterMojangStupidityOnMojangStupidity = clampVector;

            if (!player.compensatedEntities.getSelf().inVehicle()) {
                player.x = clampVector.getX();
                player.y = clampVector.getY();
                player.z = clampVector.getZ();

                player.checkManager.onPositionUpdate(update);
            } else if (update.isTeleport()) { // Mojang doesn't use their own exit vehicle field to leave vehicles, manually call the setback handler
                player.getSetbackTeleportUtil().onPredictionComplete(new PredictionComplete(0, update));
            }
        }

        if (hasLook) {
            float deltaXRot = player.xRot - player.lastXRot;
            float deltaYRot = player.yRot - player.lastYRot;

            final RotationUpdate update = new RotationUpdate(new HeadRotation(player.lastXRot, player.lastYRot), new HeadRotation(player.xRot, player.yRot), deltaXRot, deltaYRot);
            player.checkManager.onRotationUpdate(update);
        }

        player.packetStateData.lastPacketWasOnePointSeventeenDuplicate = false;

        player.packetStateData.didLastLastMovementIncludePosition = player.packetStateData.didLastMovementIncludePosition;
        player.packetStateData.didLastMovementIncludePosition = hasPosition;
    }

    private static void placeLilypad(GrimPlayer player, InteractionHand hand) {
        HitData data = getNearestHitResult(player, null, true);

        if (data != null) {
            // A lilypad cannot replace a fluid
            if (player.compensatedWorld.getFluidLevelAt(data.getPosition().getX(), data.getPosition().getY() + 1, data.getPosition().getZ()) > 0)
                return;

            BlockPlace blockPlace = new BlockPlace(player, data.getPosition(), data.getClosestDirection(), ItemStack.EMPTY, data);
            blockPlace.setReplaceClicked(false); // Not possible with use item

            // We checked for a full fluid block below here.
            if (player.compensatedWorld.getWaterFluidLevelAt(data.getPosition().getX(), data.getPosition().getY(), data.getPosition().getZ()) > 0
                    || data.getState().getType() == StateTypes.ICE || data.getState().getType() == StateTypes.FROSTED_ICE) {
                Vector3i pos = data.getPosition();
                pos = pos.add(0, 1, 0);

                blockPlace.set(pos, StateTypes.LILY_PAD.createBlockState(CompensatedWorld.blockVersion));

                if (player.gamemode != GameMode.CREATIVE) {
                    if (hand == InteractionHand.MAIN_HAND) {
                        player.getInventory().inventory.getHeldItem().setAmount(player.getInventory().inventory.getHeldItem().getAmount() - 1);
                    } else {
                        player.getInventory().getOffHand().setAmount(player.getInventory().getOffHand().getAmount() - 1);
                    }
                }
            }
        }
    }

    private static HitData getNearestHitResult(GrimPlayer player, StateType heldItem, boolean sourcesHaveHitbox) {
        Vector3d startingPos = new Vector3d(player.x, player.y + player.getEyeHeight(), player.z);
        Vector startingVec = new Vector(startingPos.getX(), startingPos.getY(), startingPos.getZ());
        Ray trace = new Ray(player, startingPos.getX(), startingPos.getY(), startingPos.getZ(), player.xRot, player.yRot);
        Vector endVec = trace.getPointAtDistance(5);
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
        GrimPlayer player = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(event.getUser());
        if (player == null) return;

        player.checkManager.onPacketSend(event);
    }
}
