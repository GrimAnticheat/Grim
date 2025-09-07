package ac.grim.grimac.events.packets;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.update.BlockBreak;
import ac.grim.grimac.utils.anticheat.update.BlockPlace;
import ac.grim.grimac.utils.blockplace.BlockPlaceResult;
import ac.grim.grimac.utils.blockplace.ConsumesBlockPlace;
import ac.grim.grimac.utils.data.BlockPlaceSnapshot;
import ac.grim.grimac.utils.data.HitData;
import ac.grim.grimac.utils.inventory.Inventory;
import ac.grim.grimac.utils.latency.CompensatedWorld;
import ac.grim.grimac.utils.nmsutil.BoundingBoxSize;
import ac.grim.grimac.utils.nmsutil.Materials;
import ac.grim.grimac.utils.nmsutil.WorldRayTrace;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.ConnectionState;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.item.type.ItemType;
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.player.GameMode;
import com.github.retrooper.packetevents.protocol.player.InteractionHand;
import com.github.retrooper.packetevents.protocol.world.BlockFace;
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import com.github.retrooper.packetevents.protocol.world.states.defaulttags.BlockTags;
import com.github.retrooper.packetevents.protocol.world.states.type.StateType;
import com.github.retrooper.packetevents.protocol.world.states.type.StateTypes;
import com.github.retrooper.packetevents.protocol.world.states.type.StateValue;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.util.Vector3f;
import com.github.retrooper.packetevents.util.Vector3i;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerBlockPlacement;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientUseItem;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerAcknowledgeBlockChanges;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSetSlot;

public class CheckManagerListener extends PacketListenerAbstract {

    public CheckManagerListener() {
        super(PacketListenerPriority.LOW);
    }

    private static void placeWaterLavaSnowBucket(GrimPlayer player, ItemStack held, StateType toPlace, InteractionHand hand, int sequence) {
        HitData data = WorldRayTrace.getNearestBlockHitResult(player, StateTypes.AIR, false, true, true);
        if (data != null) {
            BlockPlace blockPlace = new BlockPlace(player, hand, data.position(), data.closestDirection().getFaceValue(), data.closestDirection(), held, data, sequence);

            boolean didPlace = false;

            // Powder snow, lava, and water all behave like placing normal blocks after checking for waterlogging (replace clicked always false though)
            // If we hit a waterloggable block, then the bucket is directly placed
            // Otherwise, use the face to determine where to place the bucket
            if (Materials.isPlaceableWaterBucket(blockPlace.itemStack.getType()) && PacketEvents.getAPI().getServerManager().getVersion().isNewerThanOrEquals(ServerVersion.V_1_13)) {
                blockPlace.replaceClicked = true; // See what's in the existing place
                WrappedBlockState existing = blockPlace.getExistingBlockData();
                if (!(boolean) existing.getInternalData().getOrDefault(StateValue.WATERLOGGED, true)) {
                    // Strangely, the client does not predict waterlogged placements
                    didPlace = true;
                }
            }

            if (!didPlace) {
                // Powder snow, lava, and water all behave like placing normal blocks after checking for waterlogging (replace clicked always false though)
                blockPlace.replaceClicked = false;
                blockPlace.set(toPlace);
            }

            if (player.gamemode != GameMode.CREATIVE) {
                player.inventory.markSlotAsResyncing(blockPlace);
                if (hand == InteractionHand.MAIN_HAND) {
                    player.inventory.inventory.setHeldItem(ItemStack.builder().type(ItemTypes.BUCKET).amount(1).build());
                } else {
                    player.inventory.inventory.setPlayerInventoryItem(Inventory.SLOT_OFFHAND, ItemStack.builder().type(ItemTypes.BUCKET).amount(1).build());
                }
            }
        }
    }

    public static void handleQueuedPlaces(GrimPlayer player, boolean hasLook, float pitch, float yaw, long now) {
        // Handle queue'd block places
        BlockPlaceSnapshot snapshot;
        while ((snapshot = player.placeUseItemPackets.poll()) != null) {
            double lastX = player.x;
            double lastY = player.y;
            double lastZ = player.z;

            player.x = player.packetStateData.lastClaimedPosition.getX();
            player.y = player.packetStateData.lastClaimedPosition.getY();
            player.z = player.packetStateData.lastClaimedPosition.getZ();

            boolean lastSneaking = player.isSneaking;
            player.isSneaking = snapshot.isSneaking();

            if (player.inVehicle()) {
                Vector3d posFromVehicle = BoundingBoxSize.getRidingOffsetFromVehicle(player.compensatedEntities.self.getRiding(), player);
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
            }

            player.compensatedWorld.startPredicting();
            handleBlockPlaceOrUseItem(snapshot.getWrapper(), player);
            player.compensatedWorld.stopPredicting(snapshot.getWrapper());

            player.x = lastX;
            player.y = lastY;
            player.z = lastZ;
            player.isSneaking = lastSneaking;
        }
    }

