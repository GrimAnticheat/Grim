package ac.grim.grimac.checks.type;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.collisions.datatypes.SimpleCollisionBox;
import ac.grim.grimac.utils.data.LastInstance;
import ac.grim.grimac.utils.nmsutil.BlockBreakSpeed;
import ac.grim.grimac.utils.nmsutil.Ray;
import ac.grim.grimac.utils.nmsutil.ReachUtils;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.DiggingAction;
import com.github.retrooper.packetevents.protocol.player.GameMode;
import com.github.retrooper.packetevents.util.Vector3i;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerDigging;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;
import org.bukkit.util.Vector;

import java.util.LinkedList;
import java.util.Queue;

/*
 * @author Sim0n (https://github.com/sim0n/nemesis)
 * Correct digging logic by DefineOutside
 */
public abstract class AirSwingCheck extends PacketCheck {
    private static final int MAX_COMBAT_TICKS = 10; // 1 minute

    private final Queue<Integer> samples = new LinkedList<>();

    private final boolean combatCheck; // if we should only check while in combat
    private final boolean doubleClicks; // if we should allow double clicks

    private final int maxSamples;

    private int movements;

    private Vector3i diggingLocation = null;
    private DiggingAction lastDiggingAction = DiggingAction.FINISHED_DIGGING;
    // We don't know if it's digging until the tick after a player's look,
    // or the 300 ms after breaking a block successfully
    private final LastInstance lastDigging = new LastInstance(player);


    public AirSwingCheck(GrimPlayer playerData, int maxSamples, boolean combatCheck, boolean doubleClicks) {
        super(playerData);

        this.maxSamples = maxSamples;

        this.combatCheck = combatCheck;
        this.doubleClicks = doubleClicks;
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        // Cancel digging is pointless in this game
        // You don't actually cancel the digging
        if (event.getPacketType() == PacketType.Play.Client.PLAYER_DIGGING) {
            WrapperPlayClientPlayerDigging wrapper = new WrapperPlayClientPlayerDigging(event);
            switch (wrapper.getAction()) {
                case START_DIGGING:
                    double damage = BlockBreakSpeed.getBlockDamage(player, wrapper.getBlockPosition());
                    boolean wasInstabreak = (damage > 1 || (player.gamemode == GameMode.CREATIVE && damage != 0));

                    if (wasInstabreak) { // Client is done mining first tick, no more packets
                        diggingLocation = null;
                        lastDiggingAction = DiggingAction.FINISHED_DIGGING;
                    } else {
                        diggingLocation = wrapper.getBlockPosition();
                        lastDiggingAction = DiggingAction.START_DIGGING;
                        lastDigging.reset();
                    }
                    break;
                case CANCELLED_DIGGING: // The player doesn't actually cancel digging, this is a lie
                    lastDiggingAction = DiggingAction.CANCELLED_DIGGING;
                    break;
                case FINISHED_DIGGING:
                    lastDiggingAction = DiggingAction.FINISHED_DIGGING;
                    // 300 ms delay after breaking blocks have animations without sending START_DIG
                    if (diggingLocation != null) lastDigging.setRaw(-6);
                    diggingLocation = null;
                    break;
            }
        }

        if (event.getPacketType() == PacketType.Play.Client.ANIMATION && !lastDigging.hasOccurredSince(2)) {
            if (movements < 10) {
                if (combatCheck && player.attackTicks > MAX_COMBAT_TICKS)
                    return;

                if (!doubleClicks && movements == 0)
                    return;

                if (player.isTickingReliablyFor(2) && samples.add(movements) && samples.size() == maxSamples) {
                    handle(samples);
                    samples.clear();
                }
            }

            movements = 0;
        } else if (WrapperPlayClientPlayerFlying.isFlying(event.getPacketType())) {
            Vector playerPos = new Vector(player.lastX, player.lastY, player.lastZ);

            // If the player isn't within 10 blocks of the block they are digging, don't bother.
            if (diggingLocation != null && playerPos.distanceSquared(new Vector(diggingLocation.getX(), diggingLocation.getY(), diggingLocation.getZ())) < 100) {
                if (lastDiggingAction == DiggingAction.START_DIGGING) { // START_BREAK without FINISH_BREAK or CANCEL_BREAK
                    lastDigging.reset();
                } else if (lastDiggingAction == DiggingAction.CANCELLED_DIGGING) { // Buggy cancel digging
                    // Brute force eye height because desync
                    for (double eyeHeight : player.getPossibleEyeHeights()) {
                        Ray trace = new Ray(player, playerPos.getX(), playerPos.getY() + eyeHeight, playerPos.getZ(), player.xRot, player.yRot);
                        Vector endVec = trace.getPointAtDistance(5);

                        SimpleCollisionBox hitbox = new SimpleCollisionBox(diggingLocation).expand(0.1); // Give some more lenience
                        Vector intercept = ReachUtils.calculateIntercept(hitbox, playerPos, endVec).getFirst();

                        if (ReachUtils.isVecInside(hitbox, playerPos) || intercept != null) {
                            lastDigging.reset();
                            return;
                        }
                    }
                }
            }

            if (player.isTickingReliablyFor(2)) {
                ++movements;
            } else {
                movements = 0;
            }
        }
    }

    public abstract void handle(Queue<Integer> samples);
}
