package ac.grim.grimac.utils.data.movable;

import ac.grim.grimac.utils.collisions.datatypes.SimpleCollisionBox;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.data.PistonData;
import ac.grim.grimac.utils.reflection.GeyserUtil;
import ac.grim.grimac.utils.data.movable.PistonStructure.Pos;
import ac.grim.grimac.utils.data.movable.PistonStructure.Rules;
import ac.grim.grimac.utils.latency.CompensatedWorld;
import ac.grim.grimac.utils.nmsutil.PistonPushReaction.Reaction;
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import com.github.retrooper.packetevents.protocol.world.states.type.StateType;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;

import com.github.retrooper.packetevents.protocol.world.states.type.StateTypes;
import com.github.retrooper.packetevents.protocol.world.BlockFace;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * A piston's block event, handled the way the client handles it.
 *
 * <p>The server only says which piston fired. The client fires the piston it has at that position, works out which
 * blocks move from its own world, and changes its world straight away: the blocks become moving pistons, where they were
 * becomes air, and a retracting head goes. The server's block updates for all of that come a tick later. Grim makes the
 * same changes to its copy of the client's world when the client gets the event, and gives the area the blocks sweep
 * through to prediction as a push it can't simulate exactly.
 *
 * <p>What the client does depends on its version:
 * <ul>
 *     <li>Retracting with nothing to pull (action 2, or a sticky piston whose block won't come) takes the head away from
 *     1.13. Before, the client knows no action 2, and a sticky piston that pulls nothing leaves its head for the
 *     server's updates to remove.</li>
 *     <li>The blocks moved follow the client's blocks: see {@link Rules}.</li>
 * </ul>
 * Bedrock players don't run Java's piston code at all, so for them only the push is given, and the world is left to the
 * server's updates.
 */
public final class PistonBlockEvent {

    public static final int EXTEND = 0;
    public static final int RETRACT = 1;
    /** Retract without pulling: a sticky piston given too short a pulse drops its block. Since 1.13. */
    public static final int DROP = 2;

    private PistonBlockEvent() {
    }

    /**
     * Handles a piston block event the client is getting now, on the netty thread, in transaction order. Returns the
     * push the client feels, or null when the client has no piston at the position.
     */
    public static @Nullable PistonData handle(GrimPlayer player, int x, int y, int z, int action, int lastTransactionSent) {
        CompensatedWorld world = player.compensatedWorld;
        WrappedBlockState piston = world.getBlock(x, y, z);
        StateType pistonType = piston.getType();
        if (!PistonStructure.isPiston(pistonType)) return null;
        if (action != EXTEND && action != RETRACT && action != DROP) return null;

        ClientVersion version = player.getClientVersion();
        boolean modern = version.isNewerThanOrEquals(ClientVersion.V_1_13);
        // Without our world changes the push is still given, which is all the old event listeners did
        boolean changeWorld = !GeyserUtil.isBedrockPlayer(player.user.getUUID()) && (action != DROP || modern);

        BlockFace facing = piston.getFacing();
        Pos base = new Pos(x, y, z);
        Pos head = base.relative(facing);
        Rules rules = Rules.of(version);
        Push push = new Push();

        if (action == EXTEND) {
            PistonStructure structure = new PistonStructure(world, rules, base, facing, true);
            if (structure.resolve()) {
                push.add(structure, structure.toPush, facing);
                if (changeWorld) {
                    moveBlocks(world, structure, head, facing, true);
                    // The base is extended from here, so its face no longer fills the block
                    WrappedBlockState extended = piston.clone();
                    extended.setExtended(true);
                    world.updateBlock(x, y, z, extended.getGlobalId());
                }
            } else {
                // The client won't move anything, as its world doesn't let it, yet the server moved something: it will
                // send that next tick. Allow the straight line it could have been.
                push.add(structure, straightLine(structure, head, facing), facing);
            }
            // The head, coming out of the base
            push.boxes.add(box(head));
            return push.toData(base, facing, lastTransactionSent, true);
        }

        BlockFace pull = facing.getOppositeFace();
        // The head goes back into the base, wherever the client takes it away
        push.boxes.add(box(head));
        if (changeWorld) {
            world.updateBlock(base.x(), base.y(), base.z(), movingPiston(facing).getGlobalId());
        }

        if (pistonType != StateTypes.STICKY_PISTON) {
            if (changeWorld) setAir(world, head);
            return push.toData(base, pull, lastTransactionSent, false);
        }

        PistonStructure structure = new PistonStructure(world, rules, base, facing, false);
        Pos front = base.relative(facing, 2);
        WrappedBlockState pulled = structure.state(front);
        boolean pulls = action == RETRACT
                && !pulled.getType().isAir()
                && structure.isPushable(pulled, front, pull, false, facing)
                && (structure.reaction(pulled.getType()) == Reaction.NORMAL || PistonStructure.isPiston(pulled.getType()));
        if (pulls) {
            // moveBlocks takes the head away first, then may still find the blocks won't come
            if (changeWorld) setAir(world, head);
            if (structure.resolve()) {
                push.add(structure, structure.toPush, pull);
                if (changeWorld) moveBlocks(world, structure, head, pull, false);
            }
        } else if (changeWorld && modern) {
            setAir(world, head);
        }
        return push.toData(base, pull, lastTransactionSent, false);
    }

    /**
     * {@code PistonBaseBlock#moveBlocks} after resolving: what's destroyed and where the moved blocks were become air,
     * and where they're going, plus an extending head, become moving pistons.
     */
    private static void moveBlocks(CompensatedWorld world, PistonStructure structure, Pos head, BlockFace direction, boolean extending) {
        Set<Pos> vacated = new LinkedHashSet<>(structure.toPush);
        for (Pos destroyed : structure.toDestroy) {
            setAir(world, destroyed);
        }

        WrappedBlockState moving = movingPiston(direction);
        for (int i = structure.toPush.size() - 1; i >= 0; i--) {
            Pos destination = structure.toPush.get(i).relative(direction);
            vacated.remove(destination);
            world.updateBlock(destination.x(), destination.y(), destination.z(), moving.getGlobalId());
        }

        if (extending) {
            vacated.remove(head);
            world.updateBlock(head.x(), head.y(), head.z(), moving.getGlobalId());
        }

        for (Pos pos : vacated) {
            setAir(world, pos);
        }
    }

    private static WrappedBlockState movingPiston(BlockFace facing) {
        WrappedBlockState state = WrappedBlockState.getDefaultState(CompensatedWorld.blockVersion, StateTypes.MOVING_PISTON).clone();
        state.setFacing(facing);
        return state;
    }

    private static void setAir(CompensatedWorld world, Pos pos) {
        world.updateBlock(pos.x(), pos.y(), pos.z(), WrappedBlockState.getByGlobalId(CompensatedWorld.blockVersion, 0).getGlobalId());
    }

    /** The blocks in a row in front of the head, up to as many as a piston can push. */
    private static List<Pos> straightLine(PistonStructure structure, Pos from, BlockFace direction) {
        List<Pos> line = new ArrayList<>();
        Pos pos = from;
        while (line.size() < PistonStructure.MAX_PUSH_DEPTH && !structure.state(pos).getType().isAir()) {
            line.add(pos);
            pos = pos.relative(direction);
        }
        return line;
    }

    private static SimpleCollisionBox box(Pos pos) {
        return new SimpleCollisionBox(pos.x(), pos.y(), pos.z());
    }

    /** Where the moved blocks sweep through, from where they are to where they go, and whether any carry entities. */
    private static final class Push {
        final List<SimpleCollisionBox> boxes = new ArrayList<>();
        boolean slime;
        boolean honey;

        void add(PistonStructure structure, List<Pos> positions, BlockFace direction) {
            for (Pos pos : positions) {
                boxes.add(box(pos));
                boxes.add(box(pos.relative(direction)));
                StateType type = structure.state(pos).getType();
                slime |= type == StateTypes.SLIME_BLOCK;
                honey |= type == StateTypes.HONEY_BLOCK;
            }
        }

        PistonData toData(Pos base, BlockFace direction, int lastTransactionSent, boolean isPush) {
            return new PistonData(direction, boxes, lastTransactionSent, isPush, slime, honey);
        }
    }
}