    public static void handleQueuedBreaks(GrimPlayer player, boolean hasLook, float pitch, float yaw, long now) {
        BlockBreak blockBreak;
        while ((blockBreak = player.queuedBreaks.poll()) != null) {
            double lastX = player.x;
            double lastY = player.y;
            double lastZ = player.z;

            player.x = player.packetStateData.lastClaimedPosition.getX();
            player.y = player.packetStateData.lastClaimedPosition.getY();
            player.z = player.packetStateData.lastClaimedPosition.getZ();

            if (player.inVehicle()) {
                Vector3d posFromVehicle = BoundingBoxSize.getRidingOffsetFromVehicle(player.compensatedEntities.self.getRiding(), player);
                player.x = posFromVehicle.getX();
                player.y = posFromVehicle.getY();
                player.z = posFromVehicle.getZ();
            }

            // Less than 15 milliseconds ago means this is likely (fix all look vectors being a tick behind server sided)
            // Or mojang had the idle packet... for the 1.7/1.8 clients
            // No idle packet on 1.9+
            if ((now - player.lastBlockBreak < 15 || player.getClientVersion().isOlderThan(ClientVersion.V_1_9)) && hasLook) {
                player.xRot = yaw;
                player.yRot = pitch;
            }

            player.checkManager.onPostFlyingBlockBreak(blockBreak);

            player.x = lastX;
            player.y = lastY;
            player.z = lastZ;
        }
    }

    private static void handleUseItem(GrimPlayer player, ItemStack placedWith, InteractionHand hand, int sequence) {
        // Lilypads are USE_ITEM (THIS CAN DESYNC, WTF MOJANG)
        if (placedWith.getType() == ItemTypes.LILY_PAD) {
            placeLilypad(player, hand, sequence); // Pass a block place because lily pads have a hitbox
            return;
        }

        StateType toBucketMat = Materials.transformBucketMaterial(placedWith.getType());
        if (toBucketMat != null) {
            placeWaterLavaSnowBucket(player, placedWith, toBucketMat, hand, sequence);
        }

        if (placedWith.getType() == ItemTypes.BUCKET) {
            placeBucket(player, hand, sequence);
        }
    }

