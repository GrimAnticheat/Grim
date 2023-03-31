package ac.grim.grimac.checks.impl.inventory;
import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PacketCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.lists.EvictingQueue;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityVelocity;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerExplosion;
import lombok.Getter;

@CheckData(name = "InventoryD", setback = 3, experimental = true)
// Delta calculation copied from https://github.com/GladUrBad/Medusa/blob/master/Impl/src/main/java/com/gladurbad/medusa/data/processor/PositionProcessor.java
public class InventoryD extends Check implements PacketCheck {
    private final EvictingQueue<Velocity> velocityData = new EvictingQueue<>(100);
    private double deltaXZ;
    private double lastDeltaXZ;

    public InventoryD(GrimPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(final PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.CLICK_WINDOW) {
            if (player.packetStateData.lastPacketWasTeleport ||
                    player.packetStateData.lastPacketWasOnePointSeventeenDuplicate ||
                    player.isSwimming || player.slightlyTouchingLava ||
                    player.slightlyTouchingWater ||
                    player.wasTouchingLava || player.wasTouchingWater ||
                    getHorizontalVelocity() > 0 || player.compensatedEntities.getSelf().inVehicle()) {
                return;
            }

            double accel = deltaXZ - lastDeltaXZ;

            // Is not possible to click the inventory while moving
            if (deltaXZ > 0.21D && accel >= 0D) {
                if (flagWithSetback()) {
                    // Cancel the packet
                    if (shouldModifyPackets()) {
                        event.setCancelled(true);
                        player.onPacketCancel();
                    }
                    alert("");
                }
            } else {
                reward();
            }
        } else if (WrapperPlayClientPlayerFlying.isFlying(event.getPacketType())) {
            WrapperPlayClientPlayerFlying wrapper = new WrapperPlayClientPlayerFlying(event);

            if (wrapper.hasPositionChanged()) {
                double deltaX = player.x - player.lastX;
                double deltaZ = player.z - player.lastZ;
                lastDeltaXZ = deltaXZ;
                deltaXZ = Math.hypot(deltaX, deltaZ);
            }

            velocityData.removeIf(Velocity::isCompleted);
        }
    }

    @Override
    public void onPacketSend(PacketSendEvent event) {
        if (event.getPacketType() == PacketType.Play.Server.ENTITY_VELOCITY) {
            WrapperPlayServerEntityVelocity wrapper = new WrapperPlayServerEntityVelocity(event);

            if (wrapper.getEntityId() == player.entityID) {
                double x = wrapper.getVelocity().getX() / 8000d;
                double y = wrapper.getVelocity().getY() / 8000d;
                double z = wrapper.getVelocity().getZ() / 8000d;

                short lastSentTransaction = (short) (player.lastTransactionSent.get() + 1);

                velocityData.add(new Velocity(lastSentTransaction, x, y, z));
            }
        } else if (event.getPacketType() == PacketType.Play.Server.EXPLOSION) {
            WrapperPlayServerExplosion wrapper = new WrapperPlayServerExplosion(event);

            double x = wrapper.getPlayerMotion().getX();
            double y = wrapper.getPlayerMotion().getY();
            double z = wrapper.getPlayerMotion().getZ();

            if (x == 0.0D && y == 0.0D && z == 0.0D) return;

            short lastSentTransaction = (short) (player.lastTransactionSent.get() + 1);

            velocityData.add(new Velocity(lastSentTransaction, x, y, z));
        }
    }

    public double getHorizontalVelocity() {
        if (velocityData.isEmpty()) {
            return 0;
        }

        double velocitySum = 0;
        for (Velocity velocity : velocityData) {
            velocitySum += velocity.getHorizontalVelocity();
        }

        return velocitySum;
    }


    @Getter
    public class Velocity {
        private final double horizontalVelocity;
        private final double verticalVelocity;
        private final double velocityX;
        private final double velocityZ;
        private final short transaction;
        private final int completedTick;

        public Velocity(short transaction, double velocityX, double verticalVelocity, double velocityZ) {
            this.velocityX = velocityX;
            this.velocityZ = velocityZ;
            this.horizontalVelocity = Math.hypot(velocityX, velocityZ);
            this.verticalVelocity = verticalVelocity;
            this.transaction = transaction;
            this.completedTick = calculateCompletedTick();
        }

        private int calculateCompletedTick() {
            int ticks = player.totalFlyingPacketsSent;
            return (int) (ticks + ((horizontalVelocity / 2 + 2) * 15));
        }

        public boolean isCompleted() {
            return completedTick != -1 && player.totalFlyingPacketsSent > completedTick;
        }
    }
}