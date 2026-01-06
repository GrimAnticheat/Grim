package ac.grim.grimac.events.packets;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.collisions.blocks.MovingPistonCollision;
import ac.grim.grimac.utils.collisions.blocks.entity.PistonBlockEntity;
import ac.grim.grimac.utils.collisions.blocks.entity.PistonHandler;
import ac.grim.grimac.utils.data.ShulkerData;
import ac.grim.grimac.utils.nmsutil.Materials;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.world.BlockFace;
import com.github.retrooper.packetevents.protocol.world.Direction;
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import com.github.retrooper.packetevents.protocol.world.states.enums.Type;
import com.github.retrooper.packetevents.protocol.world.states.type.StateType;
import com.github.retrooper.packetevents.protocol.world.states.type.StateTypes;
import com.github.retrooper.packetevents.util.Vector3i;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerBlockAction;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import java.util.List;
import java.util.Map;

// If a player doesn't get this packet, then they don't know the shulker box is currently opened
// Meaning if a player enters a chunk with an opened shulker box, they see the shulker box as closed.
//
// Exempting the player on shulker boxes is an option... but then you have people creating PvP arenas
// on shulker boxes to get high lenience.
//
public class PacketBlockAction extends PacketListenerAbstract {
    public PacketBlockAction() {
        super(PacketListenerPriority.HIGH);
    }

    @Override
    public void onPacketSend(PacketSendEvent event) {
        if (event.getPacketType() == PacketType.Play.Server.BLOCK_ACTION) {
            GrimPlayer player = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(event.getUser());
            if (player == null) return;

            WrapperPlayServerBlockAction blockAction = new WrapperPlayServerBlockAction(event);
            Vector3i blockPos = blockAction.getBlockPosition();

            int transactionId = player.lastTransactionSent.get();
            player.latencyUtils.addRealTimeTask(transactionId, () -> {
                // The client ignores the state sent to the client.
                WrappedBlockState existing = player.compensatedWorld.getBlock(blockPos);

                // --- PISTON LOGIC ---
                StateType type = existing.getType();
                if (type == StateTypes.PISTON || type == StateTypes.STICKY_PISTON) {
                    int action = blockAction.getActionId(); // 0 = Extend, 1 = Retract
                    int param = blockAction.getActionData(); // Direction Index


                    // Piston Param maps directly to Direction Index
                    BlockFace pistonFacing = BlockFace.getBlockFaceByValue(param);
                    Type pistonType = type == StateTypes.STICKY_PISTON ? Type.STICKY : Type.NORMAL;

                    if (action == 0) {
                        if (!move(player, pistonType, blockPos, pistonFacing, true, transactionId)) {
                            return;
                        }

                        // TODO make getting existing not get a clone but get a reference to the original so no new object is allocated?
                        WrappedBlockState newBlockState = existing.clone(); // not necessary after debug removed
                        newBlockState.setExtended(true);
                        System.out.println(blockPos.x + " " + blockPos.y + " " + blockPos.z + " " + existing + " → " + newBlockState + " @ " + transactionId);
                        player.compensatedWorld.updateBlock(blockPos.x, blockPos.y, blockPos.z, newBlockState.getGlobalId());

                    } else if (action == 1 || action == 2) {
                        // --- RETRACTING ---
                        Vector3i offsetLocation = blockPos.offset(pistonFacing);
                        PistonBlockEntity blockEntity = player.compensatedWorld.getBlockEntity(offsetLocation.getX(), offsetLocation.getY(), offsetLocation.getZ());
                        if (blockEntity != null) {
                            blockEntity.finish();
                        }

                        WrappedBlockState blockState2 = WrappedBlockState.getDefaultState(StateTypes.MOVING_PISTON);
                        blockState2.setFacing(pistonFacing);
                        blockState2.setTypeData(pistonType);
                        player.compensatedWorld.updateBlock(blockPos, blockState2);

                        WrappedBlockState state = WrappedBlockState.getDefaultState(StateTypes.PISTON);
                        state.setFacing(BlockFace.getBlockFaceByValue(param & 7));


                        player.compensatedWorld.addBlockEntity(
                                new PistonBlockEntity(player, blockPos, blockState2, state, pistonFacing, false, true)
                        );
//                        world.updateNeighbors(pos, blockState2.getBlock());
//                        blockState2.updateNeighbors(world, pos, Block.NOTIFY_LISTENERS);

                        if (pistonType == Type.STICKY) {
//                            BlockPos blockPos = pos.add(direction.getOffsetX() * 2, direction.getOffsetY() * 2, direction.getOffsetZ() * 2);
//                            BlockState blockState3 = world.getBlockState(blockPos);
//                            boolean bl2 = false;
//                            if (blockState3.isOf(Blocks.MOVING_PISTON)
//                                    && world.getBlockEntity(blockPos) instanceof PistonBlockEntity pistonBlockEntity
//                                    && pistonBlockEntity.getFacing() == direction
//                                    && pistonBlockEntity.isExtending()) {
//                                pistonBlockEntity.finish();
//                                bl2 = true;
//                            }
//
//                            if (!bl2) {
//                                if (type != 1
//                                        || blockState3.isAir()
//                                        || !isMovable(blockState3, world, blockPos, direction.getOpposite(), false, direction)
//                                        || blockState3.getPistonBehavior() != PistonBehavior.NORMAL && !blockState3.isOf(Blocks.PISTON) && !blockState3.isOf(Blocks.STICKY_PISTON)) {
//                                    world.removeBlock(pos.offset(direction), false);
//                                } else {
//                                    this.move(world, pos, direction, false);
//                                }
//                            }
                        } else {
                            // In vanilla this sets to fluid state, we can handle that later for now just make air as that's what ts without fluids
                            player.compensatedWorld.updateBlock(blockPos.x,  blockPos.y, blockPos.z, 0);
                        }

//                        world.playSound(null, pos, SoundEvents.BLOCK_PISTON_CONTRACT, SoundCategory.BLOCKS, 0.5F, world.random.nextFloat() * 0.15F + 0.6F);
//                        world.emitGameEvent(GameEvent.BLOCK_DEACTIVATE, pos, GameEvent.Emitter.of(blockState2));H
                    }
                }

                if (Materials.isShulker(existing.getType())) {
                    // Param is the number of viewers of the shulker box.
                    // Hashset with .equals() set to be position
                    if (blockAction.getActionData() >= 1) {
                        ShulkerData data = new ShulkerData(blockPos, player.lastTransactionSent.get(), false);
                        player.compensatedWorld.openShulkerBoxes.remove(data);
                        player.compensatedWorld.openShulkerBoxes.add(data);
                    } else {
                        // The shulker box is closing
                        ShulkerData data = new ShulkerData(blockPos, player.lastTransactionSent.get(), true);
                        player.compensatedWorld.openShulkerBoxes.remove(data);
                        player.compensatedWorld.openShulkerBoxes.add(data);
                    }
                }
            });
        }
    }

