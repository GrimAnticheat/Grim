package ac.grim.grimac.checks.impl.movement;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PacketCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.math.GrimMath;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.potion.PotionTypes;
import com.github.retrooper.packetevents.protocol.world.states.type.StateTypes;
import org.jetbrains.annotations.NotNull;

@CheckData(name = "NoWeb", description = "suspicious movement in cobwebs", experimental = true, setback = 3)
public class NoWeb extends Check implements PacketCheck {
    private int bufferXZ;
    private int bufferY;
    private int bufferGround;
    
    public NoWeb(@NotNull GrimPlayer player) {
        super(player);
    }
    
    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.PLAYER_POSITION) {
            double deltaXZ = player.deltaXZ;
            double speed = GrimMath.getPlayerSpeed(this.player);
            
            boolean inWeb = isInWeb();
            boolean tridentTicks = player.riptideSpinAttackTicks < -50;
            
            checkXZMovement(deltaXZ, speed, inWeb, tridentTicks);
            
            checkYMovement(inWeb, tridentTicks);
            
            checkGroundMovement(inWeb, tridentTicks);
        }
    }
    
    private boolean isInWeb() {
        int blockX = (int) Math.floor(player.x);
        int blockY = (int) Math.floor(player.y);
        int blockZ = (int) Math.floor(player.z);
        
        return player.compensatedWorld.getBlock(blockX, blockY, blockZ).getType() == StateTypes.COBWEB;
    }
    
    private void checkXZMovement(double deltaXZ, double speed, boolean inWeb, boolean tridentTicks) {
        boolean exempt1 = !player.onGround && !player.isGliding && !player.isFlying && 
                         !player.predictedVelocity.isJump() && !player.predictedVelocity.isKnockback();
        boolean invalidXZ = inWeb && deltaXZ > 0.05F && speed > 0.3F;
        
        if (invalidXZ && exempt1 && tridentTicks) {
            if (bufferXZ++ > 1) {
                flagAndAlert(String.format("o[1]: %.6f", deltaXZ));
                if (shouldSetback()) player.getSetbackTeleportUtil().executeViolationSetback();
            }
        } else {
            bufferXZ = 0;
        }
    }
    
    private void checkYMovement(boolean inWeb, boolean tridentTicks) {
        double deltaY = player.actualMovement.getY();
        boolean invalidY = inWeb && deltaY > 0.005;
        boolean exempt2 = !player.compensatedEntities.self.hasPotionEffect(PotionTypes.LEVITATION) && 
                         !player.isGliding && !player.isFlying && 
                         !player.predictedVelocity.isJump() && !player.predictedVelocity.isKnockback();
        
        if (invalidY && exempt2 && tridentTicks) {
            if (bufferY++ > 1) {
                flagAndAlert(String.format("o[y]: %.6f", deltaY));
                if (shouldSetback()) player.getSetbackTeleportUtil().executeViolationSetback();
            }
        } else {
            bufferY = 0;
        }
    }
    
    private void checkGroundMovement(boolean inWeb, boolean tridentTicks) {
        double onGroundXZ = player.deltaXZ;
        boolean check = inWeb && onGroundXZ > 0.07;
        boolean data = player.onGround && 
                      !player.compensatedEntities.self.hasPotionEffect(PotionTypes.SPEED) && 
                      !player.isGliding && !player.isFlying && 
                      !player.predictedVelocity.isJump() && !player.predictedVelocity.isKnockback();
        
        if (check && data && tridentTicks) {
            if (bufferGround++ > 1) {
                flagAndAlert(String.format("o[2]: %.6f", onGroundXZ));
                if (shouldSetback()) player.getSetbackTeleportUtil().executeViolationSetback();
            }
        } else {
            bufferGround = 0;
        }
    }
}
