package ac.grim.grimac.utils.data.movable;

import ac.grim.grimac.utils.latency.CompensatedWorld;
import ac.grim.grimac.utils.nmsutil.PistonPushReaction;
import ac.grim.grimac.utils.nmsutil.PistonPushReaction.Reaction;
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import com.github.retrooper.packetevents.protocol.world.states.type.StateType;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.world.states.type.StateTypes;
import com.github.retrooper.packetevents.protocol.world.BlockFace;

import java.util.ArrayList;
import java.util.List;

/**
 * Vanilla's {@code PistonStructureResolver}, run on the blocks Grim knows the client has.
 *
 * <p>The client works out which blocks a piston moves itself, from its own world, when it handles the block event: the
 * server only says which piston fired. Grim's compensated world is the client's world at that point, so resolving here
 * finds the same blocks, slime and honey branches included.
 *
 * <p>The algorithm is the same from 1.8 ({@code BlockPistonStructureHelper}) on. What changes is the blocks: an older
 * client sees some of the server's blocks as others, through ViaVersion.
 */
final class PistonStructure {

    static final int MAX_PUSH_DEPTH = 12;

    record Pos(int x, int y, int z) {
        Pos relative(BlockFace face) {
            return relative(face, 1);
        }

        Pos relative(BlockFace face, int distance) {
            return new Pos(x + face.getModX() * distance, y + face.getModY() * distance, z + face.getModZ() * distance);
        }
    }

    private static final BlockFace[] DIRECTIONS = {
            BlockFace.DOWN, BlockFace.UP, BlockFace.NORTH, BlockFace.SOUTH, BlockFace.WEST, BlockFace.EAST
    };

    /**
     * How the client's version treats the blocks Grim has.
     *
     * @param honeyIsSlime before 1.15 there is no honey block: ViaVersion shows it as slime, which sticks to slime
     * @param pushOnly     before 1.12 nothing is push-only: glazed terracotta is shown as a block pistons pull
     */
    record Rules(boolean honeyIsSlime, boolean pushOnly) {
        static Rules of(ClientVersion version) {
            return new Rules(version.isOlderThan(ClientVersion.V_1_15), version.isNewerThanOrEquals(ClientVersion.V_1_12));
        }
    }

    private final CompensatedWorld world;
    private final Rules rules;
    private final Pos pistonPos;
    private final boolean extending;
    private final Pos startPos;
    private final BlockFace pushDirection;
    private final BlockFace pistonDirection;
    /** Read as air: a pull takes the head away before working out what comes with it. */
    private final Pos removedHead;
    final List<Pos> toPush = new ArrayList<>();
    final List<Pos> toDestroy = new ArrayList<>();

    PistonStructure(CompensatedWorld world, Rules rules, Pos pistonPos, BlockFace pistonDirection, boolean extending) {
        this.world = world;
        this.rules = rules;
        this.pistonPos = pistonPos;
        this.pistonDirection = pistonDirection;
        this.extending = extending;
        if (extending) {
            this.pushDirection = pistonDirection;
            this.startPos = pistonPos.relative(pistonDirection);
            this.removedHead = null;
        } else {
            this.pushDirection = pistonDirection.getOppositeFace();
            this.startPos = pistonPos.relative(pistonDirection, 2);
            this.removedHead = pistonPos.relative(pistonDirection);
        }
    }

    boolean resolve() {
        toPush.clear();
        toDestroy.clear();
        WrappedBlockState state = state(startPos);
        if (!isPushable(state, startPos, pushDirection, false, pistonDirection)) {
            if (extending && reaction(state.getType()) == Reaction.DESTROY) {
                toDestroy.add(startPos);
                return true;
            }
            return false;
        }

        if (!addBlockLine(startPos, pushDirection)) {
            return false;
        }

        for (int i = 0; i < toPush.size(); i++) {
            Pos pos = toPush.get(i);
            if (isSticky(state(pos)) && !addBranchingBlocks(pos)) {
                return false;
            }
        }
        return true;
    }

