package ac.grim.grimac.checks.impl.invalid;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PacketCheck;
import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;

@CheckData(name = "InvalidC", description = "Detects advanced illegal blocking actions", setback = 1, experimental = false)
public class InvalidC extends Check implements PacketCheck {

    private int lastInteractEntity = -1;
    private int lastAttackEntity = -1;
    private int attackCount = 0;
    private double buffer = 0;

    public InvalidC(GrimPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() != PacketType.Play.Client.INTERACT_ENTITY)
            return;

        WrapperPlayClientInteractEntity wrapper = new WrapperPlayClientInteractEntity(event);

        // Record normal interactions (right-clicks)
        if (wrapper.getAction() == WrapperPlayClientInteractEntity.InteractAction.INTERACT) {
            lastInteractEntity = wrapper.getEntityId();
        }

        // Handle attack packets
        if (wrapper.getAction() == WrapperPlayClientInteractEntity.InteractAction.ATTACK) {

            // Only analyze if player is blocking with item
            if (!player.packetStateData.isSlowedByUsingItem() && !player.packetStateData.isSlowedByUsingItem()) {
                resetAttackData();
                return;
            }

            int entityId = wrapper.getEntityId();

            // Suspicious case: attacking a different entity while blocking
            if (lastInteractEntity != -1 && entityId != lastInteractEntity) {
                buffer += 1.0;
            } else {
                buffer = Math.max(0, buffer - 0.5); // reward for human-like behavior
            }

            // Detect duplicate attack packets (fast or multiple ATTACKs)
            if (entityId == lastAttackEntity) {
                attackCount++;
                if (attackCount > 2)
                    buffer += 1.5; // highly suspicious
            } else {
                attackCount = 1;
            }

            lastAttackEntity = entityId;
            lastInteractEntity = -1; // reset interaction for next tick

            // Flag if buffer exceeds threshold
            if (buffer > 3.0) {
                if (flagAndAlert("check suspected | buffer=" + buffer)) {
                    setbackIfAboveSetbackVL();
                    if (shouldModifyPackets()) {
                        event.setCancelled(true);
                        player.onPacketCancel();
                    }
                }
                buffer = 0;
            }
        }
    }

    // Resets all attack-related tracking data
    private void resetAttackData() {
        lastInteractEntity = -1;
        lastAttackEntity = -1;
        attackCount = 0;
        buffer = 0;
    }
}
