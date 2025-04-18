package ac.grim.grimac.checks.impl.scaffolding;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.api.config.ConfigManager;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.CheckType;
import ac.grim.grimac.checks.type.BlockPlaceCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.update.BlockPlace;
import ac.grim.grimac.utils.change.BlockModification;
import ac.grim.grimac.utils.nmsutil.Materials;
import com.github.retrooper.packetevents.protocol.player.GameMode;
import com.github.retrooper.packetevents.protocol.world.states.type.StateType;
import com.github.retrooper.packetevents.util.Vector3i;

@CheckData(name = "AirLiquidPlace", checkType = CheckType.WORLD)
public class AirLiquidPlace extends BlockPlaceCheck {
    public AirLiquidPlace(GrimPlayer player) {
        super(player);
    }

    /*
     * This check has been plagued by falses for ages, and I've finally figured it out.
     * When breaking and placing on the same tick in the same tick, I believe the vanilla client always sends DIGGING ACTION packets first
     * This check's falses all seem to stem from processing DiggingAction.START_DIGGING before PacketType.Play.Client.PLAYER_BLOCK_PLACEMENT in the same tick
     * Since we process the break first, when we go to process the place it looks like the player placed against air in the async world
     *
     * We will often see:
     *     Async world updated: short_grass -> air at X: -32, Y: 69, Z: -240, tick 0, cause/source: DiggingAction.START_DIGGING
     *     AirLiquidPlace Check: Block state at X: -32, Y: 69, Z: -240 is air (valid: false), tick +0-1, cause/source: PacketType.Play.Client.PLAYER_BLOCK_PLACEMENT <---- previously falsed here
     *     Async world updated: air -> short_grass at X: -32, Y: 69, Z: -240, tick +3-4, cause: realtime task in applyBlockChanges(List<Vector3i> toApplyBlocks) source: PacketType.Play.Client.PONG
     *     Async world updated: short_grass -> air at X: -32, Y: 69, Z: -240, tick +0-1, cause: handleNettySyncTransaction(LatencyUtils.java:56) source: PacketType.Play.Client.PONG
     *
     * In addition, it is possible for:
     *     Async world updated: short_grass -> air at X: -49, Y: 69, Z: -190, tick 0, cause: realtime task in applyBlockChanges(List<Vector3i> toApplyBlocks) source: PacketType.Play.Client.PONG
     *     AirLiquidPlace Check: Block state at X: -49, Y: 69, Z: -190 is air (valid=false), tick 0 <--- false due to change from applyBlockChanges()
     *     Async world updated: grass_block[snowy=false] -> grass_block[snowy=false] at X: -49, Y: 69, Z: -189, tick 0, cause: handleNettySyncTransaction(LatencyUtils.java:56) source: PacketType.Play.Client.PONG
     *
     * And in even more rare cases:
     *     Async world updated: air -> air at X: -51, Y: 71, Z: -179, tick 0, cause: handleNettySyncTransaction(LatencyUtils.java:56) source: PacketType.Play.Client.PONG
     *     AirLiquidPlace Check: Block state at X: -49, Y: 70, Z: -180 is short_grass (valid=true), tick 0
     *     Async world updated: short_grass -> air at X: -49, Y: 70, Z: -180, tick 1, cause/source: DiggingAction.START_DIGGING <--- double dig here (see my AirLiquidBreak patch) this is legit behaviour. Can only be up to 2 in 1 tick though.
     *     Async world updated: air -> short_grass at X: -49, Y: 70, Z: -180, tick 1, cause/source: DiggingAction.START_DIGGING
     *     Async world updated: short_grass -> air at X: -51, Y: 70, Z: -179, tick 1, cause: realtime task in applyBlockChanges(List<Vector3i> toApplyBlocks) source: PacketType.Play.Client.PONG
     *     AirLiquidPlace Check: Block state at X: -49, Y: 70, Z: -179 is air (valid=false), tick 2 <--- falses here due to double dig if we only check the latest changed blockstate. We have to check all changes at the location in same tick.
     *     AirLiquidPlace Check: Block state at X: -49, Y: 70, Z: -179 is air (valid=true), tick 2
     *
     *     All of which previously would've caused a false.
     *     To solve this we store recently changed blocks caused by DiggingAction.START_DIGGING (instant breaking) and check against the old block.
     *     Lots of other checks have similar issues, and with the new player.blockHistory we can patch those.
     *
     * So that's it right? It's unfalsable?
     *     Very close but not quite. Vanilla's client game desyncs, especially on a laggy connection where a player is breaking and placing grass 20 cps/sec in the same tick
     *     it is possible for short grass to be interacted with even if server-side the block is air much later, and it won't be accounted for because the modification isn't recent.
     *     This is incredibly rare, unreliable and is only triggerable if you intentionally want to false the check. I consider a violation lvl of 2-3 to be reliable
     *     and 5-6 to be autobannable (if you don't care about people who are deliberately trying to get themselves false banned)
     */
    @Override
    public void onBlockPlace(final BlockPlace place) {
        if (player.gamemode == GameMode.CREATIVE) return;

        Vector3i blockPos = place.getPlacedAgainstBlockLocation();
        StateType placeAgainst = player.compensatedWorld.getBlockType(blockPos.getX(), blockPos.getY(), blockPos.getZ());

        int currentTick = GrimAPI.INSTANCE.getTickManager().currentTick;
        // this is actual more lenient then we need to be, We can check up to 1 ticks for all changes at location sand up to 0 ticks for first change
        // But for such tiny differences in legitness its not worth it.
        Iterable<BlockModification> blockModifications = player.blockHistory.getRecentModifications((blockModification) -> currentTick - blockModification.tick() < 2
                && blockPos.equals(blockModification.location())
                && (blockModification.cause() == BlockModification.Cause.START_DIGGING || blockModification.cause() == BlockModification.Cause.HANDLE_NETTY_SYNC_TRANSACTION));

        // Check if old block from instant breaking in same tick as the current placement was valid
        // There should only be one block here for legit clients
        for (BlockModification blockModification : blockModifications) {
            StateType stateType = blockModification.oldBlockContents().getType();
            if (!stateType.isAir() && !Materials.isNoPlaceLiquid(stateType)) {
                return;
            }
        }

        if (placeAgainst.isAir() || Materials.isNoPlaceLiquid(placeAgainst)) { // fail
            if (flagAndAlert() && shouldModifyPackets() && shouldCancel()) {
                place.resync();
            }
        }
    }

    @Override
    public void onReload(ConfigManager config) {
        this.cancelVL = config.getIntElse(getConfigName() + ".cancelVL", 0);
    }
}