    private static void handleBlockPlaceOrUseItem(PacketWrapper<?> packet, GrimPlayer player) {
        // Legacy "use item" packet
        if (packet instanceof WrapperPlayClientPlayerBlockPlacement place &&
                PacketEvents.getAPI().getServerManager().getVersion().isOlderThan(ServerVersion.V_1_9)) {

            if (player.gamemode == GameMode.SPECTATOR || player.gamemode == GameMode.ADVENTURE)
                return;

            if (place.getFace() == BlockFace.OTHER) {
                ItemStack placedWith = player.inventory.getHeldItem();
                if (place.getHand() == InteractionHand.OFF_HAND) {
                    placedWith = player.inventory.getOffHand();
                }

                handleUseItem(player, placedWith, place.getHand(), place.getSequence());
                return;
            }
        }

        if (packet instanceof WrapperPlayClientUseItem place) {
            if (player.gamemode == GameMode.SPECTATOR || player.gamemode == GameMode.ADVENTURE)
                return;

            ItemStack placedWith = player.inventory.getHeldItem();
            if (place.getHand() == InteractionHand.OFF_HAND) {
                placedWith = player.inventory.getOffHand();
            }

            handleUseItem(player, placedWith, place.getHand(), place.getSequence());
        }

        // Check for interactable first (door, etc)
        if (packet instanceof WrapperPlayClientPlayerBlockPlacement place) {
            ItemStack placedWith = player.inventory.getHeldItem();
            ItemStack offhand = player.inventory.getOffHand();

            boolean onlyAir = placedWith.isEmpty() && offhand.isEmpty();

            // The offhand is unable to interact with blocks like this... try to stop some desync points before they happen
            if ((!player.isSneaking || onlyAir) && place.getHand() == InteractionHand.MAIN_HAND) {
                Vector3i blockPosition = place.getBlockPosition();
                BlockPlace blockPlace = new BlockPlace(player, place.getHand(), blockPosition, place.getFaceId(), place.getFace(), placedWith, WorldRayTrace.getNearestBlockHitResult(player, null, true, false, false), place.getSequence());

                // Right-clicking a trapdoor/door/etc.
                StateType placedAgainst = blockPlace.getPlacedAgainstMaterial();
                if (player.getClientVersion().isOlderThan(ClientVersion.V_1_11) && (placedAgainst == StateTypes.IRON_TRAPDOOR
                        || placedAgainst == StateTypes.IRON_DOOR || BlockTags.FENCES.contains(placedAgainst))
                        || player.getClientVersion().isOlderThanOrEquals(ClientVersion.V_1_8) && BlockTags.CAULDRONS.contains(placedAgainst)
                        || Materials.isClientSideInteractable(placedAgainst)) {
                    player.checkManager.onPostFlyingBlockPlace(blockPlace);
                    Vector3i location = blockPlace.position;
                    player.compensatedWorld.tickOpenable(location.x, location.y, location.z);
                    return;
                }

                // This also has side effects
                // This method is for when the block doesn't always consume the click
                // This causes a ton of desync's but mojang doesn't seem to care...
                if (ConsumesBlockPlace.consumesPlace(player, player.compensatedWorld.getBlock(blockPlace.position), blockPlace)) {
                    player.checkManager.onPostFlyingBlockPlace(blockPlace);
                    return;
                }
            }
        }

        if (packet instanceof WrapperPlayClientPlayerBlockPlacement place) {
            if (player.gamemode == GameMode.SPECTATOR || player.gamemode == GameMode.ADVENTURE)
                return;

            Vector3i blockPosition = place.getBlockPosition();
            BlockFace face = place.getFace();
            ItemStack placedWith = player.inventory.getHeldItem();
            if (place.getHand() == InteractionHand.OFF_HAND) {
                placedWith = player.inventory.getOffHand();
            }

            BlockPlace blockPlace = new BlockPlace(player, place.getHand(), blockPosition, place.getFaceId(), face, placedWith, WorldRayTrace.getNearestBlockHitResult(player, null, true, false, false), place.getSequence());
            // At this point, it is too late to cancel, so we can only flag, and cancel subsequent block places more aggressively
            player.checkManager.onPostFlyingBlockPlace(blockPlace);

            blockPlace.isInside = place.getInsideBlock().orElse(false);

            if (placedWith.getType().getPlacedType() != null || placedWith.getType() == ItemTypes.FLINT_AND_STEEL || placedWith.getType() == ItemTypes.FIRE_CHARGE) {
                BlockPlaceResult.getMaterialData(placedWith.getType()).applyBlockPlaceToWorld(player, blockPlace);
            }
        }
    }

    private static void placeBucket(GrimPlayer player, InteractionHand hand, int sequence) {
        HitData data = WorldRayTrace.getNearestBlockHitResult(player, null, true, false, true);

        if (data != null) {
            BlockPlace blockPlace = new BlockPlace(player, hand, data.position(), data.closestDirection().getFaceValue(), data.closestDirection(), ItemStack.EMPTY, data, sequence);
            blockPlace.replaceClicked = true; // Replace the block clicked, not the block in the direction

            boolean placed = false;
            ItemType type = null;

            if (data.state().getType() == StateTypes.POWDER_SNOW) {
                blockPlace.set(StateTypes.AIR);
                type = ItemTypes.POWDER_SNOW_BUCKET;
                placed = true;
            }

            if (data.state().getType() == StateTypes.LAVA) {
                blockPlace.set(StateTypes.AIR);
                type = ItemTypes.LAVA_BUCKET;
                placed = true;
            }

            // We didn't hit fluid source
            if (!placed && !player.compensatedWorld.isWaterSourceBlock(data.position().getX(), data.position().getY(), data.position().getZ()))
                return;

            // We can't replace plants with a water bucket
            if (data.state().getType() == StateTypes.KELP || data.state().getType() == StateTypes.SEAGRASS || data.state().getType() == StateTypes.TALL_SEAGRASS) {
                return;
            }

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

            if (player.gamemode != GameMode.CREATIVE) {
                player.inventory.markSlotAsResyncing(blockPlace);
                setPlayerItem(player, hand, type);
            }
        }
    }