    private boolean move(GrimPlayer player, Type pistonType, Vector3i pos, BlockFace facingDirection, boolean extend, int transactionId) {
        Vector3i blockPos = pos.offset(facingDirection);
        if (!extend && player.compensatedWorld.getBlock(blockPos).getType() == StateTypes.PISTON_HEAD) {
            player.compensatedWorld.updateBlock(blockPos.x, blockPos.y, blockPos.z, 0);
        }

        PistonHandler pistonHandler = new PistonHandler(player, pos, facingDirection, extend);
        if (!pistonHandler.calculatePush()) {
            return false;
        } else {
            Map<Vector3i, WrappedBlockState> map = Maps.newHashMap();
            List<Vector3i> list = pistonHandler.getMovedBlocks();
            List<WrappedBlockState> list2 = Lists.newArrayList();

            for (Vector3i blockPos2 : list) {
                WrappedBlockState blockState = player.compensatedWorld.getBlock(blockPos2);
                list2.add(blockState);
                map.put(blockPos2, blockState);
            }

            List<Vector3i> list3 = pistonHandler.getBrokenBlocks();
            WrappedBlockState[] blockStates = new WrappedBlockState[list.size() + list3.size()];
            BlockFace direction = extend ? facingDirection : facingDirection.getOppositeFace();
            int i = 0;

//            for (int j = list3.size() - 1; j >= 0; j--) {
//                BlockPos blockPos3 = (BlockPos)list3.get(j);
//                BlockState blockState2 = world.getBlockState(blockPos3);
//                BlockEntity blockEntity = blockState2.hasBlockEntity() ? world.getBlockEntity(blockPos3) : null;
//                dropStacks(blockState2, world, blockPos3, blockEntity);
//                if (!blockState2.isIn(BlockTags.FIRE) && world.isClient()) {
//                    world.syncWorldEvent(WorldEvents.BLOCK_BROKEN, blockPos3, getRawIdFromState(blockState2));
//                }
//
//                world.setBlockState(blockPos3, Blocks.AIR.getDefaultState(), Block.NOTIFY_LISTENERS | Block.FORCE_STATE);
//                world.emitGameEvent(GameEvent.BLOCK_DESTROY, blockPos3, GameEvent.Emitter.of(blockState2));
//                blockStates[i++] = blockState2;
//            }

            for (int j = list.size() - 1; j >= 0; j--) {
                Vector3i blockPos3 = list.get(j);
                WrappedBlockState blockState2 = player.compensatedWorld.getBlock(blockPos3);
                blockPos3 = blockPos3.offset(direction);
                map.remove(blockPos3);

                WrappedBlockState blockState3 = WrappedBlockState.getDefaultState(StateTypes.MOVING_PISTON);
                blockState3.setFacing(direction);

                player.compensatedWorld.updateBlock(blockPos3, blockState3);
                player.compensatedWorld.addBlockEntity(new PistonBlockEntity(player, blockPos3, blockState3, list2.get(j), facingDirection, extend, false));
                blockStates[i++] = blockState2;
            }

            if (extend) {
                WrappedBlockState blockState4 = WrappedBlockState.getDefaultState(StateTypes.PISTON_HEAD);
                blockState4.setFacing(facingDirection);
                blockState4.setTypeData(pistonType);

                WrappedBlockState blockState2 =  WrappedBlockState.getDefaultState(StateTypes.MOVING_PISTON);
                blockState2.setFacing(facingDirection);
                blockState2.setTypeData(pistonType);

                map.remove(blockPos);
                System.out.println(blockPos.x + " " + blockPos.y + " " + blockPos.z + " " + player.compensatedWorld.getBlock(blockPos).toString() + " → " + blockState2 + " @ " + transactionId);
                player.compensatedWorld.updateBlock(blockPos, blockState2);
                player.compensatedWorld.addBlockEntity(new PistonBlockEntity(
                        player,
                        blockPos,
                        blockState2,
                        blockState4,
                        facingDirection,
                        true,
                        true
                ));
            }

            WrappedBlockState blockState5 = WrappedBlockState.getByGlobalId(0);

//            for (BlockPos blockPos4 : map.keySet()) {
//                world.setBlockState(blockPos4, blockState5, Block.MOVED | Block.NOTIFY_LISTENERS | Block.FORCE_STATE);
//            }

//            for (Map.Entry<BlockPos, BlockState> entry : map.entrySet()) {
//                BlockPos blockPos5 = (BlockPos)entry.getKey();
//                BlockState blockState6 = (BlockState)entry.getValue();
//                blockState6.prepare(world, blockPos5, Block.NOTIFY_LISTENERS);
//                blockState5.updateNeighbors(world, blockPos5, Block.NOTIFY_LISTENERS);
//                blockState5.prepare(world, blockPos5, Block.NOTIFY_LISTENERS);
//            }

//            WireOrientation wireOrientation = OrientationHelper.getEmissionOrientation(world, pistonHandler.getMotionDirection(), null);
//            i = 0;

//            for (int k = list3.size() - 1; k >= 0; k--) {
//                BlockState blockState3 = blockStates[i++];
//                BlockPos blockPos6 = (BlockPos)list3.get(k);
//                if (world instanceof ServerWorld serverWorld) {
//                    blockState3.onStateReplaced(serverWorld, blockPos6, false);
//                }
//
//                blockState3.prepare(world, blockPos6, Block.NOTIFY_LISTENERS);
//                world.updateNeighborsAlways(blockPos6, blockState3.getBlock(), wireOrientation);
//            }

//            for (int k = list.size() - 1; k >= 0; k--) {
//                world.updateNeighborsAlways(list.get(k), blockStates[i++].getBlock(), wireOrientation);
//            }

//            if (extend) {
//                world.updateNeighborsAlways(blockPos, Blocks.PISTON_HEAD, wireOrientation);
//            }

            return true;
        }
    }
}