    private boolean addBlockLine(Pos origin, BlockFace direction) {
        WrappedBlockState state = state(origin);
        if (state.getType().isAir()) return true;
        if (!isPushable(state, origin, pushDirection, false, direction)) return true;
        if (origin.equals(pistonPos)) return true;
        if (toPush.contains(origin)) return true;

        int length = 1;
        if (length + toPush.size() > MAX_PUSH_DEPTH) return false;

        // Sticky blocks drag the line behind them along
        while (isSticky(state)) {
            Pos behind = origin.relative(pushDirection.getOppositeFace(), length);
            WrappedBlockState previous = state;
            state = state(behind);
            if (state.getType().isAir()
                    || !canStickToEachOther(previous, state)
                    || !isPushable(state, behind, pushDirection, false, pushDirection.getOppositeFace())
                    || behind.equals(pistonPos)) {
                break;
            }

            if (++length + toPush.size() > MAX_PUSH_DEPTH) return false;
        }

        int added = 0;
        for (int k = length - 1; k >= 0; k--) {
            toPush.add(origin.relative(pushDirection.getOppositeFace(), k));
            added++;
        }

        for (int k = 1; ; k++) {
            Pos ahead = origin.relative(pushDirection, k);
            int collision = toPush.indexOf(ahead);
            if (collision > -1) {
                reorderListAtCollision(added, collision);
                for (int m = 0; m <= collision + added; m++) {
                    Pos pos = toPush.get(m);
                    if (isSticky(state(pos)) && !addBranchingBlocks(pos)) return false;
                }
                return true;
            }

            state = state(ahead);
            if (state.getType().isAir()) return true;
            if (!isPushable(state, ahead, pushDirection, true, pushDirection) || ahead.equals(pistonPos)) return false;
            if (reaction(state.getType()) == Reaction.DESTROY) {
                toDestroy.add(ahead);
                return true;
            }
            if (toPush.size() >= MAX_PUSH_DEPTH) return false;

            toPush.add(ahead);
            added++;
        }
    }

    private void reorderListAtCollision(int added, int collision) {
        List<Pos> before = new ArrayList<>(toPush.subList(0, collision));
        List<Pos> line = new ArrayList<>(toPush.subList(toPush.size() - added, toPush.size()));
        List<Pos> after = new ArrayList<>(toPush.subList(collision, toPush.size() - added));
        toPush.clear();
        toPush.addAll(before);
        toPush.addAll(line);
        toPush.addAll(after);
    }

    private boolean addBranchingBlocks(Pos pos) {
        WrappedBlockState state = state(pos);
        for (BlockFace direction : DIRECTIONS) {
            if (sameAxis(direction, pushDirection)) continue;
            Pos neighbour = pos.relative(direction);
            if (canStickToEachOther(state(neighbour), state) && !addBlockLine(neighbour, direction)) {
                return false;
            }
        }
        return true;
    }

    /** {@code PistonBaseBlock#isPushable}, without the world border. */
    boolean isPushable(WrappedBlockState state, Pos pos, BlockFace direction, boolean allowDestroy, BlockFace pistonDirection) {
        int minY = world.getMinHeight();
        int maxY = world.getMaxHeight() - 1;
        if (pos.y() < minY || pos.y() > maxY) return false;

        StateType type = state.getType();
        if (type.isAir()) return true;
        if (PistonPushReaction.isImmovable(type)) return false;
        if (direction == BlockFace.DOWN && pos.y() == minY) return false;
        if (direction == BlockFace.UP && pos.y() == maxY) return false;

        if (!isPiston(type)) {
            if (type.getHardness() == -1.0F) return false;
            switch (reaction(type)) {
                case BLOCK:
                    return false;
                case DESTROY:
                    return allowDestroy;
                case PUSH_ONLY:
                    return direction == pistonDirection;
                default:
                    break;
            }
        } else if (state.isExtended()) {
            return false;
        }

        return !PistonPushReaction.hasBlockEntity(type);
    }

    static boolean isPiston(StateType type) {
        return type == StateTypes.PISTON || type == StateTypes.STICKY_PISTON;
    }

    static boolean isSticky(WrappedBlockState state) {
        StateType type = state.getType();
        return type == StateTypes.SLIME_BLOCK || type == StateTypes.HONEY_BLOCK;
    }

    private boolean canStickToEachOther(WrappedBlockState first, WrappedBlockState second) {
        StateType a = first.getType();
        StateType b = second.getType();
        if (!rules.honeyIsSlime()) {
            if (a == StateTypes.HONEY_BLOCK && b == StateTypes.SLIME_BLOCK) return false;
            if (a == StateTypes.SLIME_BLOCK && b == StateTypes.HONEY_BLOCK) return false;
        }
        return isSticky(first) || isSticky(second);
    }

    Reaction reaction(StateType type) {
        Reaction reaction = PistonPushReaction.of(type);
        return reaction == Reaction.PUSH_ONLY && !rules.pushOnly() ? Reaction.NORMAL : reaction;
    }

    private static boolean sameAxis(BlockFace a, BlockFace b) {
        return a == b || a == b.getOppositeFace();
    }

    WrappedBlockState state(Pos pos) {
        if (pos.equals(removedHead)) return WrappedBlockState.getByGlobalId(CompensatedWorld.blockVersion, 0);
        return world.getBlock(pos.x(), pos.y(), pos.z());
    }
}