    public static void setPlayerItem(GrimPlayer player, InteractionHand hand, ItemType type) {
        // Give the player a water bucket
        if (player.gamemode != GameMode.CREATIVE) {
            if (hand == InteractionHand.MAIN_HAND) {
                if (player.inventory.getHeldItem().getAmount() == 1) {
                    player.inventory.inventory.setHeldItem(ItemStack.builder().type(type).amount(1).build());
                } else { // Give the player a water bucket
                    player.inventory.inventory.add(ItemStack.builder().type(type).amount(1).build());
                    // and reduce the held item
                    player.inventory.getHeldItem().setAmount(player.inventory.getHeldItem().getAmount() - 1);
                }
            } else {
                if (player.inventory.getOffHand().getAmount() == 1) {
                    player.inventory.inventory.setPlayerInventoryItem(Inventory.SLOT_OFFHAND, ItemStack.builder().type(type).amount(1).build());
                } else { // Give the player a water bucket
                    player.inventory.inventory.add(Inventory.SLOT_OFFHAND, ItemStack.builder().type(type).amount(1).build());
                    // and reduce the held item
                    player.inventory.getOffHand().setAmount(player.inventory.getOffHand().getAmount() - 1);
                }
            }
        }
    }

    private static void placeLilypad(GrimPlayer player, InteractionHand hand, int sequence) {
        HitData data = WorldRayTrace.getNearestBlockHitResult(player, null, true, false, true);

        if (data != null) {
            // A lilypad cannot replace a fluid
            if (player.compensatedWorld.getFluidLevelAt(data.position().getX(), data.position().getY() + 1, data.position().getZ()) > 0)
                return;

            BlockPlace blockPlace = new BlockPlace(player, hand, data.position(), data.closestDirection().getFaceValue(), data.closestDirection(), ItemStack.EMPTY, data, sequence);
            blockPlace.replaceClicked = false; // Not possible with use item

            // We checked for a full fluid block below here.
            if (player.compensatedWorld.getWaterFluidLevelAt(data.position().getX(), data.position().getY(), data.position().getZ()) > 0
                    || data.state().getType() == StateTypes.ICE || data.state().getType() == StateTypes.FROSTED_ICE) {
                Vector3i pos = data.position();
                pos = pos.add(0, 1, 0);

                blockPlace.set(pos, StateTypes.LILY_PAD.createBlockState(CompensatedWorld.blockVersion));

                if (player.gamemode != GameMode.CREATIVE) {
                    player.inventory.markSlotAsResyncing(blockPlace);
                    if (hand == InteractionHand.MAIN_HAND) {
                        player.inventory.inventory.getHeldItem().setAmount(player.inventory.inventory.getHeldItem().getAmount() - 1);
                    } else {
                        player.inventory.getOffHand().setAmount(player.inventory.getOffHand().getAmount() - 1);
                    }
                }
            }
        }
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        GrimPlayer player = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(event.getUser());
        if (player == null) return;

        if (event.getConnectionState() != ConnectionState.PLAY) {
            // Allow checks to listen to configuration packets
            if (event.getConnectionState() != ConnectionState.CONFIGURATION) return;
            player.checkManager.onPacketReceive(event);
            return;
        }

        if (event.getPacketType() == PacketType.Play.Client.PLAYER_BLOCK_PLACEMENT) {
            WrapperPlayClientPlayerBlockPlacement packet = new WrapperPlayClientPlayerBlockPlacement(event);
            player.lastBlockPlaceUseItem = System.currentTimeMillis();

            ItemStack placedWith = player.inventory.getHeldItem();
            if (packet.getHand() == InteractionHand.OFF_HAND) {
                placedWith = player.inventory.getOffHand();
            }

            // This is the use item packet
            if (packet.getFace() == BlockFace.OTHER && PacketEvents.getAPI().getServerManager().getVersion().isOlderThan(ServerVersion.V_1_9)) {
                player.placeUseItemPackets.add(new BlockPlaceSnapshot(packet, player.isSneaking));
            } else {
                // Anti-air place
                BlockPlace blockPlace = new BlockPlace(player, packet.getHand(), packet.getBlockPosition(), packet.getFaceId(), packet.getFace(), placedWith, WorldRayTrace.getNearestBlockHitResult(player, null, true, false, false), packet.getSequence());
                blockPlace.cursor = packet.getCursorPosition();

                if (PacketEvents.getAPI().getServerManager().getVersion().isNewerThanOrEquals(ServerVersion.V_1_11) && player.getClientVersion().isOlderThan(ClientVersion.V_1_11)) {
                    // ViaRewind is stupid and divides the byte by 15 to get the float
                    // We must undo this to get the correct block place... why?
                    if (packet.getCursorPosition().getX() * 15 % 1 == 0 && packet.getCursorPosition().getY() * 15 % 1 == 0 && packet.getCursorPosition().getZ() * 15 % 1 == 0) {
                        // This is impossible to occur without ViaRewind, fix their stupidity
                        int trueByteX = (int) (packet.getCursorPosition().getX() * 15);
                        int trueByteY = (int) (packet.getCursorPosition().getY() * 15);
                        int trueByteZ = (int) (packet.getCursorPosition().getZ() * 15);

                        blockPlace.cursor = new Vector3f(trueByteX / 16f, trueByteY / 16f, trueByteZ / 16f);
                    }
                }

                player.checkManager.onBlockPlace(blockPlace);

                if (event.isCancelled() || blockPlace.isCancelled() || player.getSetbackTeleportUtil().shouldBlockMovement()) { // The player tried placing blocks in air/water

                    if (!event.isCancelled()) {
                        event.setCancelled(true);
                        player.onPacketCancel();
                    }

                    Vector3i facePos = new Vector3i(packet.getBlockPosition().getX() + packet.getFace().getModX(), packet.getBlockPosition().getY() + packet.getFace().getModY(), packet.getBlockPosition().getZ() + packet.getFace().getModZ());

                    // Ends the client prediction introduced in 1.19+
                    if (player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_19) && PacketEvents.getAPI().getServerManager().getVersion().isNewerThanOrEquals(ServerVersion.V_1_19)) {
                        player.user.sendPacket(new WrapperPlayServerAcknowledgeBlockChanges(packet.getSequence()));
                    } else { // The client isn't smart enough to revert changes
                        player.resyncPosition(packet.getBlockPosition());
                        player.resyncPosition(facePos);
                    }

                    // Stop inventory desync from cancelling place
                    if (player.platformPlayer != null) {
                        if (packet.getHand() == InteractionHand.MAIN_HAND) {
                            ItemStack mainHand = player.platformPlayer.getInventory().getItemInHand();
                            player.user.sendPacket(new WrapperPlayServerSetSlot(0, player.inventory.stateID, 36 + player.packetStateData.lastSlotSelected, mainHand));
                        } else {
                            ItemStack offHand = player.platformPlayer.getInventory().getItemInOffHand();
                            player.user.sendPacket(new WrapperPlayServerSetSlot(0, player.inventory.stateID, 45, offHand));
                        }
                    }

                } else { // Legit place
                    player.placeUseItemPackets.add(new BlockPlaceSnapshot(packet, player.isSneaking));
                }
            }
        }

        if (event.getPacketType() == PacketType.Play.Client.USE_ITEM) {
            WrapperPlayClientUseItem packet = new WrapperPlayClientUseItem(event);
            player.placeUseItemPackets.add(new BlockPlaceSnapshot(packet, player.isSneaking));
            player.lastBlockPlaceUseItem = System.currentTimeMillis();
        }

        // Call the packet checks last as they can modify the contents of the packet
        // Such as the NoFall check setting the player to not be on the ground
        player.checkManager.onPacketReceive(event);

        if (player.packetStateData.cancelDuplicatePacket) {
            event.setCancelled(true);
            player.packetStateData.cancelDuplicatePacket = false;
        }

        // Finally, remove the packet state variables on this packet
        player.packetStateData.lastPacketWasOnePointSeventeenDuplicate = false;
        player.packetStateData.lastPacketWasTeleport = false;
    }

    @Override
    public void onPacketSend(PacketSendEvent event) {
        if (event.getConnectionState() != ConnectionState.PLAY) return;
        GrimPlayer player = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(event.getUser());
        if (player == null) return;

        if (event.getPacketType() == PacketType.Play.Server.OPEN_WINDOW) {
            player.latencyUtils.addRealTimeTask(player.lastTransactionSent.get(), () -> player.serverOpenedInventoryThisTick = true);
        }

        player.checkManager.onPacketSend(event);
    }
}
